package csNetworking;
/**
 * Author:  Camille Ciancanelli
 * 
 * */

import java.util.*;



//TODO: take the new lines out. 
public class StateInGame implements State {
	
		private Game myGame;
		private String messageToAll = new String();


		//this constructs a lobby for a specified game
		public StateInGame(Game myGame){
			this.myGame = myGame;
		}
		public int join(int id, String[] request){
			    messageToAll = new String();
				StringBuilder sb = new StringBuilder();
				String messageToPlayer = new String();
				myGame.setPlayerName(id, new String(request[2]));
				myGame.setPlayerStatus(id, Game.PlayerStatus.WATCHING);
				sb.append("[state, in_game, ").append(id).append(", ").append(myGame.getPlayerCount());
	           //was playerCount()
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
				return 0;
				}
		
		//if this closes correctly have it send a message first. 
		//need to remove client from map. 
	public void quit(int id) {
		System.err.print("QUITTING" + id + "\n");
		messageToAll = new String(); // DEBUG...could this be causing an empty
		myGame.setclientClose(id);
		myGame.setPlayerStatus(id, Game.PlayerStatus.REMOVE);
		myGame.setHasGone(id, true);


			//did getting the next player cause a winner? 
			if (id == myGame.getPlayerTurn()) {
				if (myGame.isWinner() == -1) {
			    	myGame.setNextPlayerTurn();
				    messageToAll = ("[client_quit, " + id + "] [player_turn, "
					+ myGame.getPlayerTurn() + "]"); 
				    System.err.print("QUIT MeSSAGE 1 "  + messageToAll + "\n");
				}
				//someone quit and now there is a winner
				 System.err.print("QUIT MeSSAGE 2 "  + messageToAll + "\n");
			}
			else {
				System.err.print("quitting when not player turn" + id + "\n");
				
				messageToAll = ("[client_quit, " + id + "]");
			}

		myGame.setHasMessageToAll(true);
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
				myGame.setNextPlayerTurn();
				msgBidReport(id, request);
			} else {
				System.err.print("invalid your turn" + id + " game thinks this turn "+ myGame.getPlayerTurn() + "!\n");
				invalidMove(id);
			}
		}
		return 0;
	}
	/*
	 * Creates a bid report, sends this message to the game and sets flags so message will be sent. 
	 * Need to fix bid report so that players can't bid on more dice then they have.  Also need to constrain valid number choices.
	 * 
	 * */	
	private void msgBidReport(int id, String[] request) {
		StringBuilder sb = new StringBuilder();
		messageToAll = new String(); //must reset this otherwise old messages will be stuck in it. 
		//myGame.setBid(request, id);  //moving this to bid func.  did I cause a bug?
		sb.append("[bid_report, " + id);
		for (int i = 2; i < request.length; i++) {
			sb.append(", ").append(request[i]);
		}
		
		//end of round is found so don't send next players num, round start message will. 
		if (myGame.getPlayerTurn() == myGame.firstPlayer){
			sb.append("]");
		}
		else{
		   sb.append("]").append(myGame.getPlayerTurnMessage());
		}
		messageToAll = sb.toString();
		myGame.setPlayerMessage(id, messageToAll);
		myGame.setHasMessageToAll(true);
		//System.out.print("Bidding active, in game_bid \n");
	}
		
		public int challenge(Map<Integer, Player> players, int id, String[] request){return 0;}
		public String sendToClient(Map<Integer, Player> players, int id){return "not implimented";}

		
		//Need to reset original message
		public String sendToAll(){
				return messageToAll;
		}
		
		public void resetMessageToAll(){
			messageToAll = new String();
		}
		public void changeState(Game.GameState newState){
			//myGame.setState(Game.GameState.LOBBY);
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

	    // TODO> is this specific to this class? Could a player get kicked somewhere else? 
		// if specific to this class then make private
		// can only kick if a bid timed out. 
		
		private void kicked(int id){
			myGame.setclientClose(id);
			messageToAll = ("[client_kicked, " + id + "]");
			myGame.setPlayerStatus(id, Game.PlayerStatus.REMOVE);
			if (id == myGame.getPlayerTurn()){ //if the current player gets kicked signal the next player that it is thier turn. 
				 myGame.setNextPlayerTurn(); 
			     myGame.setHasMessageToAll(true);
			}
		}
		//maybe I should pull this out and stick in game logic...all states use same message. 
		private void invalidMove(int id){
			int attempts;
			attempts = myGame.getPlayerAttemptCount(id);
			attempts = attempts -1;
			
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
		};	
	}
