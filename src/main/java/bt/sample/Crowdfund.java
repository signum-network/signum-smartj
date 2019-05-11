package bt.sample;

import bt.*;
import bt.ui.EmulatorWindow;

/**
 * Crowdfunding smart contract
 * 
 * If a target balance is achieved by a hard-coded time then the entire
 * balance will be sent to an account which is also hard-coded.
 * If not then the txs that were sent to this contract will be iterated and
 * refunded to one by one.
 *
 * Inspired by http://ciyam.org/at/at_crowdfund.html
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
class Crowdfund extends Contract {
	
	Address targetAddress;
	long targetAmount;
	long raisedAmount;
	Timestamp timeout;
	boolean paid;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public Crowdfund(){
		targetAddress = parseAddress("BURST-TSLQ-QLR9-6HRD-HCY22");
		targetAmount = 1000000 * ONE_BURST;
		timeout = getBlockTimestamp().addMinutes(60*24*15); // 15 days from now
		paid = false;
	}

	/**
	 * Private function for checking if the timeout expired.
	 * 
	 * @return true if this contract expired
	 */
	private boolean expired(){
		return paid || getBlockTimestamp().ge(timeout);
	}

	/**
	 * Private function that pays this contract.
	 */
	private void pay(){
		if(raisedAmount >= targetAmount) {
			sendBalance(targetAddress);			
		}
		else {
			// send back funds
			Timestamp ts = getCreationTimestamp();
			while(true) {
				Transaction txi = getTxAfterTimestamp(ts);
				if(txi == null)
					break;
				sendAmount(txi.getAmount(), txi.getSenderAddress());
				ts = txi.getTimestamp();
			}

		}
		paid = true;
	}

	/**
	 * A new transaction received (new participant)
	 */
	@Override
	public void txReceived(){
		if(paid){
			// Send back funds or they will be lost
			sendAmount(getCurrentTx().getAmount(), getCurrentTx().getSenderAddress());
			return;
		}

		raisedAmount += getCurrentTx().getAmount();
		if(expired()){
			pay();
		}
	}

	public static void main(String[] args) {
		new EmulatorWindow(Crowdfund.class);
	}
}


