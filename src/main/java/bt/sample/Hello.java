package bt.sample;

import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * A contract that simply send a "Hello, World" message back.
 * 
 * @author jjos
 *
 */
public class Hello extends Contract {

	/**
	 * Every time a transaction is sent to the contract, this method is called
	 */
	@Override
	public void txReceived() {
		sendMessage("Hello, World", getCurrentTx().getSenderAddress());
	}

	/**
	 * A main function for debugging purposes only, this method is not
	 * compiled into bytecode.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new EmulatorWindow(Hello.class);
	}
}
