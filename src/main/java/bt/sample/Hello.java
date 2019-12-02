package bt.sample;

import bt.compiler.CompilerVersion;
import bt.Contract;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A contract that simply send a "Hello, World" message back.
 * 
 * Contract publishing fee currently is 4 BURST for this contract and its
 * execution fee is 15.1 BURST (activation fee should be this later value or
 * more).
 * 
 * @author jjos
 *
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class Hello extends Contract {

	/**
	 * Every time a transaction is sent to the contract, this method is called
	 */
	@Override
	public void txReceived() {
		sendMessage("Hello, World", getCurrentTxSender());
	}

	/**
	 * A main function for debugging or publishing purposes only, this method is not
	 * compiled into bytecode.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new EmulatorWindow(Hello.class);
	}
}
