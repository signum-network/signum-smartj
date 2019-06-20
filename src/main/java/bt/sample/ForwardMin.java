package bt.sample;

import bt.Address;
import bt.compiler.CompilerVersion;
import bt.Contract;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A smart contract that forwards all funds received to another account
 * every time the balance reaches 1000 BURST.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class ForwardMin extends Contract {
	
	Address bmf;
	public static final long MIN_AMOUNT = 1000*Contract.ONE_BURST;
	public static final String ADDRESS = "BURST-TSLQ-QLR9-6HRD-HCY22";

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public ForwardMin(){
		bmf = parseAddress(ADDRESS);
	}
	
	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		if(getCurrentBalance() > MIN_AMOUNT)
			sendAmount(MIN_AMOUNT, bmf);
	}

	public static void main(String[] args) {
		new EmulatorWindow(ForwardMin.class);
		compile();
	}
}


