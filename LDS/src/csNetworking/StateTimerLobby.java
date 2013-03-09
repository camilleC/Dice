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
			invalidMove(id);
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
		return 0;
	}


	// players will recieve invalid move message if they bid or challenge while
	// in the lobby
	public int bid(int id, String[] request) {
		invalidMove(id);
		return 0;
	}

	public void challenge(int id) {invalidMove(id);}

	public void quit(int id){
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
	// Private methods
	///////////////////////////////////////////////////
	
	private void kicked(int id) {
		myGame.setKicked(id);
		myGame.setReadyToKick(true);
		messageToAll = ("[client_kicked, " + id + "]");
		// if the current player gets kicked signal the next player that it is
		// thier turn.
		if (id == myGame.getPlayerTurn()) {
			myGame.setNextPlayerTurn();
			messageToAll = "[client_kicked, " + id + "]" + "[player_turn, "
					+ myGame.getPlayerTurn() + "]";
		}
		myGame.setHasMessageToAll(true);
	}

	// Decrements attempt count. If to many invalid moves
	// This function will the kicked method.
	private void invalidMove(int id) {
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
