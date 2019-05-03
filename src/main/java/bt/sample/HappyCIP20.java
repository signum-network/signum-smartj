package bt.sample;

import bt.Address;
import bt.Contract;
import bt.Transaction;
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
public class HappyCIP20 extends Contract {

	static final long AMOUNT = ONE_BURST;

	@Override
	public void txReceived() {
		boolean alreadyGotSome = false;

		Transaction curTX = getCurrentTx();
		Address receiver = curTX.getReceiverAddress();

		// we will iterate all the transactions, from the first one
		// to the last one
		Transaction tx = this.getTxAfterTimestamp(null);
		while(tx!=null && tx != curTX){
			if(tx.getSenderAddress() == receiver){
				alreadyGotSome = true;
				break;
			}
			tx = getTxAfterTimestamp(tx.getTimestamp());
		}

		if(!alreadyGotSome){
			sendMessage("Happy CIP20!", receiver);
			sendAmount(AMOUNT, receiver);
		}
	}

	public static void main(String[] args) {
		new EmulatorWindow(Refund.class);
	}
}
