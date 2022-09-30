package bt.contracts;

import bt.BT;
import bt.Contract;
import bt.Register;
import bt.ui.EmulatorWindow;

public class MessagePage extends Contract {

	Register page1;
	Register page2;

	@Override
	public void txReceived() {
		page1 = getCurrentTx().getMessage(0);
		page2 = getCurrentTx().getMessage(1);
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);
		new EmulatorWindow(MessagePage.class);
	}
}
