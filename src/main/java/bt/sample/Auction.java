package bt.sample;

import bt.Contract;
import bt.Address;
import bt.Timestamp;

/**
 * Auction smart contract.
 * 
 * An Auction smart contract that will allow people to send funds which
 * will be refunded back to them (minus the activation fee) if someone
 * else sends more. Minimum bid should be higher than 100 BURST.
 *
 * Inspired by http://ciyam.org/at/at_auction.html
 * 
 * @author jjos
 */
public class Auction extends Contract {
	
	Address beneficiary;
	long highestBid;
	Address highestBidder;
	Timestamp timeout;
	boolean finished;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public Auction(){
		beneficiary = parseAddress("BURST-TSLQ-QLR9-6HRD-HCY22");
		timeout = getBlockTimestamp().addMinutes(60*24*15); // 15 days from now
		finished = false;
		highestBid = 100*ONE_BURST; // Start value, we will not accept less than this
		highestBidder = null;
	}
	
	/**
	 * Private function for checking if the timeout expired.
	 * 
	 * @return true if this contract expired
	 */
	private boolean expired(){
		if(finished)
			return true;
		
		if(getBlockTimestamp().ge(timeout)) {
			finished = true;
			// send the funds (best auction) to the beneficiary
			sendBalance(beneficiary);
		}
		return finished;
	}

	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		if(expired()){
			// Send back this last funds or they will be lost
			returnThisTx();
			return;
		}

		long newBid = getCurrentTx().getAmount();
		if(newBid > highestBid){
			// we have a new higher bid, return the previous one
			if(highestBidder != null) {
				sendAmount(highestBid, highestBidder);
			}
			highestBidder = getCurrentTx().getSenderAddress();
			highestBid = newBid;
		}
		else {
			// just send back
			returnThisTx();
		}
	}
	
	private void returnThisTx() {
		if(getCurrentTx().getAmount()>0)
			sendAmount(getCurrentTx().getAmount(), getCurrentTx().getSenderAddress());
	}
}


