package bt.sample;

import bt.Contract;

/**
 * A single public method that can be used to check and store the quantity
 * received of a given asset id.
 * 
 * @author jjos
 *
 */
public class AssetQuantityReceived extends Contract {
	
	long quantity;
	long assetId;
	
	public void checkAssetQuantity(long assetIdTocheck) {
		assetId = assetIdTocheck;
		quantity = getCurrentTx().getAmount(assetId);
	}
	
	@Override
	public void txReceived() {
	}
	
}
