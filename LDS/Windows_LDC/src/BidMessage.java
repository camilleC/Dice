
import java.util.ArrayList;


public class BidMessage extends Message {
	private ArrayList<Integer> bids = new ArrayList<Integer>();
	
	public BidMessage(int clientId, ArrayList<Integer> bids){
		super(clientId);
		this.setBids(bids);
	}

	private void setBids(ArrayList<Integer> bids) {
		this.bids = bids;
	}

	public ArrayList<Integer> getBids() {
		return bids;
	}
}
