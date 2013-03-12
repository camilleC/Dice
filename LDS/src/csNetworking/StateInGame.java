package csNetworking;
/**
 * Author:  Camille Ciancanelli
 * 
 * */

import java.util.*;


public class StateInGame implements State {
	
		private Game myGame;
		private String messageToAll = new String();
        public boolean kicked = false; 

		//this constructs a lobby for a specified game
		public StateInGame(Game myGame){
			this.myGame = myGame;
		}
		public int join(int id, String[] request){
			//has the player already joined? If so Trying to join again is an invalid move. 
			if (myGame.getPlayerStatus(id) != Game.PlayerStatus.CONNECTED){
				invalidMove(id);
			}
			    messageToAll = new String();
				StringBuilder sb = new StringBuilder();
				String messageToPlayer = new String();
				myGame.setPlayerName(id, new String(request[2]));
				myGame.setPlayerStatus(id, Game.PlayerStatus.WATCHING);
				sb.append("[state, in_game, ").append(id).append(", ").append(myGame.getPlayerCount());
	
				for (int i = 0; i < myGame. getMaxPlayerCnt(); i++){
	            
	            	if(myGame.isPlayerValid(i)){
					sb.append(" , ").append(myGame.getPlayerName(i)).append(", ").append(i);
	            	}
				} 
				sb.append("]");
				messageToPlayer = sb.toString();
				myGame.setPlayerMessage(id, messageToPlayer);
				myGame.clientHasMessage(id);
				myGame.setHasMessageToAll(true);
				messageToAll = "[client_joined, " +  myGame.getPlayerName(id) + " , " + id + "]";
				
				System.err.println("Game Join" + messageToAll);
				return 0;
				}
		
		//if this closes correctly have it send a message first. 
		//need to remove client from map. 
	public void quit(int id) {
		System.err.print("QUITTING" + id + "\n");
		messageToAll = new String(); //TODO DEBUG...could this be causing an empty
		myGame.setclientClose(id);
		myGame.setPlayerStatus(id, Game.PlayerStatus.REMOVE);
		myGame.setHasGone(id, true);


			//did getting the next player cause a winner? 
		    //If players turn
			if (id == myGame.getPlayerTurn()) {
				if (myGame.isWinner() == -1) {
			    	myGame.setNextPlayerTurn();
				    messageToAll = ("[client_quit, " + id + "] [player_turn, "
					+ myGame.getPlayerTurn() + "]"); 
				    myGame.setHasMessageToAll(true);
				}
				//someone quit and now there is a winner.
			    //the call to isWinner above will build a message. 
			}
			else { //if not players turn 
				myGame.isWinner();	
				messageToAll = ("[client_quit, " + id + "]");
				myGame.setHasMessageToAll(true);
				}

	};
	/*	
	Receives players bid. Sets bid in player object. Sends a bid report.
	alls for next players turn to be incremented, evaluates for end of
	round.*/           
	
    public int bid(int id, String[] request) {
        int bidDiceCount = 0;
        int bidDiceVal = 0;
        if (request.length  != 4 ) { //TODO learn how to use a try catch block instead of using an else, should it be 3
                System.err.print("invalid move wrong size" + id + " !\n");
                invalidMove(id);
        } else {
                bidDiceCount = Integer.parseInt(request[2]);
                bidDiceVal = Integer.parseInt(request[3]);
                // it is the players turn and bid is valid
                if (id == myGame.getPlayerTurn()
                                && isBidValid(bidDiceCount, bidDiceVal)) {
                        System.err.print(" *****  Just set to true" + id + "\n");
                        myGame.setHasGone(id, true);
                        myGame.setBid(id, request);
                        myGame.setNextPlayerTurn();
                        msgBidReport(id, request);
                } 
                else{
                        System.err.print("invalid your turn" + id + " game thinks this turn "+ myGame.getPlayerTurn() + "!\n");
                        invalidMove(id);
                }
        }
        return 0;
}
    // Client challenges the bid of the last player.  If the last players bid was correct the challenger looses a dice.  Otherwise, 
    //the last player looses a dice b/c they lied about their bid. 
    //If the client looses all of their dice they are removed from the list of "players" and added to the list of "watchers". 
	public void challenge(int id) {
		//if not players turn OR if player is first to go in a game they cannot challange.
	if (id != myGame.getPlayerTurn()){ //what if it is a new round and the very first person is challenging? 
		invalidMove(id);
	}
		else{
			myGame.setHasGone(id, true);
			List<Integer> lastPlayersBid = new ArrayList<Integer>();
			List<Integer> lastPlayersDice = new ArrayList<Integer>();
			int lastPlayer = myGame.getLastTurn();
			 System.err.print("in Challenge, last player is: " + lastPlayer + "\n");
			int looser = 0;
			int count = 0;
			int totalDice;
			int i;
			

			
			lastPlayersBid = myGame.getBid(lastPlayer);
			lastPlayersDice = myGame.getDice(lastPlayer);
			totalDice = myGame.getDiceCount(id);

			System.err.print(lastPlayer + "\n");
			System.err.print(lastPlayersBid.size() + "\n");
			// Evaluate who is the looser
			for (i = 0; i < totalDice; i++) {
				System.err.print("lastPlayersDice" + lastPlayersDice + "\n");
				if (lastPlayersDice.get(i) == lastPlayersBid.get(0)) {
					
					count++;
				}
			}
			if (count >= lastPlayersBid.get(0)) {
				looser = lastPlayer;
			} else {
				looser = id;
			}
            //Are there dice left?
			if (myGame.decrementDice(looser)== 0) {
				myGame.setPlayerStatus(id, Game.PlayerStatus.WATCHING); 
			}
			myGame.setNextPlayerTurn();	
			myGame.setLooser(looser);
			msgChallengeReport(id, looser);
		}
	}

