package bt.sample;

import bt.*;
import bt.ui.EmulatorWindow;

/**
 * A "will" contract that transfers its balance to beneficiary account 
 * if a given timeout is reached.
 *
 * Inspired by http://ciyam.org/at/at_dormant.html
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
class Will extends Contract {
	Address beneficiary;
	Timestamp timeout;
	boolean finished;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public Will(){
		beneficiary = parseAddress("BURST-TSLQ-QLR9-6HRD-HCY22");
		timeout = getBlockTimestamp().addMinutes(30000);
		finished = false;
	}

	/**
	 * Sets a new timeout in minutes from now (if not expired)
	 * 
	 * @param minutes
	 */
	public void setTimeout(int minutes){
		if(!getCurrentTx().getSenderAddress().equals(getCreator()))
			// only creator is allowed
			return;
		if(expired()){
			txReceived();
			return;
		}
		timeout = getBlockTimestamp().addMinutes(minutes);
	}

	/**
	 * Private function for checking if the timeout expired
	 * 
	 * @return true if this contract expired
	 */
	private boolean expired(){
		return finished || getBlockTimestamp().ge(timeout);
	}

	/**
	 * Private function that pays this contract.
	 */
	private void pay(){
		sendBalance(beneficiary);
		finished = true;
	}

	/**
	 * Sets a new payout address (if not expired)
	 * 
	 * @param newPayout
	 */
	public void setPayoutAddress(Address newPayout){
		if(!getCurrentTx().getSenderAddress().equals(this.getCreator()))
			// only creator is allowed
			return;

		getFieldValues();

		if(expired()){
			txReceived();
			return;
		}
		beneficiary = newPayout;
	}


	/**
	 * Any call not recognized will be handled by the this function
	 */
	@Override
	public void txReceived(){
		if(finished && getCurrentTx().getAmount()>0){
			// Send back funds or they will be lost
			sendAmount(getCurrentTx().getAmount(), getCurrentTx().getSenderAddress());
		}
		else if(expired()){
			pay();
		}
		// Otherwise do nothing, wait for the timeout
	}

	public static void main(String[] args) {
		new EmulatorWindow(Will.class);
	}
}


