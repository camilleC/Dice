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
 *        "Hash table and linked list implementation of the Map interface, with predictable iteration order."
 *TODO does game shut of the server or does main? I think main does b/c main starts the server.   
 *TODO change any for loop that uses an i to index into player map to  "for (Integer key : playerMap.keySet()"
 *This will prevent accidentally accessing an object that isn't there. Also can't use 'i' b/c player numbers are crazy. 
 *
 * */


import java.io.*;
import java.util.*;

public class Game {
	private String[] argsIn = null;
	private int portNum;
	private int minPlayers = 2; 
	int maxPlayers = 30;
	public int timeToWait = 1; //set this back to 60 as a default. 10 is here for testing..I don't want to wait 60 seconds //TODO change to private
	public boolean unblock = false;
	private long startLobbyTime;  
	//private long endLobbyTime;  
	private int atcnt = 3;
	private boolean timerOn = false;
	private boolean sendRoundMsg = false;
	private int isWinner = -1;
	private int isLooser = -1;
	private boolean sendDiceMsg = false;
	private int playerWithMessage = -1;
	private boolean hasAllMessage = false;
	private boolean roundEndMessage = false; 
	private boolean hasAllMessageGameLogic = false;
	private String messageFromGameLogic;
	private Map<Integer, Player> playerMap= new LinkedHashMap<Integer, Player>(); //TODO: Determine time space complexity
	private Integer[]turnArray;
	private State stateLobby;	
	private State stateInGame;
	private State stateTimerLobby;
	private GameState nextState;
	private NonBlockingServer server;
	private State state; 
	private int whoseTurn = -1;
	private int lastTurn = 0;
	private int previousTurn = 0; //TODO make this a private and add a getter
	public int firstPlayer = -2; //TODO make this private and put in a getter. 
	public int playersInRound = -2;
	private int lastBidVal;  //should these be static?  
	private int lastBidFace;
    private int playerToKick;
    private boolean kickMe = false;
    private boolean needsRestart = false;
	public enum GameState{DEFAULT, INGAME, LOBBY, TIMERLOBBY};
	public enum PlayerAct{DEFAULT, JOIN,QUIT,BID,CHALLENGE};
	public enum PlayerStatus{CONNECTED, PLAYING, WATCHING, REMOVE};

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
		
		if (needsRestart){
			restartGame();
		}
		
		//TODO was ==..but we may have more then min number of players. 
	    if ((getPlayerCount() >= minPlayers) && (timerOn == false)){
           state = stateTimerLobby;
           timerOn = true;
           System.err.print("turning timer on \n");
           sendTimerMessage();
           startTime();
		}
	    
