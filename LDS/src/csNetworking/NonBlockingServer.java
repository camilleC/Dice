package csNetworking;
/**
 * Author:  Camille Ciancanelli
 * This class communicates with the client.
 * Server takes a game object so that it can call Game object functions. 
 * 
 * Server Code based on "Java in A Nutshell" p. 272.
 * java.io and nava.net allow only blocking I/O so a thread is needed for each client.
 * 
 * https://blogs.oracle.com/slc/entry/javanio_vs_javaio
 * http://tutorials.jenkov.com/java-nio/selectors.html
 * 
 * 
 * NOTES:
 * NIO uses a single thread to manage pending connections (via select) and uses channels in a non-blocking way.  
 * selector object:      Keeps track of a set of registered channels. Can be blocking. 
 * ServerSocketChannel:  Connection to hardware device (i.e. network socket). Can handle reading and writing.  
 * 
 * Selector object is created.  ServerSocketChannell is created, bound to a port and put in non-blocking mode.  
 * ServerSocketChannell is registered with Selector to register the channel.  SelectionKey represents the registered channel. 
 * 
 * 	// for time out info
	// static final int TIMEOUT = 10000;
	// http://www.velocityreviews.com/forums/t134477-nio-with-timeouts-nio.html
 * */

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import csNetworking.Game.PlayerStatus;



public class NonBlockingServer {
    //private boolean startGame = true;
	private int port;
	private Game game; 
	private Map<Integer, SocketChannel> allClientChannels = new HashMap<Integer, SocketChannel>();
	private Map<SocketChannel,ByteBuffer> clientBuffers = new HashMap<SocketChannel, ByteBuffer>();
    //private ByteBuffer readBuffer;
    private int bytesread = 0;
	public NonBlockingServer(int myPort, Game myGame) {
		port = myPort; // instance variable automatically set to 0.
		this.game = myGame;
	}
	
