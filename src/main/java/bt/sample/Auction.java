package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * Auction smart contract.
 * 
 * An Auction smart contract that will allow people to send funds which
 * will be refunded back to them (minus the activation fee) if someone
 * else sends more. Minimum bid should be higher than the INITIAL_PRICE.
 * 
 * After the auction timed-out a final transaction (likely to come from
 * the beneficiary) is needed to trigger the payment and close the auction.
 * 
 * This auction contract cannot be reused, once closed or timedout the
 * auction is never open again.
 *
 * Inspired by http://ciyam.org/at/at_auction.html
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class Auction extends Contract {

	public static final String BENEFICIARY = "BURST-TSLQ-QLR9-6HRD-HCY22";
	public static final long INITIAL_PRICE = 1000*ONE_BURST;
	public static final int TIMEOUT_MIN = 40; // 40 minutes == 10 blocks

	public static final int ACTIVATION_FEE = 30; // expected activation fee in BURST
	
	boolean isOpen;
	Address beneficiary;
	long highestBid;
	Address highestBidder;
	Timestamp timeout;
	
	long newBid, fee;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public Auction(){
		beneficiary = parseAddress(BENEFICIARY);
		timeout = getBlockTimestamp().addMinutes(TIMEOUT_MIN);
		isOpen = true;
		highestBid = INITIAL_PRICE; // Start value, we will not accept less than this
		highestBidder = null;
	}
	
	/**
	 * Private function for checking if the timeout expired.
	 * 
	 * @return true if this contract expired
	 */
	private boolean expired(){
		if(!isOpen)
			return true;
		
		if(getBlockTimestamp().ge(timeout)) {
			isOpen = false;
			fee = getCurrentBalance()/100;
			sendAmount(fee, getCreator());
			// send the funds (best auction) to the beneficiary
			sendBalance(beneficiary);
		}
		return !isOpen;
	}

	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		if(expired()){
			// Send back this last funds or they will be lost
			refund();
			return;
		}

		newBid = getCurrentTxAmount() + ACTIVATION_FEE;
		if(newBid > highestBid){
			// we have a new higher bid, return the previous one
			if(highestBidder != null) {
				sendAmount(highestBid - ACTIVATION_FEE, highestBidder);
			}
			highestBidder = getCurrentTxSender();
			highestBid = newBid;
		}
		else {
			// just send back
			refund();
		}
	}
	
	/**
	 * Refunds the last received transaction
	 */
	private void refund() {
		sendAmount(getCurrentTxAmount(), getCurrentTxSender());
	}

	public static void main(String[] args) throws Exception {
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();

		Address creator = Emulator.getInstance().getAddress(BENEFICIARY);
		emu.airDrop(creator, 1000*Contract.ONE_BURST);
		Address auction = Emulator.getInstance().getAddress("AUCTION");
		emu.createConctract(creator, auction, Auction.class, Contract.ONE_BURST);

		emu.forgeBlock();

		new EmulatorWindow(Auction.class);
		compile();
	}
}


