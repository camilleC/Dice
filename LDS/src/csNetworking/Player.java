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
	
	private int playerId;
	private int MAX_DICE = 5;
	private int diceCount = MAX_DICE;
	private int attemptCount = 3; 
	boolean timeOut = false;
	private boolean hasGone = false;
	private boolean startTimer = true;
	private long collectMessageStartTime = 0;
	private long collectMessageEndTime = 0;
	private long maxCollectionTime = 0;
	private String name;
    private String outMessage = new String();
	private String collectMessage;
	private String[] finalMessage;
	static Random generator = new Random();
	private List<Integer> myDice = new ArrayList<Integer>();
	private List<Integer> myBids = new ArrayList<Integer>();
	private Game.PlayerStatus playerStatus = Game.PlayerStatus.CONNECTED; 
	
	//Constructor
	public Player(int playerId, int atcnt, long maxCollectionTime){
		this.playerId = playerId;
		this.attemptCount = atcnt;
		this.maxCollectionTime = maxCollectionTime;
	}
	
	
	/////////  TIMING NOT TESTED YET CHECK CONVERSIONS //////////////////
	private void startTime(){
		collectMessageStartTime =  System.currentTimeMillis( );
	}
	
	private void endTime(){
		collectMessageEndTime =  System.currentTimeMillis( );
	}
	
    public boolean timedOut(){return timeOut;}
	
    private boolean timeIt(){
		//of they are both zero collection process has not started yet. 
		if ((collectMessageEndTime + collectMessageStartTime) > 0){
			if (elapsedTime() >= maxCollectionTime){
			timeOut = true;
			}
		}    	
    	return timeOut;
    	
    }
    
    //TODO are these numbers correct?  Is there supposed to be a conversion? 
	private long elapsedTime(){
		long elapsed = collectMessageEndTime - collectMessageStartTime ;
		return elapsed;
	}
	///////////////////////////////////////////////////////////////////////
	
	
	// This just collects a message for a given time period. It does not
	// validate data.
	public int setMessage(String message) {
		int errorValue = 0; // 0 = message ok, -1 means too long.
		boolean bracket1 = false;
		boolean bracket2 = false;

		if (startTimer) {
			startTime();
			startTimer = false;
		}

		collectMessage = collectMessage + message;

		char[] chars = collectMessage.toCharArray();
		if (chars.length <= 600) { //message need to be less than 600 characters long
			for (int i = 0, n = chars.length; i < n; i++) {
				char c = chars[i];
				if (c == '[')
					bracket1 = true;
				if (c == ']')
					bracket2 = true;
				
				//timeUp could happen here...may wait forever for final bracket.....
				timeIt();
			}
		} else {
			errorValue = -1;
		}

		if (chars.length <= 600) {
			if (bracket1 && bracket2) {
				String delims = "[\\[, \\]]+"; // should I NOT have the plus????
												// there should just be one set
												// of brackets per message.
				finalMessage = collectMessage.split(delims);
				collectMessage = null;
				// finalBracketFound. Stop collection timer
				// reset to collection mode.
	            timeIt(); // is it okay to have it in two places? 
				startTimer = true;
				collectMessageStartTime = 0;
				collectMessageEndTime = 0;
				return 1;
			}
		} else {
			errorValue = -1;
		}
		return errorValue;
	}
	
	
	public void setPlayerMessage(String sendMessage){
        outMessage = sendMessage;
     }
    public String getPlayerMessage(){
        return outMessage;              
        }
    
	public String getName() {
		return name;
	}
 //TODO i < bids.length-1 b/c i = 0 is null, why??? and i = 1 is "bid".  
	//change this, a bid is only two numbers.  Opps!
	public void setBid(String[] bids){
		for (int i = 2; i < bids.length; i++) {
			myBids.add(Integer.parseInt(bids[i]));
		}
		System.out.print(myBids.get(0) + " " + myBids.get(1));
		
	}
	
	public List<Integer> getBid(){return myBids;}
	public List<Integer> getDice(){return myDice;}
	
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
		System.out.print("in palyer " + attemptCount + "\n");
		return attemptCount;
	}

	public boolean getHasGone(){return hasGone;}
	public void setHasGone(boolean newVal){hasGone = newVal;} 
	
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
	

	
	public String diceMessage(){
		//System.err.print("myPlayernum: " + playerId + " myDiceCount " + getDiceCount());
		String myMessage;
		StringBuilder sb = new StringBuilder();
		sb.append("[your_dice, ").append(getDiceCount());
        for (int i = 0; i < getDiceCount(); i++){
			//System.err.print("dice num" + i+ " value " + myDice.get(i) + "\n");
        	sb.append(", ").append(myDice.get(i));
        	}
	    sb.append("]");
	    myMessage = sb.toString();
		return myMessage;
	}
	
	//can use this for end of round dice counts
	public String getPlayersDiceMessage(){
		String myMessage;
		StringBuilder sb = new StringBuilder();
		sb.append(getDiceCount());
        for (int i = 1; i < getDiceCount()-1; i++){
        	sb.append(", ").append(myDice.get(i));
        	}
        myMessage = sb.toString();	 
		return myMessage;
	}
	
	
	//Pre:  Takes the number of dice the player has
	//Post: Roles all the dice. 
	public void rollDice(){
		int min = 1;
		int max = 6; 

		Integer value;
		Random r = new Random();
		
		for (int i = 0; i <= diceCount; i++) {
			value = r.nextInt(max - min +1) + min;
			myDice.add(value);
		}
	}
	
   /**
    * Decrements dice count till player has no dice.
    * Returns the count of remaining dice. 
    * */
	public int decrementDice(){		
		if (diceCount >= 1){
		diceCount = diceCount -1;
		myDice.remove(0); 
		}
        return diceCount;
	}
}
