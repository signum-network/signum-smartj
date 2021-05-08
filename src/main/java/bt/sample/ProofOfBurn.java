package bt.sample;

import bt.Contract;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A smart contract that burns all BURST it receives.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class ProofOfBurn extends Contract {
	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		sendAmount(getCurrentBalance(), getAddress(0L));
	}

	public static void main(String[] args) {
		new EmulatorWindow(ProofOfBurn.class);
	}
}

