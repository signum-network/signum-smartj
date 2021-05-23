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
public class SimpleNFTContractTest extends SimpleNFTContract {
	
	public static void main(String[] args) {
		BT.activateCIP20(true);
		new EmulatorWindow(SimpleNFTContractTest.class);
	}

	@Override
	public void txReceived() {	
			
	}
}


