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



public class NonBlockingServer {

	private int port;
	private Game game; //
	private Map<Integer, SocketChannel> allClientChannels = new HashMap<Integer, SocketChannel>();

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
				SelectionKey.OP_ACCEPT, SelectionKey.OP_READ);

		while (!exit) {
			//selector.select();  //This will cause it to block
			selector.select((long)game.timeToWait);  // This lets it timeout so that it isn't blocking on a connection
			game.timeOutOver();
			Set keys = selector.selectedKeys(); //****ANDY CHECK THIS IT BLOCKS****  It will wait for any connected client to send a message.  
		    System.err.print("bar\n");          //  I can't send messages out arbitrarily b/c of this....as soon as a client sends me a   
		                                        //  message then it gets past the selector and I can spam ALL my clients with messages but 
		                                        //I need to figure out how to prevent this from blocking.    
		                                        
			for (Iterator i = keys.iterator(); i.hasNext();) {
				SelectionKey key = (SelectionKey) i.next();
				i.remove();
                System.err.print("foo\n");
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
						counter++;
					}
				} else {
					SocketChannel client = (SocketChannel) key.channel();
					if (!key.isReadable())
						continue;
					int bytesread = client.read(buffer);
					if (bytesread == -1) {//end of stream, client disconnected
						System.err.print("in Key cancle and close");
						key.cancel();
						client.close();
						continue;
					}
					buffer.flip();
					String request = decoder.decode(buffer).toString();
					
					game.setMessage(((Integer) key.attachment()).intValue(),request);
					//System.out.print(request);
					buffer.clear();
					
					if (request.trim().equals("quit")) {
						client.write(encoder.encode(CharBuffer.wrap("Bye.")));
						key.cancel();
						client.close();
						// didn't get a quit message.
					} else {
						//int num = ((Integer) key.attachment()).intValue();
						String response = new String();
                        
						int temp = game.getPlayerNumWithMessage();

						if (temp != -1) {
							response = game.getPlayerMessage(game.getPlayerNumWithMessage());
							allClientChannels.get(game.getPlayerNumWithMessage()).write(encoder.encode(CharBuffer.wrap(response)));
							game.resetPlayerNumWithMessage();
						}

                        //Message created by a player in a state needs to get sent to all     						
						if (game.getHasMessageToAll()) {

							for (Integer j : allClientChannels.keySet()) {
								//checks to make sure clients who have connected but have not Joined do not receive messages. 
								if (game.isPlayerValid(j)){
									System.err.print("created in state"+ game.getAllPlayerMessageFromState() + "\n");
								allClientChannels.get(j).write(encoder.encode(CharBuffer.wrap(game.getAllPlayerMessageFromState())));
							   
								}
							}
							// Reset for new message.
							game.setHasMessageToAll(false);
						}
						
						//Message created by the game logic needs to get sent to all
						if (game.getHasMessageToAllFromGameLogic()) {

							for (Integer j : allClientChannels.keySet()) {
								//checks to make sure clients who have connected but have not Joined do not receive messages. 
								if (game.isPlayerValid(j)){
									System.err.print("created in logic" + game.getAllPlayerMessageFromGameLogic() + "\n");
									allClientChannels.get(j).write(encoder.encode(CharBuffer.wrap(game.getAllPlayerMessageFromGameLogic())));
								}
							}
							// Reset for new message.
							game.setHasMessageToAllFromGameLogic(false);
						}
						
						if (game.getDiceMsg()) {

							for (Integer j : allClientChannels.keySet()) {
								//checks to make sure clients who have connected but have not Joined do not receive messages. 
								if (game.isPlayerInRound(j)){
								allClientChannels.get(j).write(encoder.encode(CharBuffer.wrap(game.getPlayerDiceMessageFromState(j))));
								}
							}
							// Reset for new message.
							game.setDiceMsg();
						}
						
						
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		allClientChannels.remove(id);
	}
}