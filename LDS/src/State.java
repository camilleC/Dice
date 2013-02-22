/**
 * Author:  Camille Ciancanelli
 *
 * Interface so that the "State Pattern" may be implemented. 
 * 
 * */

import java.util.Map;


//TODO handle case where player joins but is all ready joined
//TODO handle case where player joins game but the state is not in lobby;
public interface State {
	
	public int join(int id, String[] request);
	public void quit(int id);
	public int bid(Map<Integer, Player> players, int id, String[] request);
	public int challenge(Map<Integer, Player> players, int id, String[] request);
	public String sendToClient(Map<Integer, Player> players, int id);
	public String sendToAll();
	public void changeState(Game.GameState newState);
}