	    //timeOutOver(); //check if the timeout is over. Doing this in server now 
		
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
			break; //was missing this.  Caused lots of bugs. 
		case BID:
			state.bid(clientId, request);
			break;
		case CHALLENGE:
			state.challenge(clientId);
			break;
		} 
	
	//////////////////////////////////////////////////////////
    // EVALUATE Round end.  Look for a winner
	//////////////////////////////////////////////////////////
		if (roundEnd()) {
			System.err.print(" AAAAAAAAAAa  \n");
			// If no winner reset for next round
			if (-1 == isWinner()) {
				setHasGone();
				roundEndMessage = true;
				sendRoundMsg = true;
				lastBidVal = 0;
				lastBidFace = 0;
			}
		}
			
		//////////////////////////////////////////////////////////
	    // EVALUATE Start of round,
        //////////////////////////////////////////////////////////
		if (sendRoundMsg) {
			for (int i = 0; i < maxPlayers; i++){
	        	if(isPlayerValid(i)){
	             playerMap.get(i).rollDice();
	        	}
			}
	
			System.err.print("calling round start mesage\n");
	    	roundStartMsg();
	    	sendRoundMsg = false;
	    	sendDiceMsg= true;
	    }
	}
	
	//Evaluates if there is a winner.
	//returns -1 if no winner yet (game still on)
	//or player number if there is a winner. 
	public int isWinner() {
		System.err.print("IN IS WINNER" + "\n");
		if (getCountPlayersMakingMoves() == 1) {
			for (Integer key : playerMap.keySet()) {
				if ((playerMap.get(key).getPlayerStatus() == PlayerStatus.PLAYING)) {
					isWinner = key;
					endGame(key);
					System.err.print("YOU WON PLAYER : " + key + "\n");
				}
			}
		}
		return isWinner;
	}
	
	public boolean timeOutOver(){
		boolean unblock = false;
	    if (state != stateInGame) {
			if (timerOn & elapsedTime(startLobbyTime) >= timeToWait) {
				System.err.print("THIS SHOULD ONLY BE SEEN ONCE IN A GAME \n");
				state = stateInGame;
				reSetStatus();  //game starts, set players from waiting to playing
				setTurnOrder(); //set order of turns for players. 
				playersInRound = getCountPlayersMakingMoves(); // count of players making moves at the start of game. 
				sendRoundMsg = true;
				unblock = true;
			}
		}
	    return unblock;
		}


	//Summarizes end of game - last player with >= 1 dice remaining
	private void endGame(int winner){
		messageFromGameLogic = new String();
		messageFromGameLogic =  "[game_end, " + winner + "]";
		setHasMessageToAllFromGameLogic(true);
		needsRestart = true;
	}
	
	private void restartGame(){
		//This was in end game. FRIDAY NIGHT 3:8:13 8:30 pm.  
		//Reset game to original status.  
		//Reset players to original status when the game begins, not here.  
		this.state = stateLobby;
		isWinner = -1;
		firstPlayer = -2; //TODO make this private and put in a getter. 
		playersInRound = -2;
		lastBidVal = 0; 
		lastBidFace = 0;
		timerOn = false; //reset timer so the lobbyTimer can be entered for the next game
		needsRestart = false;
		//TODO can I remove from the the player map here? 
	}

   // returns true if all players have gone.  Else, returns false. 
	//TODO Fix this, I am not sure if it is working correctly. 
	private boolean roundEnd() {
		System.err.print("IN ROUND END? " + "\n");
		boolean end = false;
		int playersGone = 0;
		int id = 0;
		int playersInRound = 0;
		for (Integer key : playerMap.keySet()){
			if ((playerMap.get(key).getPlayerStatus() == PlayerStatus.PLAYING) && (playerMap.get(key).getHasGone() == true )){
               playersGone++;
               id = key;
			}
		}
		
		playersInRound = getCountPlayersMakingMoves();
        
		//everyone has gone.
		if (playersInRound == playersGone){
			end = true;
		} 
		
		//CAMILLE ADDED Friday night.  I may have introduced a bug.  Check it. 
		//only one player remains so they should win by default. 
		//Add a unit test? 
	//	if (playersInRound-1 == playersGone){
	//	end = true;	
	//	}
 
		return end;
	}

	
	
	//Resets "hasGone" to false so that players can start a new round. 
	private void setHasGone() {
		System.err.print("IN RESET TURNS? " + "\n");
		for (Integer key : playerMap.keySet()){
			if ((playerMap.get(key).getPlayerStatus() == PlayerStatus.PLAYING)){
				playerMap.get(key).setHasGone(false);
			}
				
		}
	}
	
	//players who joined while the game was in round need to be switched to playing. 
	private void reSetStatus() {
		System.err.print("IN RESET STATUS? " + "\n");
		for (Integer key : playerMap.keySet()){
			if ((playerMap.get(key).getPlayerStatus() == PlayerStatus.WATCHING)){
				playerMap.get(key).setPlayerStatus(PlayerStatus.PLAYING);
			}
				
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
		messageFromGameLogic = new String(); 
        System.err.print("In send timer message \n");
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
	
	
	public void roundStartMsg(){
		System.err.print("  IN ROUND START MESSSAGE" +  messageFromGameLogic +  " \n");
		messageFromGameLogic = new String(); 
		String tempString = new String();
		StringBuilder sb = new StringBuilder();
		sb.append("[round_start, ").append(getPlayerCount());
		
        for (int i = 0; i < maxPlayers; i++){
        	if(isPlayerValid(i)){
			sb.append(" , ").append(i).append(", ").append(playerMap.get(i).getDiceCount()); 
        	}
		}
        sb.append("]" + getPlayerTurnMessage());
       
        if (roundEndMessage){
        	tempString = sb.toString();
        	messageFromGameLogic = roundEndMessage() + tempString;
        	if (isWinner != -1){
        		messageFromGameLogic += "[game_end, " + isWinner + "]";
        		isWinner = -1;
        	}
        	roundEndMessage = false;
        }
        else
	  
        messageFromGameLogic =  sb.toString();
		setHasMessageToAllFromGameLogic(true);
		return;		
	}

	//working on round end message
	private String roundEndMessage(){
		   System.err.print("  IN ROUND END MESSAGE  "+  " \n");
		String message = new String();
		StringBuilder sb = new StringBuilder();
		message = "[round_end, " + isLooser + ", " + getPlayerCount();
		
        for (int i = 0; i < maxPlayers; i++){
        	if(isPlayerValid(i)){
            playerMap.get(i).rollDice();
			sb.append(" , ").append(i).append(", ").append(playerMap.get(i).getPlayersDiceMessage()); 
        	}
		}
        sb.append("]  ");  
        message = message + sb.toString();
		return message;
	}
	
	/**msg_Player_Turn()       
	[player_turn,player#]
	Sample: [player_turn, 2]
	*/
	public String getPlayerTurnMessage() {
		String message = null;
		StringBuilder sb = new StringBuilder();
		sb.append("[player_turn, " + getPlayerTurn() + "]");
		message = sb.toString();
		return message;
	}

	//
	public int getPlayerTurn(){
		//no one has gone yet, must set here
		//due to loop logic in setNextPlayerTurn
		if (whoseTurn == -1){
			whoseTurn = 0;

		}
		System.err.print(" +++++++++++++++ WHOES TURN " + whoseTurn + " \n");
		return whoseTurn;
	}
	
	
	/**Determines next palyer's turn.  If the end of list is reached
	* Turn is equal to the first entry of the list.  
	*/
	public int setNextPlayerTurn() {
		int i;
		// previous turn is the person before turn is updated.
		// after the update lastTurn = whose turn so must keep
		// track of predecessor.
		previousTurn = whoseTurn;

		System.err.print("  +++++++++++++  previous Turn" + previousTurn + "\n");
		for (i = lastTurn; i < maxPlayers; i++) {
			if (isPlayerInRound(turnArray[i])) {
				if (playerMap.get(i).getHasGone() == false) {
					whoseTurn = turnArray[i];
					lastTurn = whoseTurn;
					break;
				}
			}
			// end of list, reset to start.

			if (i == 29) {
				whoseTurn = 0;
				lastTurn = 0;
			}
		}
		System.err.print("   +++++++++  next turn has been set  to " + whoseTurn + "\n");
		return whoseTurn;
	}

	//===============================================================
	//      Public methods  --why are there some privates here? Fix!
	//===============================================================
	
	
	/*Returns true if the player is observing OR playing*/
	public boolean isPlayerValid(int id){
		boolean valid = false;
		if (playerMap.containsKey(id)) { //must check this first.
			if ((playerMap.get(id).getPlayerStatus() == PlayerStatus.PLAYING) ||
			     playerMap.get(id).getPlayerStatus() == PlayerStatus.WATCHING) {
				valid = true;
			}
		}
		return valid;
		}
    
	//Returns true if Client is playing game.  	
	public boolean isPlayerInRound(int id) {
		boolean valid = false;
		if (playerMap.containsKey(id)) {
			if ((playerMap.get(id).getPlayerStatus() == PlayerStatus.PLAYING)) {
				valid = true;
			}
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
		   server.clientClose(id);

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
	

	//Sets the client ID of player with a message
    public void clientHasMessage(int id){
    	playerWithMessage = id;}

	/*Returns player# with a message or -1 if no message */
	public int getPlayerNumWithMessage(){
		return playerWithMessage;}
	
	//Call after a message has been set to a player
	public void resetPlayerNumWithMessage(){
		playerWithMessage = -1;
	}

	//Adds player to the PlayerQ but does not "join" them to the game 
	public void addPlayer(int playerID){
		Player newPlayer = new Player(playerID, atcnt);
		playerMap.put(playerID, newPlayer);
	}

	//Removes all players who's player status is "remove"
	public void removePlayers() {
		for (Integer key : playerMap.keySet()){
			if ((playerMap.get(key).getPlayerStatus() == PlayerStatus.REMOVE)) {
				playerMap.remove(key);
				System.err.print("player removed : " + key + "\n");
				}
			}
	}
	
	//Places the order in which players take turns into the turnArray.
	private void setTurnOrder(){
		//get rid of the old one, new game, new list
		turnArray = new Integer[maxPlayers];
		int i = 0;
		//Initialize array to all -1
		for (i = 0; i < maxPlayers; i++){
			turnArray[i] = -1;
			}
		for (Integer key : playerMap.keySet()){
		   turnArray[(int)key] = (int)key;
		}
		firstPlayer = turnArray[0];
	}

	//returns number of players who are allowed to bid, challenge, or watch. 
	//Watchers are people who have joined to late to play or are players who 
	//lost their dice and are now just watching. 
	public int getPlayerCount() {
		int count = 0;
		for (Integer key : playerMap.keySet()){
			if ((playerMap.get(key).getPlayerStatus() == PlayerStatus.PLAYING)||(playerMap.get(key).getPlayerStatus() == PlayerStatus.WATCHING)){
				count++;
			}
		}
		return count;
	}

	//returns number of players who are allowed to bid or challenge. 
	private int getCountPlayersMakingMoves() {
		int count = 0;
		for (Integer key : playerMap.keySet()){
			if ((playerMap.get(key).getPlayerStatus() == PlayerStatus.PLAYING)){
				count++;
			}
		}
		return count;
	}
	
	//==============================================================
	//             Getters and Setters
	//==============================================================
	

	//Messages from a state
	public boolean getHasMessageToAll (){return hasAllMessage;} 
	public String  getAllPlayerMessageFromState(){return state.sendToAll();} //this one giving us client join
	public void    setHasMessageToAll(boolean reset){hasAllMessage = reset;}

	// Method player message  
	public String  getPlayerMessage(int id){return playerMap.get(id).getPlayerMessage();}
	public void    setPlayerMessage(int Id, String sendMessage){playerMap.get(Id).setPlayerMessage(sendMessage);}

	
	//Messages from the game to all clients
	public boolean getHasMessageToAllFromGameLogic() {return hasAllMessageGameLogic;}
	public String  getAllPlayerMessageFromGameLogic(){return messageFromGameLogic;} 
	public void    resetMessageFromGameLogicString() {messageFromGameLogic = new String();}  //CAMILLE ADDED WILL THIS FIX REAPEATING MESSAGE?
	public void    setHasMessageToAllFromGameLogic(boolean reset){hasAllMessageGameLogic = reset;}
	
	// Methods for dice 
	public void  setDiceMsg(){sendDiceMsg = false;}
	public boolean  getDiceMsg(){return sendDiceMsg;}
	public String getPlayerDiceMessageFromState(int id){return playerMap.get(id).diceMessage();}
	public List<Integer> getDice(int id){return playerMap.get(id).getDice();}
	public int getDiceCount(int id){return  playerMap.get(id).getDiceCount();}
	public int decrementDice(int id){return playerMap.get(id).decrementDice();}
	

	
	
	//Returns true if client is a player and false if client is observing
	public PlayerStatus getPlayerStatus(int id){return playerMap.get(id).getPlayerStatus();} 
	public void    setPlayerStatus(int id, PlayerStatus newStatus){playerMap.get(id).setPlayerStatus(newStatus);}	
	public String  getPlayerName(int Id){ return playerMap.get(Id).getName();}
	public void    setPlayerName(int Id, String name){playerMap.get(Id).setName(name);}
	public int     getPlayerAttemptCount(int id){return playerMap.get(id).getAttemptCount();}
	public void    setPlayerAttemptCount(int id, int newValue){ playerMap.get(id).setAttemptCount(newValue);}
	public int     getMinPlayerCnt(){return minPlayers;}
	public int     getMaxPlayerCnt(){return maxPlayers;}
	public int     getTimeToWait(){return timeToWait;}
	public int     getAtcnt(){return atcnt;}
	public void    setState(GameState nextState){System.err.print("in changing state");this.nextState = nextState;}
	public void setBid(int id, String[] bids){playerMap.get(id).setBid(bids);}
	public List<Integer> getBid(int id){return playerMap.get(id).getBid();}
	public int getLastBidVal() {return lastBidVal;}
	public int getLastBidFace() {return lastBidFace;}
	public void setLastBidVal(int currentBidVal) {lastBidVal = currentBidVal;}
	public void setLastBidFace(int currentBidFace) {lastBidFace = currentBidFace;}
	public boolean getHasGone(int id){return playerMap.get(id).getHasGone();}
	public void setHasGone(int id, boolean newVal){playerMap.get(id).setHasGone(newVal);}
	public void setKicked(int id){this.playerToKick = id;}
	public void setReadyToKick(boolean kickme){this.kickMe = kickme;}
	public boolean getReadyToKick(){return kickMe;}
    public int getKicked() {return this.playerToKick;}
    public int getLastTurn(){return this.previousTurn;}
    public void reSetSendRoundMessage(){sendRoundMsg = false;}
    public Integer[] getTurnArray(){return turnArray;}
}

