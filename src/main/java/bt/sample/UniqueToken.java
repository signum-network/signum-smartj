package bt.sample;

import bt.*;
import bt.ui.EmulatorWindow;

/**
 * A unique (non-fungible) token smart contract.
 * 
 * This contract represents a non-fungible or unique token for Burst blockchain.
 * 
 * When created the contract is <i>owned</i> by the creator. After that the
 * owner can transfer the ownership to another address. Additionally, the owner
 * can put the token on sale with a given timeout.
 *
 * Although this was inspired by http://erc721.org/, the mechanics are very
 * different. For instance, there is one contract <i>instance</i> per token and
 * each token is already a decentralized exchange (DEX) with absolutely no
 * middle man.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class UniqueToken extends Contract {

	Address owner;
	long salePrice;
	Timestamp saleTimeout;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public UniqueToken() {
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
	public void transfer(Address newOwner) {
		if (owner.equals(this.getCurrentTx().getSenderAddress())) {
			// only current owner can do this
			owner = newOwner;
			salePrice = 0; // not for sale
			saleTimeout = null;
		}
	}

	/**
	 * Put this toke on sale for the given price.
	 * 
	 * @param priceNQT the price in NQT==1E-8 BURST (buyer need to transfer at least
	 *                 this amount)
	 * @param timeout  how many minutes the sale will be available
	 */
	public void putOnSale(long priceNQT, long timeout) {
		if (owner.equals(this.getCurrentTx().getSenderAddress())) {
			// only current owner can do this
			this.salePrice = priceNQT;
			this.saleTimeout = getBlockTimestamp().addMinutes(timeout);
		}
	}

	/**
	 * Buy an on sale token.
	 * 
	 * Buyer needs to transfer the asked price along with the transaction. Recalling
	 * that the amount sent should also cover for the activation fee.
	 * 
	 * If the token was not on sale or the amount is not enough, the order is
	 * refunded (minus the contract activation fee).
	 */
	public void buy() {
		if (salePrice > 0 && getBlockTimestamp().le(saleTimeout) && getCurrentTx().getAmount() >= salePrice) {
			// Conditions match, let's execute the sale
			sendAmount(salePrice, owner); // pay the current owner
			owner = getCurrentTx().getSenderAddress(); // new owner
			salePrice = 0; // not for sale
			saleTimeout = null;
		} else {
			// Invalid order
			txReceived();
		}
	}

	/**
	 * This contract only accepts the public method calls above.
	 * 
	 * If an unrecognized method was called or an invalid order was placed, this
	 * function will refund the order (minus the activation fee).
	 */
	public void txReceived() {
		// send back funds of an invalid order
		sendAmount(getCurrentTx().getAmount(), getCurrentTx().getSenderAddress());
	}

	/**
	 * A main function for debugging purposes only.
	 * 
	 * This function is not compiled into bytecode and do not go to the blockchain.
	 */
	public static void main(String[] args) throws Exception {
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();
		Address creator = Emulator.getInstance().getAddress("CREATOR");
		emu.airDrop(creator, 1000*Contract.ONE_BURST);

		Address token1 = Emulator.getInstance().getAddress("TOKEN1");
		Address token2 = Emulator.getInstance().getAddress("TOKEN2");
		emu.createConctract(creator, token1, UniqueToken.class.getName(), Contract.ONE_BURST);
		emu.createConctract(creator, token2, UniqueToken.class.getName(), Contract.ONE_BURST);

		emu.forgeBlock();

		new EmulatorWindow(UniqueToken.class);
		compile();
	}
}
