package bt.sample;

import bt.Address;
import bt.compiler.CompilerVersion;
import bt.Contract;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A smart contract for accepting tips and return a tank you message.
 * 
 * For every transaction received (amount higher than the activation fee),
 * a "Thank you!" message is returned. Messages from smart contracts
 * currently come only as hexadecimal numbers. To actually read the
 * message contents convert from hexadecimal to string using, for instance,
 * https://codebeautify.org/hex-string-converter.
 * 
 * When the balance reaches a specified mininum amount, the balance
 * is transfered to the beneficiary.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class TipThanks extends Contract {
	
	Address beneficiary;
	public static final long MIN_AMOUNT = 1000*Contract.ONE_BURST;
	public static final String ADDRESS = "BURST-JJQS-MMA4-GHB4-4ZNZU";

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public TipThanks(){
		beneficiary = parseAddress(ADDRESS);
	}
	
	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		sendMessage("Thank you!", getCurrentTx().getSenderAddress());

		if(getCurrentBalance() > MIN_AMOUNT)
			sendBalance(beneficiary);
	}

	/**
	 * Main function for debugging purposes only.
	 * 
	 * This function is not converted into bytecode.
	 */
	public static void main(String[] args) {
		new EmulatorWindow(TipThanks.class);
		compile();
	}
}


