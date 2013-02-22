
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
				StringBuilder sb = new StringBuilder();
				String messageToPlayer = new String();
				myGame.setPlayerName(id, new String(request[2]));			
				sb.append("[state, in_game, ").append(id).append(", ").append(myGame.getPlayerCount());
	            for (int i = 0; i < myGame.getPlayerCount(); i++){
	            
	            	if(myGame.isPlayerValid(i)){
					sb.append(" , ").append(myGame.getPlayerName(i)).append(", ").append(i);
	            	}
				} 
				sb.append("]\n");
				messageToPlayer = sb.toString();
				myGame.setPlayerMessage(id, messageToPlayer);
				myGame.clientHasMessage(id);
				myGame.setWaitingStatus(id, true);// player has joined but can't play this round.  
				myGame.setHasMessageToAll(true);
				messageToAll = "[client_joined, " +  myGame.getPlayerName(id) + " , " + id + "]\n";
				return 0;
				}
		
		//if this closes correctly have it send a message first. 
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
			//myGame.setState(Game.GameState.LOBBY);
		}
		
		//===============================================
		// Helper methods specific to this class
		//===============================================
	
		public void kicked(int id){
			myGame.setclientClose(id);
			messageToAll = ("[client_kicked, " + id + "]");
			myGame.setHasMessageToAll(true);
		};	
		
		public void inValidMove(int id){
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