package bt.sample;

import bt.Address;
import bt.Contract;
import bt.EmulatorWindow;

/**
 * A smart contract that simply forwards all funds received to another account.
 * 
 * @author jjos
 */
public class Forward extends Contract {
	
	Address bmf;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public Forward(){
		bmf = parseAddress("BURST-TSLQ-QLR9-6HRD-HCY22");
	}
	
	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		sendAmount(getCurrentTx().getAmount(), bmf);
	}

	public static void main(String[] args) {
		new EmulatorWindow(Forward.class);
	}
}


