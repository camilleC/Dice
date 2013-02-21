
import java.util.ArrayList;


public class QuitMessage extends Message {
	private String name;
	
	public QuitMessage(int clientId, String name){
		super(clientId);
		this.setName(name);
	}

	private void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}