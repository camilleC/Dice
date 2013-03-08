package csNetworking;
/**
 * Author:  Camille Ciancanelli
 * TODO should I get rid of this class and put its functionality into the game? 
 * 
 * */



//need to handle case where the message is not correct. 
public class Parser {

	/**
	 * Static so I don't need to make objects and function is stateless. 
	 * will return null if it didn't find a correct message.
	 * @param myMessage
	 * @return
	 */
	//parse the message and determine what action to take. 
	public static Game.PlayerAct parse(int clientId, String[] myMessage){
	Game.PlayerAct message; 
	if (myMessage[1].equals("join")) {message = Game.PlayerAct.JOIN;}
	else if (myMessage[1].equals("quit")){message = Game.PlayerAct.QUIT;} 
	else if (myMessage[1].equals("bid")){message = Game.PlayerAct.BID;}
	else if (myMessage[1].equals("challenge")){message = Game.PlayerAct.CHALLENGE;} 
	else message = Game.PlayerAct.DEFAULT;	
    return message;
	}
}

