package bt.contracts;

import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

public class Send2Messages extends Contract {

	long result;

	@Override
	public void txReceived() {
		sendMessage("first part", getCurrentTxSender());
		sendMessage("second part", getCurrentTxSender());
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);
		new EmulatorWindow(Send2Messages.class);
	}
}
