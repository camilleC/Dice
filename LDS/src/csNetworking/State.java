package csNetworking;
/**
 * Author:  Camille Ciancanelli
 *
 * Interface so that the "State Pattern" may be implemented. 
 * 
 *  
 * TODO Ask Dr. Reedy.  I've decided not to use my "change state" function here b/c I think 
 * the game should be responsible for changing state. Wont this decrease the coupling? 
 * 
 * */

public interface State {
	
	public int join(int id, String[] request);
	public void quit(int id);
	public int bid(int id, String[] request);
	public void challenge(int id);
	public String sendToAll();

}
