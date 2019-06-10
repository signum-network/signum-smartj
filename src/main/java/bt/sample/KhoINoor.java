package bt.sample;

import bt.*;

/**
 * The 'Kho-I-Noor' smart contract.
 * 
 * This smart contract was designed to be single, in the sense that there is
 * only one of his category in the entire world.
 * 
 * When created, the contract is <i>owned</i> by the creator and a selling price
 * of 5000 BURST is set. Anyone that transfers more than this amount is the new
 * owner. When a new owner is set, the contract price is automatically rised by 10%.
 * 
 * So, every new owner either have 10% return of investment (minus 1% fee)
 * or keep the ownership of the Kho-I-Noor.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class KhoINoor extends Contract {

	public static final long ACTIVATION_FEE = ONE_BURST * 10;
	public static final long INITIAL_PRICE = ONE_BURST * 5000;

	Address owner, creator;
	long price, fee;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public KhoINoor() {
		creator = getCreator();
		owner = creator;
		price = INITIAL_PRICE;
	}

	/**
	 * A buyer needs to transfer the current price to the contract.
	 * 
	 * Current owner will receive this amount and the sender will become the new
	 * owner. On this event, the price is rised by 10% automatically.
	 * 
	 */
	public void txReceived() {
		if (getCurrentTx().getAmount() + ACTIVATION_FEE >= price) {
			// Conditions match, let's execute the sale
			fee = price / 100; // 1% fee
			sendAmount(price - fee, owner); // pay the current owner the price, minus fee
			sendMessage("Kho-I-Noor have a new owner.", owner);
			owner = getCurrentTx().getSenderAddress(); // new owner
			sendMessage("You are the Kho-I-Noor owner!", owner);

			price += 10 * price / 100; // new price is 10% more

			sendAmount(getCurrentBalance()-ACTIVATION_FEE, creator); // round up with the creator
			return;
		}

		// send back funds of an invalid order
		sendAmount(getCurrentTx().getAmount(), getCurrentTx().getSenderAddress());
		sendMessage("Amount sent was not enough.", getCurrentTx().getSenderAddress());
	}
}
