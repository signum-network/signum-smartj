import bt.Contract;

public class Equals extends Contract {

	boolean equalReceived;

	@Override
	public void txReceived() {
		equalReceived = getCurrentTx().getSenderAddress().equals(getCreator());
	}
}
