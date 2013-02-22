/*
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
 * 
 * Selector object is created.  ServerSocketChannell is created, bound to a port and put in non-blocking mode.  
 * ServerSocketChannell is registered with Selector to register the channel.  SelectionKey represents the registered channel. 
 * */

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class NonBlockingServer {
	// for time out info
	// static final int TIMEOUT = 10000;
	// http://www.velocityreviews.com/forums/t134477-nio-with-timeouts-nio.html
	private int port;
	private Game game; //
	private boolean isClientClosed = false;
	private Map<Integer, SocketChannel> allClientChannels = new HashMap<Integer, SocketChannel>();

	public NonBlockingServer(int myPort, Game myGame) {
		port = myPort; // instance variable automatically set to 0.
		this.game = myGame;
	}

	public void clientClose(int id) {
		try {
			allClientChannels.get(id).close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		allClientChannels.remove(id);
		game.removePlayer(id);
	}


	public boolean setClientClose() {
		return isClientClosed = true;
	}

	public void start() throws IOException {

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
				SelectionKey.OP_ACCEPT);

		while (!exit) {
			selector.select();
			Set keys = selector.selectedKeys();

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
						counter++;
					}
				} else {
					SocketChannel client = (SocketChannel) key.channel();
					if (!key.isReadable())
						continue;
					int bytesread = client.read(buffer);
					if (bytesread == -1) {//end of stream, client disconnected
						System.out.print("in Key cancle and close");
						key.cancel();
						client.close();
						// probably need to remove the client from the list.
						continue;
					}
					buffer.flip();
					String request = decoder.decode(buffer).toString();
					game.setMessage(((Integer) key.attachment()).intValue(),
							request);

					buffer.clear();
					if (request.trim().equals("quit")) {
						client.write(encoder.encode(CharBuffer.wrap("Bye.")));
						// key.cancel();
						// client.close();
						// didn't get a quit message.
					} else {
						int num = ((Integer) key.attachment()).intValue();
						String response = new String();

						int temp = game.getPlayerNumWithMessage();

						if (temp != -1) {
							response = game.getPlayerMessage(game.getPlayerNumWithMessage());
							allClientChannels.get(game.getPlayerNumWithMessage()).write(encoder.encode(CharBuffer.wrap(response)));
							game.resetPlayerNumWithMessage();
						}

						if (game.getHasMessageToAll()) {

							for (Integer j : allClientChannels.keySet()) {
								allClientChannels.get(j).write(
										encoder.encode(CharBuffer.wrap(game
												.getMessageAllPlayers())));
							}
							// Reset for new message.
							game.setHasMessageToAll(false);
						}
					}
				}
			}
		}
	}
}