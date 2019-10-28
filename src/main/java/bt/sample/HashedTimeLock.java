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
 * This contract should be initialized with a hashlock, timelock, and
 * benefitiary.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class HashedTimeLock extends Contract {

	/** Expected activation fee in BURST */
	public static final int ACTIVATION_FEE = 22;

	Register hashlock;
	Timestamp timelock;
	Address beneficiary;

	Register key, hashedKey;

	/**
	 * Any new transaction received will be handled by this function.
	 * 
	 * The benefitiary should send a message with the key to unlock the funds. The
	 * creator can withdraw the funds if the timeout has passed.
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
