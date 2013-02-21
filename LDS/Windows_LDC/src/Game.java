
//import java.util.ArrayList;
import java.io.*;
import java.util.Map;
import java.util.HashMap;

public class Game {
	private String[] argsIn = null;
	private int portNum;
	private int minPlayers;
	private int maxPlayers;
	private int timeToWait;
	private int atcnt;
	private int playerWithMessage = -1;
	private boolean hasAllMessage = false;



	public enum GameState{DEFAULT, INGAME, LOBBY, TIMERLOBBY};
	public enum PlayerAct{DEFAULT, JOIN,QUIT,BID,CHALLENGE};

	private Map<Integer, Player> playerMap= new HashMap<Integer, Player>();
	private State stateLobby;	
	private State stateInGame;
	private State stateTimerLobby;
	private GameState nextState;
	private NonBlockingServer server;
	private State state; 


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

	//===========================================
	// Getters and setters for Game
	//===========================================

	public int getPlayerCount() {
		return playerMap.size();
	}

	public void setState(GameState nextState){
		System.err.print("in changing state");
		this.nextState = nextState;	
		}			
		
	public int getMinPlayerCnt(){return minPlayers;}
	public int getMaxPlayerCnt(){return maxPlayers;}
	public int getTimeToWait(){return timeToWait;}
	public int getAtcnt(){return atcnt;}
	private void removePlayer(Player player){/*playerList.remove(player)*/;}
    public void clientHasMessage(int id){playerWithMessage = id;}

	// Returns player# with a message or -1 if no message---------
	public int getPlayerNumWithMessage(){
		return playerWithMessage;}
	public void resetPlayerNumWithMessage(){playerWithMessage = -1;
	}

	// Adds a player to the PlayerQ and makes a player object.
	// This does not "join" the player to the game. 
	// Player will recieve all messages.
	public void addPlayer(int playerID){
		Player newPlayer = new Player(playerID, atcnt);
		playerMap.put(playerID, newPlayer);
	}

	//will this remove the player but keep the key ordering the same? 
	public void removePlayer(int playerID){playerMap.remove(playerID);}
	
	//- Three methods are needed to send messages to all players.
	//- First, check if there is a message.  Then call the state
	// and get the message.  Then, reset "has message" to false. 
	public boolean getHasMessageToAll (){return hasAllMessage;} 
	public String  getMessageAllPlayers(){return state.sendToAll();}
	public void setHasMessageToAll(boolean reset){hasAllMessage = reset;}


	//==================================================
    // Methods calling Player getter and setters
    //===================================================

	//----player message -----------------------
	public void setPlayerMessage(int Id, String sendMessage){playerMap.get(Id).setPlayerMessage(sendMessage);}
	public String getPlayerMessage(int id){return playerMap.get(id).getPlayerMessage();}


	//-----returns true if player is player
	//-----returns false if player is observing
	public boolean getWaitingStatus(int id){return playerMap.get(id).getWaitingStatus();}
	public void setWaitingStatus(int id, boolean newWaitingStatus){playerMap.get(id).setWaitingStatus(newWaitingStatus);}

	//-----player Name  --------------------------------
	
	
	public String getPlayerName(int Id){ return playerMap.get(Id).getName();}
	public void setPlayerName(int Id, String name){playerMap.get(Id).setName(name);}
	
	public int getPlayerAttemptCount(int id){return playerMap.get(id).getAttemptCount();}
	public void setPlayerAttemptCount(int id, int newValue){ playerMap.get(id).setAttemptCount(newValue);}
	
	public boolean isPlayerValid(int id){return playerMap.containsKey(id);}

	public void rollDice(int id){playerMap.get(id).rollDice();}
	//================================================================
	//     methods for starting, ending, and parsing game info
	//===============================================================

	public void serverStart(){
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setclientClose(int id){
		   server.setClientClose();
		   server.clientClose(id);
	}

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
			recieveAction(new Integer(clientId), playerMap.get(clientId).getMessage());
		}
	}
	//  Message translated into action ----------------------------------------
	public void recieveAction(int clientId, String[] request){
		
		//First check if state has changed. 
		if (nextState == GameState.LOBBY){this.state = stateLobby;}
		else if (nextState == GameState.TIMERLOBBY){this.state = stateTimerLobby;}
		else if (nextState == GameState.INGAME){this.state = stateInGame;}	
		
		PlayerAct message= Parser.parse(clientId, request);
		switch(message){
		case DEFAULT: 
			//will need to send a message to client saying bad message;
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

}
