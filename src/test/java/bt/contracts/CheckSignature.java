package bt.contracts;

import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * Checks if the signature of the creator can be verified for the given message.
 * 
 * @author jjos
 *
 */
public class CheckSignature extends Contract {

	long checked;

	public void checkSignature(long msg2, long msg3, long msg4) {
		checked = checkSignature(msg2, msg3, msg4, getCurrentTx(), 1, getCreator().getId());
	}
	
	@Override
	public void txReceived() {
		// not used
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);
		new EmulatorWindow(CheckSignature.class);
	}
}
