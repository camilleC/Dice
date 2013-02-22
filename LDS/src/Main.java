
	/**
	 * Program: Liars Dice Server
	 * Author:  Camille Ciancanelli
	 * Purpose: Implements the game "Liars Dice" via a non-blocking server
	 *          using the NIO library for networking. 
	 *          
	 *           API - Valid Message Conventions from server to client and 
	 *           client to server are outlined in the file "readme" 
	 */

public class Main {

	public static void main(String[] args) {
		Game newGame = new Game(args);	
		newGame.serverStart();
	}

}
