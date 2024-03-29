package csNetworking;

/**
 * Author:  Camille Ciancanelli
 * 
 * */


public class StateTimerLobby implements State {
	private Game myGame;
	private String messageToAll;

	// this constructs a lobby for a specified game
	public StateTimerLobby(Game myGame) {
		this.myGame = myGame;
	}

	// TODO Send to many clients
	public int join(int id, String[] request) {
		
		//has the player already joined? If so Trying to join again is an invalid move. 
		if (myGame.getPlayerStatus(id) != Game.PlayerStatus.CONNECTED){
			invalid(id);
		}
		
		messageToAll = new String();// ADDED MON NIGHT
		StringBuilder sb = new StringBuilder();
		// String tempMessage = new String();
		String messageToPlayer = new String();
		myGame.setPlayerName(id, new String(request[2]));
		myGame.setPlayerStatus(id, Game.PlayerStatus.PLAYING);
		sb.append("[state, timer_with_lobby, ").append(id).append(", ")
				.append(myGame.getPlayerCount());
		for (int i = 0; i < myGame.getPlayerCount(); i++) {

			if (myGame.isPlayerValid(i)) {
				sb.append(" , ").append(myGame.getPlayerName(i)).append(", ")
						.append(i);
			}
		}
		sb.append("]");
		messageToPlayer = sb.toString();
		myGame.setPlayerMessage(id, messageToPlayer);
		myGame.clientHasMessage(id);
		myGame.setHasMessageToAll(true);
		messageToAll = "[client_joined, " + myGame.getPlayerName(id) + " , "
				+ id + "]";
		
		System.err.println("Lobby Timer Join " + messageToAll);
		return 0;
	}


	// players will recieve invalid move message if they bid or challenge while
	// in the lobby
	public int bid(int id, String[] request) {
		invalid(id);
		return 0;
	}

	public void challenge(int id) {invalid(id);}

	public void quit(int id){
		messageToAll = new String();
		System.out.print("in quit method\n");
		myGame.setclientClose(id);
		messageToAll = ("[client_quit, " + id + "]");
		myGame.setHasMessageToAll(true);
	}

	// Need to reset original message
	public String sendToAll() {
		return messageToAll;

	}
	
	////////////////////////////////////////////////////
	// Was private.  Made public so parser can kick if invalid message.  this should
	// all be moved into one class.  These functions appear multiple times.  Refactor code!
	// 
	///////////////////////////////////////////////////
	
	//sends kicked message, sets flags so that after message is sent client connection will be closed. 
	public void kicked(int id){
		myGame.setKicked(id);
		myGame.setReadyToKick(true);
		myGame.setHasGone(id, true); //This counts as thier turn. 
		//myGame.setPlayerStatus(id, Game.PlayerStatus.REMOVE); // NO.  Do after message is sent. 
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

	// Decrements attempt count. If to many invalid moves
	// This function will the kicked method.
	public void invalid(int id) {
		int attempts = myGame.getPlayerAttemptCount(id);
		System.out.print(attempts);
		attempts = attempts - 1;
		System.out.print(attempts);
		if (attempts == 0) {
			kicked(id);
		} else {
			myGame.setPlayerAttemptCount(id, attempts);
			String messageToPlayer = new String();
			messageToPlayer = ("[invalid_move," + attempts + "]");
			myGame.setPlayerMessage(id, messageToPlayer);
			myGame.clientHasMessage(id);
		}
	}

}
