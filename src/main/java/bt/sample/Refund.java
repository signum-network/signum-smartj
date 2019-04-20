package bt.sample;

import bt.Contract;

/**
 * A contract that simply refunds any funds received (minus activation fee).
 * 
 * @author jjos
 */
public class Refund extends Contract {

	@Override
	public void txReceived() {
		sendAmount(getCurrentTx().getAmount(),
				getCurrentTx().getSenderAddress());
	}
}
