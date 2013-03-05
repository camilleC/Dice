package csNetworking;
/**
 * Author:  Camille Ciancanelli
 * 
 * */

import java.util.*;



//TODO: take the new lines out. 
public class StateInGame implements State {
	
		private Game myGame;
		private boolean timerOn = false;
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
	            for (int i = 0; i < myGame.getPlayerCount(); i++){
	            
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
		// ///////////////////////////
		myGame.turns++; // this is a hack
		// ////////////////////////////
		messageToAll = new String(); // DEBUG...could this be causing an empty
										// string?
		myGame.setclientClose(id);
		myGame.setPlayerStatus(id, Game.PlayerStatus.REMOVE);

		// If I quit when it is my turn then set the next players turn.
		// otherwise let who ever is playing keep thier turn.

		// removing the player made a winner.
		//if there is a winner say the client quit
		if (myGame.isWinner() != -1) {
			messageToAll = ("[client_quit, " + id + "]");
		} 
		//if there is not a winner check two conditions.
		else {
			//players turn
			if (id == myGame.getPlayerTurn()) {
				myGame.setNextPlayerTurn();
				messageToAll = ("[client_quit, " + id + "] [player_turn, "
						+ myGame.getPlayerTurn() + "]"); // this is repeating
															// the message. Bad!
															// Refractor ths.
				System.err.print("quitting on player turn" + id + "\n");
				//not players turn
			} else {
				System.err.print("quitting when not player turn" + id + "\n");
				messageToAll = ("[client_quit, " + id + "]");
			}
		}
		myGame.setHasMessageToAll(true);
	};
		
		//Receives players bid.  Sets bid in player object.  Sends a bid report. 
		//calls for next players turn to be incremented, evaluates for end of round. 
		public int bid(int id, String[] request){
			if (id == myGame.getPlayerTurn()){
          /////////////////////////////
            myGame.turns++; // this is a hack
           //////////////////////////////
			  myGame.setNextPlayerTurn(); 
			  msgBidReport(id, request); //is this the order?
			}
			else{
				System.err.print("invalid move player" + id + " !\n");
				invalidMove(id);
			}
			return 0;}
	/*
	 * Creates a bid report, sends this message to the game and sets flags so message will be sent. 
	 * */	
	private void msgBidReport(int id, String[] request) {
		StringBuilder sb = new StringBuilder();
		messageToAll = new String(); //must reset this otherwise old messages will be stuck in it. 
		myGame.setBid(request, id);
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
		

	    // TODO> is this specific to this class? Could a player get kicked somewhere else? 
		// if specific to this class then make private
		// can only kick if a bid timed out. 
		
		public void kicked(int id){
			myGame.setclientClose(id);
			messageToAll = ("[client_kicked, " + id + "]");
			myGame.setPlayerStatus(id, Game.PlayerStatus.REMOVE);
			if (id == myGame.getPlayerTurn()){ //if the current player gets kicked signal the next player that it is thier turn. 
				 myGame.setNextPlayerTurn(); 
			     myGame.setHasMessageToAll(true);
			}
		}
		
		public void invalidMove(int id){
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
