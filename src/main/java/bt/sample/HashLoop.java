package bt.sample;

import bt.compiler.CompilerVersion;
import bt.Contract;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A contract that run hashes until there is no more balance.
 * 
 * @author jjos
 *
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class HashLoop extends Contract {
	long hash;

	/**
	 * Every time a transaction is sent to the contract, this method is called
	 */
	@Override
	public void txReceived() {
		while(true){
			// run this forever
			// execution will freeze when there is no more balance and can be resumed
			// by charging the contract with more BURST
			hash = performSHA256_64(getPrevBlockHash1(), hash);
        }
	}

	/**
	 * A main function for debugging purposes only, this method is not
	 * compiled into bytecode.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new EmulatorWindow(HashLoop.class);
	}
}
