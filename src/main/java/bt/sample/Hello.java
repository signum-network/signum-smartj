package bt.sample;

import bt.Contract;

/**
 * A contract that simply send a "Hello!" message back.
 * 
 * @author jjos
 *
 */
public class Hello extends Contract {

	@Override
	public void txReceived() {
		sendMessage("Hello!", getCurrentTx().getSenderAddress());
	}
}
