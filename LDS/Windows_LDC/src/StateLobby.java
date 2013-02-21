
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
			//String tempMessage = new String();
			String messageToPlayer = new String();
			myGame.setPlayerName(id, new String(request[2]));
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
			myGame.setWaitingStatus(id, false);// player has joined and doesn't have to wait.
			myGame.setHasMessageToAll(true);
			messageToAll = "[client_joined, " +  myGame.getPlayerName(id) + " , " + id + "]";
	        //@TODO player count isn't the same as the number of players joined. Fix if time. 
			System.err.print("in lobby   " + id );

			//if minimum number of players are reached change state and set the timer. 
			if (myGame.getPlayerCount() == 3){
                changeState(Game.GameState.TIMERLOBBY);
                
			}
			return 0;
			}

	
	public void quit(int id){
		System.out.print("in quit method\n");
		myGame.setclientClose(id);
		messageToAll = ("[client_quit, " + id + "]");
		myGame.setHasMessageToAll(true);
	};
	
	
	
	public int bid(Map<Integer, Player> players, int id, String[] request){return 0;}
	public int challenge(Map<Integer, Player> players, int id, String[] request){return 0;}

	public String sendToClient(Map<Integer, Player> players, int id){return "not implimented";}
	public String sendToAll(){return messageToAll;}
	public void changeState(Game.GameState newState){
		myGame.setState(newState);
	}
}
	
	

	

