package bt.dapps;

import bt.Address;
import bt.BT;
import bt.Contract;
import bt.Register;
import bt.Timestamp;
import bt.Transaction;
import bt.ui.EmulatorWindow;

/**
 * A smart contract for atomic cross-chain liquidity pool for trading SIGNA and
 * other coins.
 *
 * A single contract can handle multiple users and is created only once by
 * the system manager.
 *
 * @author jjos
 */
public class AtomicPool extends Contract {

	public static final long ACTIVATION_FEE = 50000000L;

	public static final long STATE_FINISHED = 0L;
	public static final long STATE_LOCKED = 1L;

	public static final long FEE_DIVISION = 400; // 0.25 % fee

	/**
	 * Address of the contract collecting the fees to be distributed among TRT
	 * holders
	 */
	Address feeContract;

	long numberOfLockedDeposits;

	// Map-value keys
	public static final long KEY_STATE = 1;
	public static final long KEY_OWNER = 2;
    public static final long KEY_DEPOSIT_TRANSACTION = 3;
    public static final long KEY_AMOUNT = 4;
	public static final long KEY_BENEFICIARY = 5;
	public static final long KEY_TIMEOUT = 6;

	// Temporary variables
	Timestamp timeout;
	Transaction depositTransaction;
	Address beneficiary;
	long fee;
	long amount;
	boolean secretMatched;

	Register secretHash, secret;

	/**
	 * Lock a deposit with a given beneficiary and timeout in minutes.
	 *
	 * @param beneficiary
	 * @param timeoutMinutes
	 */
	public void deposit(Address beneficiary, long timeoutMinutes) {
		setMapValue(KEY_OWNER, getCurrentTx().getId(), getCurrentTxSender().getId());
        setMapValue(KEY_STATE, getCurrentTx().getId(), STATE_LOCKED);
        setMapValue(KEY_DEPOSIT_TRANSACTION, getCurrentTx().getId(), getCurrentTx().getId());
		setMapValue(KEY_BENEFICIARY, getCurrentTx().getId(), beneficiary.getId());
		setMapValue(KEY_AMOUNT, getCurrentTx().getId(), getCurrentTxAmount());

		timeout = getCurrentTxTimestamp().addMinutes(timeoutMinutes);
		setMapValue(KEY_TIMEOUT, getCurrentTx().getId(), timeout.getValue());

		numberOfLockedDeposits++;
	}

	/**
	 * Refund the locked amount after the lock timed-out.
	 *
	 * @param offer the offer id to cancel.
	 */
	public void refund(long offer) {
		if (getMapValue(KEY_STATE, offer) == STATE_LOCKED) {
			// only if the amount is still locked

			// check if the timeout has expired so we can refund
			timeout = getTimestamp(getMapValue(KEY_TIMEOUT, offer));
			if (getCurrentTxTimestamp().le(timeout)) {
				return;
			}
			// we send back the amount locked to the original owner
			depositTransaction = getTransaction(getMapValue(KEY_DEPOSIT_TRANSACTION, offer));
			beneficiary = getAddress(getMapValue(KEY_OWNER, offer));
			pay();
		}
	}

	/**
	 * Claim (pay the beneficiary) by sending the secret.
	 *
	 * @param offer the offer id to claim.
	 */
	public void claim(long offer) {
		if (getMapValue(KEY_STATE, offer) == STATE_LOCKED) {
			depositTransaction = getTransaction(getMapValue(KEY_DEPOSIT_TRANSACTION, offer));
			if (depositTransaction == null) {
				return;
			}

			// secret hash is on the page 1 of the original message
			secretHash = depositTransaction.getMessage(1);
			// we are sending the secret on the page 1 of this claim transaction
			secret = getCurrentTx().getMessage(1);

			secretMatched = checkSHA256(secretHash, secret);
			if (!secretMatched) {
				return;
			}

			beneficiary = getAddress(getMapValue(KEY_BENEFICIARY, offer));
			pay();
		}
	}

	/**
	 * Private method to pay/refund a deposit.
	 */
	private void pay() {
		amount = getMapValue(KEY_AMOUNT, depositTransaction.getId());

		// 0.25 % fee
		fee = amount / FEE_DIVISION;
		amount -= fee;

		setMapValue(KEY_STATE, depositTransaction.getId(), STATE_FINISHED);
		sendAmount(amount, beneficiary);
		sendAmount(fee, feeContract);
		numberOfLockedDeposits--;
	}

	@Override
	public void txReceived() {
		if (numberOfLockedDeposits == 0L) {
			// if there is no locked deposits, clean up the dust
			sendBalance(feeContract);
		}
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);

		new EmulatorWindow(AtomicPool.class);
	}
}
