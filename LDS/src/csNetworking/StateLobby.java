package csNetworking;
/**
 * Author:  Camille Ciancanelli
 * 
 * */

import java.util.*;




public class StateLobby implements State{
	private Game myGame;
	private boolean timerOn = false;
	private enum GameState{DEFAULT, INGAME, LOBBY, TIMERLOBBY};
	private String messageToAll;

	//this constructs a lobby for a specified game
	public StateLobby(Game myGame){
		this.myGame = myGame;
	}
	//TODO Send to many clients
	public int join(int id, String[] request){
			StringBuilder sb = new StringBuilder();
			String messageToPlayer = new String();
			myGame.setPlayerName(id, new String(request[2]));
			myGame.setWaitingStatus(id, Game.PlayerStatus.PLAYING);
			sb.append("[state, in_lobby, ").append(id).append(", ").append(myGame.getPlayerCount());
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

	
	public void quit(int id){
		System.out.print("in quit method\n");
		myGame.setclientClose(id);
		messageToAll = ("[client_quit, " + id + "]");
		myGame.setHasMessageToAll(true);
	};
	
	
	
	public int bid(int id, String[] request){return 0;}
	public int challenge(Map<Integer, Player> players, int id, String[] request){return 0;}

	public String sendToClient(Map<Integer, Player> players, int id){return "not implimented";}
	public String sendToAll(){return messageToAll;}
	public void changeState(Game.GameState newState){
		myGame.setState(newState);
	}
}
	
	

	

