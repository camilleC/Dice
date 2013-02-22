/**
 * Author:  Camille Ciancanelli
 * 
 * Player represents a client.  
 *        Collects a message (i.e. has matching brackets & <= 600 chars long]).
 *        Message sent to server.
 *        Can join, quit a game, make challenge or bid or be kicked out of a game.  
 *           
 * */

import java.util.*;

public class Player {
	private String name;
	private boolean waiting = false; // observing the game or actually playing. 
	private String outMessage = new String();
	private String collectMessage;
	private String[] finalMessage;
	private int playerId;
	private int MAX_DICE = 6;
	private int diceCount = MAX_DICE;
	private List<Integer> myDice = new ArrayList<Integer>();
	private int attemptCount;

	static Random generator = new Random();
	// Getters and setters
	
	public Player(int playerId, int atcnt){
		this.playerId = playerId;
		this.attemptCount = atcnt;
	}
	
	public String getName() {
		return name;
	}
	
	//sets messages to go between the servers.  Not to 
	//be used for setting commands. 
	public void setPlayerMessage(String sendMessage){
	   outMessage = sendMessage;
	}
	
	public String getPlayerMessage(){
		return outMessage;		
		}

	public boolean getWaitingStatus(){
		return waiting;
	}
	public void setWaitingStatus(boolean newWaitingStatus){
		waiting = newWaitingStatus;
	}
	
	public int getPlayerId() {
		return playerId;
	}

	public void setDiceCount(int diceCount) {
		this.diceCount = diceCount;
	}

	public int getDiceCount() {
		return diceCount;
	}
	
	public void setName(String myName) {
		this.name = myName; 
	}

	//initial attemptCount set in constructor.  This will update it. 
	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	// Class methods
	public String bid() {
		String message = null;
		return message;
	}
	
	public String challenge() {
		String message = null;
		return message;
	}
	
	public String quit() {
		String message = null;
		return message;
	}
	
	public int roll() {
		int value = 0;
		  value =1+ generator.nextInt(6);
		return value;
	}
	public String[] getMessage(){
	return finalMessage;
	}
	
	//will need to fix this for invalid messages like [.... or ....]
	//also check to make sure it is not longer then 600
	public int setMessage(String message){
		
	boolean bracket1 = false;
	boolean bracket2 = false;
	
		collectMessage = collectMessage + message;

		char[] chars = collectMessage.toCharArray();
		for (int i = 0, n = chars.length; i < n; i++) {
		    char c = chars[i];
		    if (c == '[') bracket1 = true;
		    if (c == ']') bracket2 = true;
		}
		 
		if (bracket1 && bracket2){
			String delims = "[\\[, \\]]+";
			finalMessage = collectMessage.split(delims);
			//System.err.print("testing in set message" + finalMessagint totalDicee[1]);;
			collectMessage = null;
			return 1;
		}
		return -1;
	}
	//Pre:  Takes the number of dice the player has
	//Post: Roles all the dice. 
	public void rollDice(){
		int newValue = 1;
		int min = 1;
		int max = 6; 
		Integer value;
		Random r = new Random();
		
		for (int i = 0; i <= diceCount; i++) {
			value = r.nextInt(max - min +1) + min;
			myDice.add(value);
		}
		
		
	}
	//Returns the diceCount after decrementation
	public int decrementDice(){		
		diceCount = diceCount -1;
        return diceCount;
	}
}
