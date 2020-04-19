package bt.sample;

import bt.Contract;
import bt.ui.EmulatorWindow;
import bt.Timestamp;
import bt.Address;
import bt.BT;

/**
 * A unidirectional (micro) payment channel.
 * 
 * The creator is the payer and someone else is the payee. There is no way for
 * the payer to reduce the BURST amount in an open channel, only to increase it
 * by depositing more BURST on it.
 * 
 * So the payer should write "checks" ({@link #approveAmount(long, long)} signed
 * messages) and send them to the payee (off-chain). Neither the payer nor the
 * payee usually broadcast these messages, since that would cost a transaction
 * fee without additional benefit.
 * 
 * Whenever the payee finds suitable (but before the channel timeout) the
 * payee broadcast the payer mesage for the largest value approved and then
 * closes the channel {@link #closeChannel(long)} to receive the BURST amount.
 * 
 * By extending this contract, a bi-directional channel could be implemented.
 * Either that or simply open two different channels with payer and payee roles
 * inverted.
 * 
 * @author jjos
 */
public class PaymentChannel extends Contract {

	Address payee;
	Timestamp timeout;
	long amountApproved;
	long nonce;

	/**
	 * Open a new channel for the given payee and timeout.
	 * 
	 * Only the creator can open the channel and it must not be currently open.
	 * 
	 * @param payee
	 * @param timeout
	 */
	public void openChannel(Address payee, long timeout) {
		checkTimeout();
		// only creator can open a channel, and it must be currenctly closed (payee==null)
		if (getCurrentTxSender().equals(getCreator()) && this.payee == null) {
			this.payee = payee;
			this.timeout = getBlockTimestamp().addMinutes(timeout);
			this.amountApproved = 0;
		}
	}

	/**
	 * A message that approves the transfer of the given amount to the payee.
	 * 
	 * This message usually will be signed and sent from the payer to the payee
	 * off-chain. The payee can check the message contents and signature off-chain
	 * and then accept the payment **instantly**.
	 * 
	 * @param amount
	 * @param nonce
	 */
	public void approveAmount(long amount, long nonce) {
		checkTimeout();

		if (getCurrentTxSender().equals(getCreator()) && this.nonce == nonce) {
			// Only creator can approve command must be valid:
			// - before timeout
			// - correct nonce (avoids double spend)
			if (amount > amountApproved)
				amountApproved = amount;
		}
	}

	/**
	 * Closes the channel and pays the approved amount to the payee.
	 * 
	 * Only the payee can close the channel.
	 * 
	 * The given nonce must match the nonce stored on the contract. When the channel
	 * is closed the nonce is incremented avoiding double spending on this contract
	 * and allowing to reuse this contract by calling
	 * {@link #openChannel(Address, long)} again.
	 */
	public void closeChannel(long nonce) {
		checkTimeout();
		if (getCurrentTxSender().equals(payee) && this.nonce == nonce) {
			// Only payee can close the channel:
			// - before timeout
			// - correct nonce (avoids double spend)
			sendAmount(amountApproved, payee);
			// increment the nonce, so any previous payment order becomes invalid
			nonce++;
			payee = null;
			timeout = null;
		}
	}

	/**
	 * Utility function to get the channel balance back.
	 * 
	 * Only the creator can call this function. It would be used to get back the
	 * balance of a closed channel or when the channel timeout.
	 */
	public void getBalance() {
		checkTimeout();
		// only creator can get the balance back, the channel must be currently
		// closed (payee==null or timedout)
		if (!getCurrentTxSender().equals(getCreator()))
			return;
		if (payee == null)
			sendBalance(getCreator());
	}

	/**
	 * This contract only accepts the public method calls above.
	 * 
	 * If an unrecognized method was called we do nothing
	 */
	public void txReceived() {
	}

	/**
	 * Private method, not available from blockchain messages, for checking
	 * if this channel timedout.
	 */
	private void checkTimeout() {
		if (getBlockTimestamp().ge(timeout)) {
			// expired
			nonce++;
			payee = null;
		}
	}

	/**
	 * A main function for debugging purposes only.
	 * 
	 * This function is not compiled into bytecode and do not go to the blockchain.
	 */
	public static void main(String[] args) throws Exception {
		BT.activateCIP20(true);
		new EmulatorWindow(PaymentChannel.class);
	}
}
