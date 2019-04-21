package bt.sample;

import bt.Address;
import bt.Contract;
import bt.EmulatorWindow;

/**
 * A smart contract that forwards all funds received to another account
 * every time the balance reaches 1000 BURST.
 * 
 * @author jjos
 */
public class Forward1000 extends Contract {
	
	Address bmf;
	long minAmount;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public Forward1000(){
		bmf = parseAddress("BURST-TSLQ-QLR9-6HRD-HCY22");
		minAmount = 1000*Contract.ONE_BURST;
	}
	
	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		if(getCurrentBalance() > minAmount)
			sendBalance(bmf);
	}

	public static void main(String[] args) {
		new EmulatorWindow(Forward1000.class);
	}
}


