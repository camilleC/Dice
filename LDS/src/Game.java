
/**
 * Author:  Camille Ciancanelli
 * Game Class: Has one state interface implemented by several "game states". 
 *             Has a player class.
 *             Has a server class.
 *             Sends the messages to the clients.
 *             Calls appropriate state to take action on player message.   
 *             Sends player messages back to the server     
 * NOTE: Clients are socket connects representing players.
 *       Players are objects in the game.  There is one player per socket connection.
 *        
 *TODO does game shut of the server or does main? I think main does b/c main starts the server.   
 * */
import java.io.*;
import java.util.Map;
import java.util.HashMap;

public class Game {
	private String[] argsIn = null;
	private int portNum;
	private int minPlayers; //TODO what if only two people join?  Does the game eventually kick them out and shut off? 
	private int maxPlayers;
	private int timeToWait; //wait time in Lobby with timer b/f moving to game state. 
	private int startLobbyTime;  
	private int endLobbyTime;  
	private int atcnt;
	private int playerWithMessage = -1;
	private boolean hasAllMessage = false;
	private Map<Integer, Player> playerMap= new HashMap<Integer, Player>();
	private State stateLobby;	
	private State stateInGame;
	private State stateTimerLobby;
	private GameState nextState;
	private NonBlockingServer server;
	private State state; 
	
	public enum GameState{DEFAULT, INGAME, LOBBY, TIMERLOBBY};
	public enum PlayerAct{DEFAULT, JOIN,QUIT,BID,CHALLENGE};

	// Constructor
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
	
	/* Drives the game based on received messages from clients.
	*  Responsible for changing state and implementing the game. 
	*  Initial state is "LOBBY".  When min number of players is reached
	*  state changes to "TIMERLOBBY".  When max wait time is over
	*  state changes to "INGAME" where the betting rounds will be held.
	*  When game is over TODO a winning message will be sent and state will
	*  be reset to "LOBBY"
	*/
	public void gameLogic(int clientId, String[] request){
		
		//1) check if a player has timed out change state request made. 
		//2) check if state has changed.
		
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
	
	//================================================================
	//     Helper Methods for Game and Game Logic
	//===============================================================
	
	
	/*Returns true if the palyer's number is found in the map. 
	 Call this method before iterating through a hashmap to
	 prevent accessing a null reference.*/ 
	public boolean isPlayerValid(int id){return playerMap.containsKey(id);}
    
	/*Rolls all dice held by a player*/
	public void rollDice(int id){playerMap.get(id).rollDice();}

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
	
	//==============================================================
	//             Getters and Setters
	//==============================================================
	
	//- Three methods are needed to send messages to all players.
	//- First, check if there is a message.  Then call the state
	// and get the message.  Then, reset "has message" to false. 
	public boolean getHasMessageToAll (){return hasAllMessage;} 
	public String  getMessageAllPlayers(){return state.sendToAll();}
	public void    setHasMessageToAll(boolean reset){hasAllMessage = reset;}
	public void    setPlayerMessage(int Id, String sendMessage){playerMap.get(Id).setPlayerMessage(sendMessage);}
	public String  getPlayerMessage(int id){return playerMap.get(id).getPlayerMessage();}


	//Returns true if client is a player and false if client is observing
	public boolean getWaitingStatus(int id){return playerMap.get(id).getWaitingStatus();} 
	public void    setWaitingStatus(int id, boolean newWaitingStatus){playerMap.get(id).setWaitingStatus(newWaitingStatus);}	
	public String  getPlayerName(int Id){ return playerMap.get(Id).getName();}
	public void    setPlayerName(int Id, String name){playerMap.get(Id).setName(name);}
	public int     getPlayerAttemptCount(int id){return playerMap.get(id).getAttemptCount();}
	public void    setPlayerAttemptCount(int id, int newValue){ playerMap.get(id).setAttemptCount(newValue);}
	public int     getMinPlayerCnt(){return minPlayers;}
	public int     getMaxPlayerCnt(){return maxPlayers;}
	public int     getTimeToWait(){return timeToWait;}
	public int     getAtcnt(){return atcnt;}
	public int     getPlayerCount() {return playerMap.size();}
	public void    setState(GameState nextState){System.err.print("in changing state");this.nextState = nextState;	}
	
	}

