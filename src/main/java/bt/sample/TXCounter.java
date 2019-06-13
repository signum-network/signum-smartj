package bt.sample;

import bt.*;
import bt.ui.EmulatorWindow;

/**
 * A smart contract that simply counts the number of received transactions
 * and processed blocks (with transactions).
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class TXCounter extends Contract {

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
	}

	public static void main(String[] args) {
		new EmulatorWindow(TXCounter.class);
	}
}


