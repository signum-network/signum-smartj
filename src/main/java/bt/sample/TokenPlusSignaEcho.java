package bt.sample;

import bt.Contract;

/**
 * A single public method that sends you back the token received as well all surplus SIGNA.
 * 
 * @author jjos
 *
 */
public class TokenPlusSignaEcho extends Contract {
	
	long quantity;
	long assetId;
	
	public void echoToken(long tokenToCheck) {
		assetId = tokenToCheck;
		quantity = getCurrentTx().getAmount(assetId);
		
		sendAmount(getCurrentTxAmount(), tokenToCheck, quantity, getCurrentTxSender());
	}
	
	@Override
	public void txReceived() {
	}
	
}
