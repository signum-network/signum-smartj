package bt.contracts;

import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

public class CheckAccountBalance extends Contract {

	long balance;
	
	public void retrieveBalance(long assetId, long accountId) {
		balance = getAccountBalance(assetId, accountId);
	}

	@Override
	public void txReceived() {
		// do nothing
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);
		
		new EmulatorWindow(CheckAccountBalance.class);
	}
}
