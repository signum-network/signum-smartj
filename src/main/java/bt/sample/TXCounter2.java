package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A smart contract that simply counts the number of received transactions
 * and processed blocks (with transactions).
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class TXCounter2 extends Contract {

	long ntx, nblocks;
	Address address;
	
	/**
	 * Any new transaction received will be handled by this function.
	 */
	@Override
	public void txReceived(){
		ntx++;
		address = getCurrentTx().getSenderAddress();
	}

	@Override
	protected void blockFinished() {
		nblocks++;
		// causes the execution to be halted
		sendBalance(getCreator());
	}

	public static void main(String[] args) {
		new EmulatorWindow(TXCounter2.class);
	}
}


