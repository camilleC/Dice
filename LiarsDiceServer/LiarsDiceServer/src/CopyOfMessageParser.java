
import java.util.ArrayList;


public class CopyOfMessageParser {
/*
	*//**
	 * Static so I don't need to make objects and function is stateless. 
	 * will return null if it didn't find a correct message.
	 * @param myMessage
	 * @return
	 *//*
	public static Message parse(int clientId, String[] myMessage){
	

	if (myMessage[1] =="join"){
			
			System.err.print(myString);
		}//new JoinLobbyMessage(clientId, myMessage[1]);
		
	    if (myMessage[0] =="challenge") return new ChallengeMessage(clientId);
		    (myMessage[0] =="quit") return new QuitMessage(clientId, myMessage[1]);
		if (myMessage[0] =="bid") {
			ArrayList<Integer> thebids = new ArrayList<Integer>();
			diceCnt = new Integer(myMessage[1]);
			for (i = 0; i < diceCnt; i++){
				thebids.add(new Integer (myMessage[i+2]));
			}
			return new BidMessage(clientId, thebids);
		}*/			
		//else
	//	return null;
	//}
}
