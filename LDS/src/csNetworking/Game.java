package csNetworking;

/**
 * Author:  Camille Ciancanelli
 * Game Class: Has one state interface implemented by several "game states". 
 *             Has a player class.
 *             Has a server class.
 *             Sends the messages to the clients.
 *             Calls appropriate state to take action on player message.   
 *             Sends player messages back to the server     
 * NOTE: Using an array of length 30 (max num players) would be more efficient but
 *       I've never used hashMap in Java so I want to learn how to work with it.
 * NOTE: Clients are socket connects representing players.
 *       Players are objects in the game.  There is one player per socket connection.
 *        
 *TODO does game shut of the server or does main? I think main does b/c main starts the server.   
 * */

import java.io.*;
import java.util.*;

public class Game {
	private String[] argsIn = null;
	private int portNum;
	private int minPlayers = 3; 
	private int maxPlayers = 30;
	private int timeToWait = 10; //set this back to 60 as a default. 10 is here for testing..I don't want to wait 60 seconds 
	private long startLobbyTime;  
	//private long endLobbyTime;  
	private int atcnt;
	private boolean timerOn = false;
	private boolean sendRoundMsg = false;
	private boolean sendDiceMsg = false;
	private int playerWithMessage = -1;
	private boolean hasAllMessage = false;
	private boolean hasAllMessageGameLogic = false;
	private String messageFromGameLogic;
	private Map<Integer, Player> playerMap= new LinkedHashMap<Integer, Player>(); //LinkedHashMap will maintain insertion order for iteration purposes. 
	//private ListIterator iterator = playerMap.hasNext question for Dr. Reedy
	private Iterator iterator = playerMap.iterator()//method does not exist.  Ask Dr. Reedy
	private State stateLobby;	
	private State stateInGame;
	private State stateTimerLobby;
	private GameState nextState;
	private NonBlockingServer server;
	private State state; 
	private int whoseTurn = 0;

	public enum GameState{DEFAULT, INGAME, LOBBY, TIMERLOBBY};
	public enum PlayerAct{DEFAULT, JOIN,QUIT,BID,CHALLENGE};
	public enum PlayerStatus{CONNECTED, PLAYING, WATCHING};

	//===============================================================
	//   Constructor
	//===============================================================
	public Game(String[] argsIn){
		this.argsIn = argsIn; 
		parseCommandLine();
		server = new NonBlockingServer(portNum, this);
		stateLobby = new StateLobby(this); // make new instance of lobby with current instance of game
		stateInGame = new StateInGame(this);
		stateTimerLobby = new StateTimerLobby(this);
		this.state = stateLobby;
		assert state != null;
	}
	//===============================================================
	//      Private methods
	//===============================================================
	
	/* Drives the game based on received messages from clients.
	*  Responsible for changing state and implementing the game. 
	*  Initial state is "LOBBY".  When min number of players is reached
	*  state changes to "TIMERLOBBY".  When max wait time is over
	*  state changes to "INGAME" where the betting rounds will be held.
	*  When game is over TODO a winning message will be sent and state will
	*  be reset to "LOBBY"
	*/
	private void gameLogic(int clientId, String[] request){
		
		//TODO wDoes this compare memory locations or the values at the address //this.state == stateTimerLobby
	    if ((getPlayerCount() == minPlayers) && (timerOn == false)){
           state = stateTimerLobby;
           timerOn = true;
           sendTimerMessage();
           System.err.print("min players reached");
           startTime();
		}
	    //Only check this if the timer has been set (b/c min num of players is reached)
	    //This will start the "round" and flip timer back to off. 
	    if (timerOn & elapsedTime(startLobbyTime)>= timeToWait){
	    	state = stateInGame;
	    	sendRoundMsg = true;
	    	timerOn = false;
	    } 
	    
	    if (sendRoundMsg) {
	    	//send everyone round start message
	    	roundStartMsg();
	    	sendRoundMsg = false;
	    	sendDiceMsg= true;
	    }
	    
	    
	    //calculate who's turn.  
		
		if (nextState == GameState.LOBBY){this.state = stateLobby;}
		else if (nextState == GameState.TIMERLOBBY){this.state = stateTimerLobby;}
		else if (nextState == GameState.INGAME){this.state = stateInGame;}	
		
		PlayerAct message= Parser.parse(clientId, request);
		switch(message){
		case DEFAULT: 
			//will need to send a message to client saying bad message;s
			break;
		case JOIN:
			state.join(clientId, request);
			break;
		case QUIT: 
			state.quit(clientId);
		case BID:
			state.bid(playerMap, clientId, request);
			break;
		case CHALLENGE:
			state.challenge(playerMap, clientId, request);
			break;
		} 
	}
	
	
	private void parseCommandLine(){
		int i = 0;
		for (i = 0; i < argsIn.length; i++){
			if (argsIn[i].equals("-p")){
				portNum= Integer.parseInt(argsIn[i+1]);//portNum // = Integer.parseInt(argsIn[i+1]); i++;
			}
			if (argsIn[i].equals("-m")){
				minPlayers= Integer.parseInt(argsIn[i+1]);
			}
			if (argsIn[i].equals("-M")){
				maxPlayers = Integer.parseInt(argsIn[i+1]);
			}
			if (argsIn[i].equals("-t")){
				timeToWait= Integer.parseInt(argsIn[i+1]); 
			}
			if (argsIn[i].equals("-a")){
				atcnt = Integer.parseInt(argsIn[i+1]); 
			}
		}
		System.out.println(String.format("port = %d min = %d max %d time %d kicked %d", portNum, minPlayers, maxPlayers, timeToWait, atcnt));
	}
	
