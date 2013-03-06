package csNetworking;
/**
 * Author:  Camille Ciancanelli
 * 
 * */

import java.util.*;


public class StateTimerLobby implements State{
	private Game myGame;
	private boolean timerOn = false;
	private String messageToAll;

	//this constructs a lobby for a specified game
	public StateTimerLobby(Game myGame){
		this.myGame = myGame;
	}
	//TODO Send to many clients
	public int join(int id, String[] request){
		    messageToAll = new String();//ADDED MON NIGHT
		    StringBuilder sb = new StringBuilder();
			//String tempMessage = new String();
			String messageToPlayer = new String();
			myGame.setPlayerName(id, new String(request[2]));
			myGame.setPlayerStatus(id, Game.PlayerStatus.PLAYING);
			sb.append("[state, timer_with_lobby, ").append(id).append(", ").append(myGame.getPlayerCount());
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

	public void invalidMove(int id){
		int attempts;
		attempts = myGame.getPlayerAttemptCount(id);
		attempts = attempts -1;
		
		if (attempts == 0){
			kicked(id);
		}
	}
	
	
		public void kicked(int id){
			myGame.setclientClose(id);
			messageToAll = ("[client_kicked, " + id + "]");
			myGame.setPlayerStatus(id, Game.PlayerStatus.REMOVE);
			if (id == myGame.getPlayerTurn()){ //if the current player gets kicked signal the next player that it is thier turn. 
				 myGame.setNextPlayerTurn(); 
			     myGame.setHasMessageToAll(true);
			}
		}
	public void quit(int id){
		System.out.print("in quit method\n");
		myGame.setclientClose(id);
		messageToAll = ("[client_quit, " + id + "]");
		myGame.setHasMessageToAll(true);
	};
	
	
	//players will recieve invalid move message if they bid or challenge while in the lobby	
	public int bid(int id, String[] request){
		invalidMove(id);
		return 0;}
	

	public int challenge(Map<Integer, Player> players, int id, String[] request){invalidMove(id); return 0;}
	public String sendToClient(Map<Integer, Player> players, int id){return "not implimented";}

	//Need to reset original message
	public String sendToAll(){
	       return messageToAll;		

	}
	public void changeState(Game.GameState newState){}
}
	
