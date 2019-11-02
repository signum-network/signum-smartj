package bt.sample;

import bt.compiler.CompilerVersion;
import bt.Contract;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A contract that simply send a "Hello, World" message back.
 * Contracts creation fee: 4 Burst.
 * Contracts execution fee: 18.3 Burst. 
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
		compile();
	}
}
