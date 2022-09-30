package bt.contracts;

import bt.Contract;
import bt.ui.EmulatorWindow;

public class EqualsCreator extends Contract {

	boolean equalReceived;

	@Override
	public void txReceived() {
		equalReceived = getCurrentTx().getSenderAddress().equals(getCreator());
	}

	public static void main(String[] args) {
		new EmulatorWindow(EqualsCreator.class);
	}
}