	private void sendTimerMessage(){
		messageFromGameLogic = "[timer_start, " + timeToWait + "]";
	    setHasMessageToAllFromGameLogic(true);
	}
	private void startTime(){
		startLobbyTime =  System.currentTimeMillis( );
	}
	
	private int elapsedTime(long time){
		long temp = System.currentTimeMillis( ) - time;
		int elapsed = (int) (temp/1000) % 60 ;
		return elapsed;
	}
	
	/*
	 * TODO: Test this with differnet dice values. 
      [round_start,player_count,player1#, p1_diceno,..., playern#, pn_diceno]
      Informs everyone of all the players’ dice counts
      Sample (4 players. p1 has 5 dice, p3 has 5, etc): [round_start, 4,1,5,3,4,4,5,5,4] 
	 * */
	
	private String roundStartMsg(){
		StringBuilder sb = new StringBuilder();
		sb.append("[round_start, ").append(getPlayerCount());
		
        for (int i = 0; i < getPlayerCount(); i++){
        	if(isPlayerValid(i)){
        	//TODO: move this.  rollDice shouldn't be here. at start of each round player rolls the dice	
            playerMap.get(i).rollDice();
			sb.append(" , ").append(i).append(", ").append(playerMap.get(i).getDiceCount()); 
        	}
		} 
		
        sb.append("] [player_turn, " + getPlayerTurn() + "]");
		messageFromGameLogic = sb.toString();
		setHasMessageToAllFromGameLogic(true);
		return messageFromGameLogic;		
	}
	
	public int getPlayerTurn(){
		return whoseTurn;
	}
	
	public void nextPlayerTurn(int current){
		   if (getPlayerCount() == current)
			   current = 0;
	       if (isPlayerValid(current)){
	    	   
	       } 
	            whoseTurn = current;
	}
	//===============================================================
	//      Public methods  --why are there some privates here? Fix!
	//===============================================================
	
	
	/*Returns true if the palyer's number is found in the map. 
	 and the player has joined or is waiting.  Prevents connected
	 players who haven't joined from receiving messages. 
	 prevent accessing a null reference.*/ 
	public boolean isPlayerValid(int id){
		boolean valid = false;
		if ((playerMap.get(id).getWaitingStatus() != PlayerStatus.CONNECTED) & playerMap.containsKey(id)){
			valid = true;
		}
	
		return valid;
		}
    
	//Client is playing game, not just observing. 
	public boolean isPlayerInRound(int id){
		boolean valid = false;
		if ((playerMap.get(id).getWaitingStatus() == PlayerStatus.PLAYING) & playerMap.containsKey(id)){
			valid = true;
		}
	
		return valid;
		}
	
	
	
	/*Rolls all dice held by a player*/
	public void rollDice(int id){
		playerMap.get(id).rollDice();
		}