	private void msgBidReport(int id, String[] request) {
		StringBuilder sb = new StringBuilder();
		messageToAll = new String(); //must reset this otherwise old messages will be stuck in it. 
		sb.append("[bid_report, " + id);
		for (int i = 2; i < request.length; i++) {
			sb.append(", ").append(request[i]);
		}
		
		// Set next players turn has already been called
		//end of round is found so don't send next players num, round start message will. 
		//if (myGame.getPlayerTurn() == myGame.firstPlayer){
		//	sb.append("]");
		//}
		//else{
		   sb.append("]").append(myGame.getPlayerTurnMessage());
		//}
		messageToAll = sb.toString();
		myGame.setPlayerMessage(id, messageToAll);
		myGame.setHasMessageToAll(true);
		//System.out.print("Bidding active, in game_bid \n");
	}
	
    
	private void msgChallengeReport(int id, int looser){
		StringBuilder sb = new StringBuilder();
		sb.append("[challenge_report, ").append(looser);

		// Set next players turn has already been called
		// end of round is found so don't send next players num, round start
		// message will.
	//	if (myGame.getPlayerTurn() == myGame.firstPlayer) {
			sb.append("]");
	//	} else {
	//		sb.append("]").append(myGame.getPlayerTurnMessage());
	//	}
		messageToAll = sb.toString();
		myGame.setPlayerMessage(id, messageToAll);
		myGame.setHasMessageToAll(true);
	}
	
		//Need to reset original message
		public String sendToAll(){
				return messageToAll;
		}
		
		public void resetMessageToAll(){
			messageToAll = new String();
		}

		/*
		if last bid was 3,2 
		bid of 4, 2 is valid
		bid of 3, 3 is valid
		bid 3,3  valid
		bid 4,1 is not valid
		TODO need to test this
	     */
		private boolean isBidValid (int currVal, int currFace){
			boolean bidOkay = false;
			int lastBidVal = myGame.getLastBidVal();
			int lastBidFace = myGame.getLastBidFace();
			
			if ((currVal == lastBidVal) && (currFace > lastBidFace)){bidOkay = true;}	
			if ((currVal > lastBidVal) && (currFace >= lastBidFace)){bidOkay = true;}	
			
			if (bidOkay){
			myGame.setLastBidVal(currVal);
			myGame.setLastBidFace(currFace);
			}
			return bidOkay;
		}
		
		//sends kicked message, sets flags so that after message is sent client connection will be closed. 
		public void kicked(int id){
			myGame.setKicked(id);
			myGame.setReadyToKick(true);
			myGame.setHasGone(id, true); //This counts as thier turn. 
			myGame.setPlayerStatus(id, Game.PlayerStatus.REMOVE);
			messageToAll = new String();
			
			
			
			//if the current player gets kicked need to signal next player
			//that it is their turn.  However, must check for a winner first. 
			if (id == myGame.getPlayerTurn()) {
				if (myGame.isWinner() == -1) {
			    	myGame.setNextPlayerTurn();
				    messageToAll = ("[client_kicked, " + id + "]"); 
				    myGame.setHasMessageToAll(true);
				}
				//someone kicked and now there is a winner.
			    //the call to isWinner above will build a message. 
				//DO NOT set a MessageToAll here. 
			}
			else { //if not players turn check if there is a winner. 
				myGame.isWinner();	
			    messageToAll = ("[client_kicked, " + id + "]"); 
			    myGame.setHasMessageToAll(true);
				}
			
	
		}
 
		//Decrements attempt count.  If to many invalid moves
		//This function will the kicked method. 
		private void invalidMove(int id){
			int attempts = myGame.getPlayerAttemptCount(id);
			System.out.print(attempts);
			attempts = attempts -1;
			System.out.print(attempts);
			if (attempts == 0){
				kicked(id);
			}
			else{
			myGame.setPlayerAttemptCount(id, attempts);
			String messageToPlayer = new String();
			messageToPlayer = ("[invalid_move," + attempts + "]");
			myGame.setPlayerMessage(id, messageToPlayer);
			myGame.clientHasMessage(id);
			}
			}
		
		};
