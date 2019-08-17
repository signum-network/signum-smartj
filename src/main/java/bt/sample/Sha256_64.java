package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A smart contract that simply echoes the message received.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class Sha256_64 extends Contract {

    long sha256_64;
	
	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		sha256_64 = this.performSHA256_64(1, 2);
	}

	public static void main(String[] args) {
		new EmulatorWindow(Sha256_64.class);
	}
}


