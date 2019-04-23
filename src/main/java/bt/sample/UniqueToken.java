package bt.sample;

import bt.Contract;
import bt.EmulatorWindow;
import bt.Address;

/**
 * A unique token smart contract.
 * 
 * This contract represents a non-fungible or unique token for Burst blockchain.
 * 
 * When created it is owned by the creator. After that it can be transfered
 * to other addresses. Only the owner can transfer the ownership.
 *
 * Inspired by http://erc721.org/
 * 
 * @author jjos
 */
public class UniqueToken extends Contract {
	
	Address owner;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public UniqueToken(){
		// Initially token is owned by the creator
		owner = getCreator();
	}

	public void transfer(Address newOwner){
		if(owner.equals(this.getCurrentTx().getSenderAddress())){
			// only owner can transfer ownership
			owner = newOwner;
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


