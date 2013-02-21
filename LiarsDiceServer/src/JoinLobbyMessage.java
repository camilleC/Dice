

public class JoinLobbyMessage extends Message {
    private String name;
	public JoinLobbyMessage(int clientId, String name){
		super(clientId);
		this.setName(name);
		
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
}
