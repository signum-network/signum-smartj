package bt.sample;

import bt.Contract;

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
		assetId = getCurrentTx().getAssetIds().getValue1();
		quantity = getCurrentTx().getAmount(assetId);
		
		sendAmount(getCurrentTxAmount()/2, assetId, quantity/2, getCurrentTxSender());
	}
	
}
