package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * This is a sample contract planned to run as a celebration of CIP20.
 * 
 * In case CIP20 goes live, there are plans to run a contract that will return more BURST
 * than it receives until it goes out of balance.
 * 
 * In the implementation below, anyone sending only the activation amount (to be decided)
 * will get back 1 BURST. The implementation is very inefficient, since it iterates over
 * all transaction history for every new transaction. This contract would run out of
 * balance much faster on the previous AT fees.
 * 
 * The idea is to people with new wallets to get a very small anount of BURST from one
 * of the faucets and then get 1 BURST from this contract (automatically activating their
 * accounts).
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class HappyCIP20 extends Contract {

	static final long AMOUNT = ONE_BURST;

	boolean alreadyGotSome;
	Transaction tx, curTX;
	Address sender;

	@Override
	public void txReceived() {
		alreadyGotSome = false;

		curTX = getCurrentTx();
		sender = curTX.getSenderAddress();

		// we will iterate all the transactions, from the first one
		// to the last one
		tx = this.getTxAfterTimestamp(null);
		while(tx!=null && tx != curTX){
			if(tx.getSenderAddress() == sender){
				alreadyGotSome = true;
				break;
			}
			tx = getTxAfterTimestamp(tx.getTimestamp());
		}

		if(!alreadyGotSome){
			sendMessage("Happy CIP20!", sender);
			sendAmount(AMOUNT, sender);
		}
	}

	public static void main(String[] args) {
		new EmulatorWindow(HappyCIP20.class);
	}
}
