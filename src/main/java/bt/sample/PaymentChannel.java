package bt.sample;

import bt.Contract;
import bt.ui.EmulatorWindow;
import bt.Timestamp;
import bt.Address;
import bt.BT;

/**
 * A unidirectional (micro) payment channel.
 * 
 * The creator is the payer and someone else is the receiver. There is no way for
 * the payer to reduce the SIGNA amount in an open channel, only to increase it
 * by depositing more SIGNA on it.
 * 
 * So the payer should write "checks" (signed messages) and send them to the
 * receiver (off-chain). Neither the payer nor the receiver usually broadcast
 * these messages, since that would cost a transaction fee without additional benefit.
 * 
 * Whenever the receiver finds suitable, he/she broadcasts the payer message
 * for the largest value approved by calling {@link #claimPayment(long, long)}
 * to receive the SIGNA amount.
 * 
 * The creator can call {@link #askToClose()} to either get back the locked funds
 * after the deadline or to force the receiver to claim the payment.
 * 
 * 
 * @author jjos
 */
public class PaymentChannel extends Contract {

	Address receiver;
	long deadline;
	long minimumPayout;
	Timestamp timeout;
	long nonce;
	
	/**
	 * Open a new channel for the given receiver and deadline in minutes.
	 * 
	 * Only the creator can open the channel and it must not be currently open.
	 * 
	 * @param receiver
	 * @param timeout
	 */
	public void openChannel(Address receiver, long deadline, long minimumPayout) {
		// only creator can open a channel, and it must be currently closed (receiver==null)
		if (getCurrentTxSender().equals(getCreator()) && this.receiver == null) {
			this.receiver = receiver;
			this.deadline = deadline;
			this.minimumPayout = minimumPayout;
			this.timeout = null;
		}
	}

	/**
	 * Process a message that approves the transfer of the given amount to the receiver.
	 * 
	 * This message usually will be signed and sent from the payer to the receiver
	 * off-chain. The receiver can check the message contents and signature off-chain
	 * and then accept the payment **instantly** if the amount is lower than the
	 * current contract balance.
	 * 
	 * @param amount
	 * @param nonce
	 */
	public void claimPayment(long nonce, long amount) {
		checkTimeout();

		if(this.nonce == nonce && checkSignature(nonce, amount, receiver.getId(), getCurrentTx(), 1L, getCreator().getId()) != 0) {
			// We got a valid signature with the correct nonce (avoids double spend)
			sendAmount(amount, receiver);
			
			resetContract();
		}
	}

	/**
	 * Resets the contract, sends the minimum payout to the receiver and the balance back to the creator.
	 */
	private void resetContract() {
		sendAmount(minimumPayout, receiver);
		nonce++;
		receiver = null;
		timeout = null;
		sendBalance(getCreator());
	}
	
	
	/**
	 * The creator is asking to close the channel.
	 * 
	 * Only the creator can call this method. It would be used to get back the
	 * remaining balance in the contract or to force the receiver to claim the payment.
	 * If any off-chain payment was made to the receiver, he/she has to be broadcast it
	 * to {@link #claimPayment(long, long)} before the timeout.
	 */
	public void askToClose() {
		checkTimeout();
		// only creator can ask to close the channel
		if (timeout != null || !getCurrentTxSender().equals(getCreator()))
			return;
		
		if(receiver != null) {
			// initiate the timeout so we can close it later
			timeout = getCurrentTxTimestamp().addMinutes(deadline);
		}
	}

	/**
	 * This contract only accepts the public method calls above.
	 * 
	 * If an unrecognized method was called we do nothing. This can be used to increase the
	 * contract balance.
	 */
	public void txReceived() {
	}

	/**
	 * Private method, not available from blockchain messages, for checking
	 * if this channel timedout.
	 */
	public void checkTimeout() {
		if (timeout!=null && getBlockTimestamp().ge(timeout)) {
			// expired
			resetContract();
		}
	}

	/**
	 * A main function for debugging purposes only.
	 * 
	 * This function is not compiled into bytecode and do not go to the blockchain.
	 */
	public static void main(String[] args) throws Exception {
		BT.activateSIP37(true);
		new EmulatorWindow(PaymentChannel.class);
	}
}
