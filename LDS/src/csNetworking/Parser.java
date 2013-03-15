package csNetworking;
/**
 * Author:  Camille Ciancanelli
 * Static so I don't need to make objects and function is stateless. Will return null if it didn't find a correct message.
 * NOTE: I used Regular Expression to practice using them. 
 * TODO check for null in every method that calls this.
 * */

import java.util.regex.Pattern;



//need to handle case where the message is not correct. 
public class Parser {

	/**
	 * Static so I don't need to make objects and function is stateless. 
	 * will return null if it didn't find a correct message.
	 * @param myMessage
	 * @return
	 */
	//parse the message and determine what action to take. If message is mallformed
	//throw it out.  Does not count toward a players turn. 
	public static Game.PlayerAct parse(int id, String[] myMessage) {
		String nameRex = "[a-zA-Z1-9]+"; //valid name just added numbers here to handle the bott
		String bidCountRex = "[1-5]"; //valid bid count
		String bidFaceRex = "[1-6]";  //valid face values
		Game.PlayerAct message = Game.PlayerAct.DEFAULT;
		String errorMessage = null;
		Boolean hasError = false;
		
		try{
		if (myMessage[1].equals("join")){ 
			if (myMessage[2].matches(nameRex) && (myMessage[2].length() <= 10)) {
				message = Game.PlayerAct.JOIN;
			} else {
				errorMessage = "name malformed";
				hasError = true;
				
			}
		} else if (myMessage[1].equals("quit")){  
				if (myMessage[1].length() == 4){
			      message = Game.PlayerAct.QUIT;
				}
		else {
				errorMessage = "quit malformedn";
				hasError = true;
			}
		} else if (myMessage[1].equals("bid")) {
			if (myMessage[2].matches(bidCountRex) && (myMessage[3].matches(bidFaceRex))) {
				message = Game.PlayerAct.BID;
				System.err.println("BID MESSAGE = " + myMessage[1].toString());
			} else {
				errorMessage = "bid malformed";
				hasError = true;
			}
		} else if (myMessage[1].equals("challenge")) {
			System.err.println("challenge should not be error = |" +  myMessage[1] + "|\n");
			if (myMessage.length == 2) {
				message = Game.PlayerAct.CHALLENGE;
			} else {
				errorMessage = "challenge malformed";
				hasError = true;
			}
		}
		if (hasError){
			System.err.println("ERROR: Client ID " + id + " " + errorMessage);
			System.err.println("ERROR = |" +  myMessage[1] + "|\n");
			hasError = false;
			errorMessage = new String();
		}
		}
		catch (ArrayIndexOutOfBoundsException e){
		System.err.println("Error: Array handeling exception");
		}
		
		
		return message;
	}
}

