package bt.contracts;

import bt.Contract;
import bt.ui.EmulatorWindow;

public class DistributeToHolders extends Contract {

	long tokenId;

	public void distributeToken(long tokenToDistribute) {
		// distributes SIGNA+token to holders
		distributeToHolders(0, tokenId, getCurrentTxAmount(), tokenToDistribute, getCurrentBalance(tokenToDistribute));		
	}
	
	@Override
	public void txReceived() {
		// distributes SIGNA to holders
		distributeToHolders(0, tokenId, getCurrentTxAmount(), 0, 0);
	}

	public static void main(String[] args) {
		new EmulatorWindow(DistributeToHolders.class);
	}
}
