package bt.sample;

import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * A contract that sends back half the signa and token received (if some token is actually received).
 * 
 * @author jjos
 *
 */
public class TokenPlusSignaEchoHalf extends Contract {
	
	long quantity;
	long assetId;
	
	@Override
	public void txReceived() {
		sendAmount(getCurrentTxAmount()/2, getCurrentTxSender());
		
		assetId = getCurrentTx().getAssetIds().getValue1();
		if(assetId != 0) {
			quantity = getCurrentTx().getAmount(assetId);		
			sendAmount(assetId, quantity/2, getCurrentTxSender());
		}

		assetId = getCurrentTx().getAssetIds().getValue2();
		if(assetId != 0) {
			quantity = getCurrentTx().getAmount(assetId);		
			sendAmount(assetId, quantity/2, getCurrentTxSender());
		}

		assetId = getCurrentTx().getAssetIds().getValue3();
		if(assetId != 0) {
			quantity = getCurrentTx().getAmount(assetId);		
			sendAmount(assetId, quantity/2, getCurrentTxSender());
		}

		assetId = getCurrentTx().getAssetIds().getValue4();
		if(assetId != 0) {
			quantity = getCurrentTx().getAmount(assetId);		
			sendAmount(assetId, quantity/2, getCurrentTxSender());
		}
	}
	
	public static void main(String[] args) {
		BT.activateSIP37(true);
		new EmulatorWindow(TokenPlusSignaEchoHalf.class);
	}
	
}