	public void startServer() throws IOException {

		int MSG_MAX = 600;
		boolean exit = false;
		int counter = 0;

		Charset charset = Charset.forName("ISO-8859-1");
		CharsetEncoder encoder = charset.newEncoder();
		CharsetDecoder decoder = charset.newDecoder();

		ByteBuffer buffer = ByteBuffer.allocate(MSG_MAX);

		Selector selector = Selector.open();

		ServerSocketChannel server = ServerSocketChannel.open();
		server.socket().bind(new java.net.InetSocketAddress(port));
		server.configureBlocking(false);
		SelectionKey serverkey = server.register(selector,
				SelectionKey.OP_ACCEPT); //  SelectionKey.OP_READ this doesn't help

		while (!exit) {

           /**
			 Code in this section will go to clients 
			 even if they are not sending the server a message. 
             */
			
			
			// evaluate the lobby time out.
			// If time up is over round start message will be sent. 
			if (game.timeOutOver()){
                game.roundStartMsg();
					for (Integer j : allClientChannels.keySet()) { 
						if (game.isPlayerValid(j)){
							allClientChannels.get(j).write(ByteBuffer.wrap(game.getAllPlayerMessageFromGameLogic().getBytes()));

						}
					}
					game.reSetSendRoundMessage(); //this will set round message to false so that I don't keep getting it. 
					game.setHasMessageToAllFromGameLogic(false);
					
					//send dice message
					if (game.getDiceMsg()) {

						for (Integer j : allClientChannels.keySet()) {
							//checks to make sure clients who have connected but have not Joined do not receive messages. 
							if (game.isPlayerInRound(j)){
							allClientChannels.get(j).write(ByteBuffer.wrap(game.getPlayerDiceMessageFromState(j).getBytes()));
							}
						}
						// Reset for new message.
						game.setDiceMsg();
					}
					
			}
			
			
			
			 selector.select(2L); // milli seconds
			 Set keys = selector.selectedKeys();             
	        
	           /**
				Reactive code.  Is only triggered if a client sends a message (including trying to connect) 
	           */
		    
		       for (Iterator i = keys.iterator(); i.hasNext();) {
				SelectionKey key = (SelectionKey) i.next();
				i.remove();
				if (key == serverkey) {
					if (key.isAcceptable()) {
		              
						SocketChannel client = server.accept();
						allClientChannels.put(new Integer(counter), client);
						client.configureBlocking(false);
						SelectionKey clientkey = client.register(selector,
								SelectionKey.OP_READ);

						clientkey.attach(new Integer(counter));
					
						//Client added but not "joined" yet. 
						game.addPlayer(counter);
						clientBuffers.put(client, ByteBuffer.allocate(600));
						counter++;
						System.out.print(counter);
					}
				} else {
					SocketChannel client = (SocketChannel) key.channel();
					if (!key.isReadable())
						continue;
					try{
					ByteBuffer readBuffer = clientBuffers.get(client);
					int bytesread = client.read(readBuffer);
					

		
					if (bytesread == -1) {//end of stream, client disconnected
						System.err.print("in Key cancle and close");
						key.cancel();
						client.close();
						String messageOut = new String();
						//This will give me the id of the disconnecting client. 
						Integer closeId = ((Integer)key.attachment()).intValue();
						game.setPlayerStatus(closeId, Game.PlayerStatus.REMOVE);
						
						//did getting the next player cause a winner? 
					    //If players turn
						if (closeId == game.getPlayerTurn()) {
							if (game.isWinner() == -1) {
						    	game.setNextPlayerTurn();
						    	messageOut = ("[client_quit, " + closeId + "][player_turn, " + game.getPlayerTurn() + "]"); 
							}
							//someone quit and now there is a winner.
						    //the call to isWinner above will build a message. 
						}
						else { //if not players turn, see if there is a winner.  
							game.isWinner();	
							messageOut = ("[client_quit, " + closeId + "]");
							}
						
						for (Integer j : allClientChannels.keySet()) {
							if (game.isPlayerInRound(j)){
							allClientChannels.get(j).write(ByteBuffer.wrap(messageOut.getBytes()));
							}
						}
						
						continue;
					
					}
					//buffer.flip();
					
					
					String request = new String(readBuffer.array());
					System.out.println("From Client:" + request);
					clientBuffers.put(client, ByteBuffer.allocate(600));
					readBuffer.clear();
					
					game.setMessage(((Integer) key.attachment()).intValue(),request.trim());
					//System.out.print(request);
					readBuffer.clear();
					
					if (request.trim().equals("quit")) {
						client.write(encoder.encode(CharBuffer.wrap("Bye.")));
						key.cancel();
						client.close();
					} else {
					
						String response = new String();
                        
						int temp = game.getPlayerNumWithMessage();

						if (temp != -1) {
							response = game.getPlayerMessage(game.getPlayerNumWithMessage());
							//allClientChannels.get(game.getPlayerNumWithMessage()).write(encoder.encode(CharBuffer.wrap(response)));
							allClientChannels.get(game.getPlayerNumWithMessage()).write(ByteBuffer.wrap(response.getBytes()));
							game.resetPlayerNumWithMessage();
						}

						
						
/*						for (Integer t : allClientChannels.keySet()) { 
							if (game.isPlayerValid(t)){
								if (game.playerMessagCollectionTimedOut(t)){
									game.kickClinetTimedOut(t);
									}
								}
						}*/
						
						
						
                        //Message created by a player in a state needs to get sent to all     						
						if (game.getHasMessageToAll()) {

							for (Integer j : allClientChannels.keySet()) {
								//checks to make sure clients who have connected but have not Joined do not receive messages. 
								if (game.isPlayerValid(j)){
									System.err.print("created in state"+ game.getAllPlayerMessageFromState() + "\n");
								allClientChannels.get(j).write(ByteBuffer.wrap(game.getAllPlayerMessageFromState().getBytes()));
								}
							}
							
							//if a client is kicked they need the kicked message, they can't be closed till here.
							//the for loop above provides the message. 
							if (game.getReadyToKick()){
								clientClose(game.getKicked());
								key.cancel();
								game.setPlayerStatus(game.getKicked(), Game.PlayerStatus.REMOVE);
								game.setReadyToKick(false);
							}
							
							// Reset for new message.

							game.setHasMessageToAll(false);
						}
						
						//Message created by the game logic needs to get sent to all
						if (game.getHasMessageToAllFromGameLogic()) {
                            System.err.print("Should I bee here? \n");
							for (Integer j : allClientChannels.keySet()) {
								//checks to make sure clients who have connected but have not Joined do not receive messages. 
								if (game.isPlayerValid(j)){
									System.err.print("created in logic" + game.getAllPlayerMessageFromGameLogic() + "\n");
									//allClientChannels.get(j).write(encoder.encode(CharBuffer.wrap(game.getAllPlayerMessageFromGameLogic())));
									allClientChannels.get(j).write(ByteBuffer.wrap(game.getAllPlayerMessageFromGameLogic().getBytes()));
								}
							}
							
			
							// Reset for new message.
							game.setHasMessageToAllFromGameLogic(false);
						}
						
						if (game.getDiceMsg()) {

							for (Integer j : allClientChannels.keySet()) {
								//checks to make sure clients who have connected but have not Joined do not receive messages. 
								if (game.isPlayerInRound(j)){
								allClientChannels.get(j).write(ByteBuffer.wrap(game.getPlayerDiceMessageFromState(j).getBytes()));
								}
							}
							// Reset for new message.
							game.setDiceMsg();
						}
						
						
					}
				}
					catch (IOException e){
					      System.err.print("Cancelling Key - No Attachment");
						}
			}

		       }
			}
	}
		

	
	//-------------------------------------
	//    Helper functions for Server
	//-------------------------------------
	
    /* Closes connection to client 
	 * Removes client from player queue
	 */
	public void clientClose(int id) {
		try {
			allClientChannels.get(id).close();

			System.out.print("in key cancle");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		allClientChannels.remove(id);
	}
}