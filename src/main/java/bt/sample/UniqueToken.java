package bt.sample;

import bt.Contract;
import bt.EmulatorWindow;
import bt.Timestamp;
import bt.Address;

/**
 * A unique (non-fungible) token smart contract.
 * 
 * This contract represents a non-fungible or unique token for Burst blockchain.
 * 
 * When created it is owned by the creator. After that the owner can transfer
 * the ownership to another address.
 * Additionally, the owner can put the token on sale with a given timeout.
 *
 * Inspired by http://erc721.org/
 * 
 * @author jjos
 */
public class UniqueToken extends Contract {
	
	Address owner;
	long price;
	Timestamp timeout;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public UniqueToken(){
		// Initially token is owned by the creator
		owner = getCreator();
	}

	/**
	 * Transfers the ownership of this token.
	 * 
	 * Only the current owner can transfer the ownership.
	 * 
	 * @param newOwner
	 */
	public void transfer(Address newOwner){
		if(owner.equals(this.getCurrentTx().getSenderAddress())){
			// only owner can transfer ownership
			owner = newOwner;
			price = 0; // not for sale
		}
	}

	/**
	 * Put this toke on sale for the given price.
	 * 
	 * @param price the price (buyer need to transfer at least this amount)
	 * @param timeout how many minutes the sale will be available
	 */
	public void sell(long price, long timeout){
		this.price = price;
		this.timeout = getBlockTimestamp().addMinutes(timeout);
	}

	/**
	 * Buy an on sale token.
	 * Buyer need to transfer the asked price
	 */
	public void buy(){
		if(price>0 && getBlockTimestamp().le(timeout) && getCurrentTx().getAmount() >= price){
			// conditions match, let's execute the sale
			sendAmount(price, owner); // pay the current owner
			owner = getCurrentTx().getSenderAddress(); // new owner
			price = 0; // not for sale
		}
		else {
			// send back funds of an invalid order
			sendAmount(getCurrentTx().getAmount(), getCurrentTx().getSenderAddress());
		}
	}

	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		// do nothing
	}
	
	public static void main(String[] args) {
		new EmulatorWindow(UniqueToken.class);
	}
}


