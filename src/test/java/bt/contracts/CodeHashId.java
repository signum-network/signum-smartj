package bt.contracts;

import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * Gets the code hash id for the given contract id.
 * 
 * @author jjos
 *
 */
public class CodeHashId extends Contract {

	long thisCodeHashId;
	long codeHashId;

	public void getCodeHash(long contractId) {
		thisCodeHashId = getCodeHashId();
		codeHashId = getCodeHashId(contractId);
	}

	@Override
	public void txReceived() {
		// not used
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);
		new EmulatorWindow(CodeHashId.class);
	}
}
