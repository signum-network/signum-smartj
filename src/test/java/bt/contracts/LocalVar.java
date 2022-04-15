package bt.contracts;

import bt.Contract;
import bt.ui.EmulatorWindow;


public class LocalVar extends Contract {

	public static final long FEE = Contract.ONE_BURST;

	long amountNoFee;
	long valueTimes2;

	@Override
	public void txReceived() {
		long local = getCurrentTx().getAmount();
		amountNoFee = local + FEE;
	}

	public void setValue(long value) {
		valueTimes2 = value + value;
	}

	public static void main(String[] args) throws Exception {
		new EmulatorWindow(LocalVar.class);
	}
}
