package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A hashed time lock smart contract.
 * 
 * This small sample can be extended to implement cross-chain atomic swaps.
 * 
 * This contract should be initialized with a hashlock and beneficiary.
 * 
 * If the beneficiary sends the correct key the funds are unlocked. If the
 * creator send a transaction and the timelock as passed, the funds are
 * withdraw.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class HashedTimeLock extends Contract {

	/** Expected activation fee in BURST */
	public static final long ACTIVATION_FEE = 30 * ONE_BURST;

	/** Timeout in minutes */
	public static final int TIMEOUT = 12;

	Register hashlock;
	Address beneficiary;

	Timestamp timelock;
	Register key, hashedKey;

	/**
	 * This constructor is called the first time the contract receives a
	 * transaction.
	 * 
	 * The contract creator should send a transaction with the amount to be
	 * locked. After this transaction is received the timelock is set with
	 * the configured TIMEOUT.
	 */
	public HashedTimeLock() {
		timelock = getBlockTimestamp().addMinutes(TIMEOUT);
	}

	/**
	 * Any new transaction received will be handled by this function.
	 * 
	 * The benefitiary should send a message with the key to unlock the funds. The
	 * creator can withdraw the funds if the timelock has expired.
	 * 
	 */
	public void txReceived() {
		if (getCurrentTxSender() == beneficiary) {
			// lets check the hash
			key = getCurrentTx().getMessage();
			hashedKey = performSHA256(key);
			if (hashedKey.equals(hashlock))
				sendAmount(getCurrentBalance(), beneficiary);
		}

		if (getCurrentTxSender() == getCreator() && getBlockTimestamp().ge(timelock)) {
			// creator can claim back the balance after the timelock
			sendAmount(getCurrentBalance(), getCreator());
		}
	}

	public static void main(String[] args) throws Exception {
		new EmulatorWindow(HashedTimeLock.class);
	}
}
