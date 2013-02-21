
public abstract class Message {
	
	private int clientID;

	public Message(int clientID)
	{
	 this.clientID = clientID;
	}

	public int getClientID() {
		return clientID;
	}

}