	public void serverStart(){
		try {
			server.startServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    //Method calls server to close connection to client channel. 
	public void setclientClose(int id){
		   server.setClientClose();
		   server.clientClose(id);
	}
    //TODO implement and write PRE/POST comments
	private void endGame(){}


	//---------------------------------------------------------------------------
	/* Collect data from client to form a complete message
		 When entire message is collect recieveAction will be called.*/ 
	public void setMessage(int clientId, String messageIn){
		Player temPlayer = playerMap.get(new Integer(clientId));
		assert temPlayer != null;
		//TODO: check if key is valid.
		if (playerMap.get(new Integer(clientId)).setMessage(messageIn) != -1){
			gameLogic(new Integer(clientId), playerMap.get(clientId).getMessage());
		}
	}
	
    //TODO why is remove player functionality commented out? 
	private void removePlayer(Player player){
		/*playerList.remove(player)*/;}
	
	/* Sets the client ID of player with a message*/
    public void clientHasMessage(int id){
    	playerWithMessage = id;}

	/*Returns player# with a message or -1 if no message */
	public int getPlayerNumWithMessage(){
		return playerWithMessage;}
	
	/* Call after a message has been set to a player*/
	public void resetPlayerNumWithMessage(){
		playerWithMessage = -1;
	}

	/* Adds player to the PlayerQ but does not "join" them to the game */ 
	public void addPlayer(int playerID){
		Player newPlayer = new Player(playerID, atcnt);
		playerMap.put(playerID, newPlayer);
	}

	/*will this remove the player but keep the key ordering the same?*/ 
	public void removePlayer(int playerID){
		playerMap.remove(playerID);
	}
	
	/*
	 * Does not count players who have connected but are not joined. 
	 * */
	public int getPlayerCount() {
		// Uncomment to test. 
		//System.out.print("NEW CALL TO PLAYER COUNT"  + "\n");
		//System.out.print("size of palyer map is :" + playerMap.size() + "\n");
		int count = 0;
		for (Integer key : playerMap.keySet()){
			if (playerMap.get(key).getWaitingStatus() != PlayerStatus.CONNECTED){
				count++;
			//	System.out.print("name of player waiting : " + playerMap.get(key).getName() + " \n");
			}
		}
		//System.out.print("Size of playing/waiting palyers is:  :" + count + "\n");
		return count;
	}
	
	//==============================================================
	//             Getters and Setters
	//==============================================================
	

	//Messages from a sate
	public boolean getHasMessageToAll (){return hasAllMessage;} 
	public String  getAllPlayerMessageFromState(){return state.sendToAll();}
	public void    setHasMessageToAll(boolean reset){hasAllMessage = reset;}
	public void    setPlayerMessage(int Id, String sendMessage){playerMap.get(Id).setPlayerMessage(sendMessage);}
	
	//Messages from the game to all clients
	public boolean getHasMessageToAllFromGameLogic() {return hasAllMessageGameLogic;}
	public String  getAllPlayerMessageFromGameLogic(){return messageFromGameLogic;} 
	public void    setHasMessageToAllFromGameLogic(boolean reset){hasAllMessageGameLogic = reset;}
	/*public String getPlayerTurnMessageFromGameLogic(){
		String turnMsg =  null;
		 if isPlayerInRound()
		return turnMsg;
	}*/
	
	//Messages from the game to each client
	public void  setDiceMsg(){sendDiceMsg = false;}
	public boolean  getDiceMsg(){return sendDiceMsg;}
	public String getPlayerDiceMessageFromState(int id){return playerMap.get(id).diceMessage();}
	public String  getPlayerMessage(int id){return playerMap.get(id).getPlayerMessage();}

	//Returns true if client is a player and false if client is observing
	public PlayerStatus getWaitingStatus(int id){return playerMap.get(id).getWaitingStatus();} 
	public void    setWaitingStatus(int id, PlayerStatus newWaitingStatus){playerMap.get(id).setWaitingStatus(newWaitingStatus);}	
	public String  getPlayerName(int Id){ return playerMap.get(Id).getName();}
	public void    setPlayerName(int Id, String name){playerMap.get(Id).setName(name);}
	public int     getPlayerAttemptCount(int id){return playerMap.get(id).getAttemptCount();}
	public void    setPlayerAttemptCount(int id, int newValue){ playerMap.get(id).setAttemptCount(newValue);}
	public int     getMinPlayerCnt(){return minPlayers;}
	public int     getMaxPlayerCnt(){return maxPlayers;}
	public int     getTimeToWait(){return timeToWait;}
	public int     getAtcnt(){return atcnt;}
	public void    setState(GameState nextState){System.err.print("in changing state");this.nextState = nextState;	}
	
}
