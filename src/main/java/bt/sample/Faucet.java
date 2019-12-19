package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A faucet smart contract sending back 30 BURST for every transaction received.
 * 
 * This will run until there is no more balance. Anyone can recharge this
 * contract by sending more than 30 BURST.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class Faucet extends Contract {

	/**
	 * For every transaction received we send back 30 BURST.
	 */
	public void txReceived() {
		sendAmount(30 * ONE_BURST, getCurrentTxSender());
	}


	/**
	 * Main function for testing or publishing the contract on chain.
	 */
	public static void main(String[] args) {
		new EmulatorWindow(Faucet.class);
	}
}
