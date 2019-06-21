package bt.sample;

import bt.Address;
import bt.compiler.CompilerVersion;
import bt.Contract;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A smart contract that simply forwards all funds received to another account.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class Forward extends Contract {
	
	public static final String ADDRESS = "BURST-TSLQ-QLR9-6HRD-HCY22";
	Address bmf;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public Forward(){
		bmf = parseAddress(ADDRESS);
	}
	
	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		sendAmount(getCurrentTxAmount(), bmf);
	}

	public static void main(String[] args) {
		new EmulatorWindow(Forward.class);
		compile();
	}
}


