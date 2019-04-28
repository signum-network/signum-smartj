package bt.sample;

import bt.Contract;
import bt.Emulator;
import bt.ui.EmulatorWindow;
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
	long salePrice;
	Timestamp saleTimeout;

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
			salePrice = 0; // not for sale
		}
	}

	/**
	 * Put this toke on sale for the given price.
	 * 
	 * @param price the price (buyer need to transfer at least this amount)
	 * @param timeout how many minutes the sale will be available
	 */
	public void putOnSale(long price, long timeout){
		this.salePrice = price;
		this.saleTimeout = getBlockTimestamp().addMinutes(timeout);
	}

	/**
	 * Buy an on sale token.
	 * 
	 * Buyer needs to transfer the asked price along with the transaction.
	 * Recalling that the amount sent should also cover for the activation fee.
	 */
	public void buy(){
		if(salePrice>0 && getBlockTimestamp().le(saleTimeout) && getCurrentTx().getAmount() >= salePrice){
			// conditions match, let's execute the sale
			sendAmount(salePrice, owner); // pay the current owner
			owner = getCurrentTx().getSenderAddress(); // new owner
			salePrice = 0; // not for sale
		}
		else {
			// send back funds of an invalid order
			sendAmount(getCurrentTx().getAmount(), getCurrentTx().getSenderAddress());
		}
	}

	/**
	 * This contract only accepts the public method calls above.
	 * 
	 * We will do nothing if an unrecognized transaction comes.
	 */
	public void txReceived(){
		// do nothing
	}
	
	/**
	 * A main function for debugging purposes only.
	 * 
	 * This function is not compiled into bytecode and do not go to the blockchain.
	 */
	public static void main(String[] args) throws Exception {
		Emulator emu = Emulator.getInstance();

		// some initialization code to make things easier to debug
		Address creator = Emulator.getInstance().getAddress("CREATOR");
		emu.airDrop(creator, 1000*Contract.ONE_BURST);

		Address token1 = Emulator.getInstance().getAddress("TOKEN1");
		Address token2 = Emulator.getInstance().getAddress("TOKEN2");
		emu.createConctract(creator, token1, UniqueToken.class.getName(), Contract.ONE_BURST);
		emu.createConctract(creator, token2, UniqueToken.class.getName(), Contract.ONE_BURST);

		emu.forgeBlock();

		new EmulatorWindow(UniqueToken.class);
	}
}


