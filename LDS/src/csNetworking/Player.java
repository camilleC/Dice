package csNetworking;
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
	
	private Game.PlayerStatus playerStatus = Game.PlayerStatus.CONNECTED; 
	private String outMessage = new String();
	private String collectMessage;
	private String[] finalMessage;
	private int playerId;
	private int MAX_DICE = 6;//TODO remove hard code
	private int diceCount = MAX_DICE;
	private List<Integer> myDice = new ArrayList<Integer>();
	private List<Integer> myBids = new ArrayList<Integer>();
	private int attemptCount = 3; //TODO remove hard code
	//private boolean myTurn = false;

	static Random generator = new Random();
	// Getters and setters
	
	public Player(int playerId, int atcnt){
		this.playerId = playerId;
		this.attemptCount = atcnt;
	}
	
	public String getName() {
		return name;
	}
 //TODO i < bids.length-1 b/c i = 0 is null, why??? and i = 1 is "bid".  
	public void setBid(String[] bids){
		for (int i = 2; i < bids.length-1; i++) {
			//System.err.print(i + " " + bids[i] + "\n");
			myBids.add(Integer.parseInt(bids[i]));
		   }
		}
	
	public List<Integer> getBid(){return myBids;}
	
	//public void setMyTurn(boolean turn){
	//	myTurn = turn;
	//}
	
	///public boolean getMyTurn(){return myTurn;}
	//sets messages to go between the servers.  Not to 
	//be used for setting commands. 
	public void setPlayerMessage(String sendMessage){
	   outMessage = sendMessage;
	}
	
	public String getPlayerMessage(){
		return outMessage;		
		}

	public Game.PlayerStatus getPlayerStatus(){
		return playerStatus;
	}
	public void setPlayerStatus(Game.PlayerStatus newWaitingStatus){
		playerStatus = newWaitingStatus;
	}
	
	public int getPlayerId() {
		return playerId;
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
	
	//TODO will need to fix this for invalid messages like [.... or ....]
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
	
	public String diceMessage(){
		//System.err.print("myPlayernum: " + playerId + " myDiceCount " + getDiceCount());
		String myMessage;
		StringBuilder sb = new StringBuilder();
		sb.append("[your_dice, ").append(getDiceCount());
        for (int i = 1; i < getDiceCount()-1; i++){
			//System.err.print("dice num" + i+ " value " + myDice.get(i) + "\n");
        	sb.append(", ").append(myDice.get(i));
        	}
	    sb.append("]");	
		return myMessage = sb.toString();	 
	}
	
	//can use this for end of round dice counts
	public String getPlayersDiceMessage(){
		//System.err.print("myPlayernum: " + playerId + " myDiceCount " + getDiceCount());
		String myMessage;
		StringBuilder sb = new StringBuilder();
		sb.append(getDiceCount());
        for (int i = 1; i < getDiceCount()-1; i++){
			//System.err.print("dice num" + i+ " value " + myDice.get(i) + "\n");
        	sb.append(", ").append(myDice.get(i));
        	}
        myMessage = sb.toString();	 
		return myMessage;
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
	
	// TODO: insetead of decrementing count try uinsg myDice.size() after the removal
	// Returns the diceCount after decrementation
	public int decrementDice(){		
		if (diceCount >= 1){
		diceCount = diceCount -1;
		myDice.remove(0); // remove the first dice, shifts all otherds to the left.
		}
		else{
			System.err.print("Error: no dice left to decrement."); //TODO prevent this situation from occuring and then delete his message. 
		}
		
		//They have no more dice left.  Change status to "watching".
		if (diceCount == 0){
			setPlayerStatus(Game.PlayerStatus.WATCHING);
		}
        return diceCount;
	}
}
