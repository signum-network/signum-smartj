package bt.sample;

import bt.Contract;

/**
 * Sends back you 1 token for every SIGNA received.
 * 
 * First this contract issues a new token, this is the token that
 * is sent back.
 * 
 * @author jjos
 *
 */
public class AssetFactory extends Contract {
	
	long namePart1;
	long namePart2;
	long decimalPlaces;
	long factor;
	
	long tokenId;
	long amount;
	
	public AssetFactory() {
		// constructor, runs when the first TX arrives
		tokenId = issueAsset(namePart1, namePart2, decimalPlaces);
	}

	@Override
	public void txReceived() {
		amount = (getCurrentTxAmount() + getActivationFee()) / factor;
		
		mintAsset(tokenId, amount);
		// sends back tokens 1-1
		sendAmount(tokenId, amount, getCurrentTxSender());
	}
	
}
