package bt.sample;

import bt.*;
import bt.ui.EmulatorWindow;

/**
 * A smart contract that simply echoes the message received.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class Echo extends Contract {
	
	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		sendMessage(getCurrentTx().getMessage(), getCurrentTx().getSenderAddress());
	}

	public static void main(String[] args) {
		new EmulatorWindow(Echo.class);
		compile();
	}
}


