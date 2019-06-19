package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;

/**
 * The 'Koh-i-Noor' smart contract.
 * 
 * This smart contract was designed to be single, in the sense that there is
 * only one of his category in the entire world.
 * 
 * When created, the contract is <i>owned</i> by the creator and a selling price
 * of 5000 BURST is set. Anyone that transfers more than this amount is the new
 * owner. When a new owner is set, the contract price is automatically increased by
 * 10%.
 * 
 * So, every new owner either have 10% return of investment (minus 1% fee) or
 * keep the ownership of the Koh-i-Noor.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class KohINoor extends Contract {

	public static final long ACTIVATION_FEE = ONE_BURST * 30;
	public static final long INITIAL_PRICE = ONE_BURST * 5000;

	Address owner, creator;
	long price, fee, activationFee, balance;

	Address curTXAddress;
	long curTXAmount;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public KohINoor() {
		creator = getCreator();
		owner = creator;
		price = INITIAL_PRICE;
		activationFee = ACTIVATION_FEE;
	}

	/**
	 * A buyer needs to transfer the current price to the contract.
	 * 
	 * Current owner will receive this amount and the sender will become the new
	 * owner. On this event, the price is increased by 10% automatically.
	 * 
	 */
	public void txReceived() {
		curTXAmount = getCurrentTx().getAmount();
		curTXAddress = getCurrentTx().getSenderAddress();
		if (curTXAmount + activationFee >= price) {
			// Conditions match, let's execute the sale
			fee = price / 100; // 1% fee
			sendAmount(price - fee, owner); // pay the current owner the price, minus fee
			sendMessage("Koh-i-Noor has a new owner.", owner);
			owner = curTXAddress; // new owner
			sendMessage("You are the Koh-i-Noor owner!", owner);

			price += 10 * price / 100; // new price is 10% more
			return;
		}

		// send back funds of an invalid order
		sendMessage("Amount sent was not enough.", curTXAddress);
		sendAmount(curTXAmount, curTXAddress);
	}

	@Override
	protected void blockFinished() {
		// round up with the creator if there is some balance left
		balance = getCurrentBalance();
		if(balance > activationFee)
			sendAmount(balance - activationFee, creator);
	}
}
