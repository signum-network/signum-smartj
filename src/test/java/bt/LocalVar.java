package bt;

import bt.Contract;
import bt.ui.EmulatorWindow;

public class LocalVar extends Contract {

	boolean equalReceived;
	long amountPlusOne;
	long doubleValue;

	@Override
	public void txReceived() {
		long amount = getCurrentTx().getAmount();

		amountPlusOne = amount+Contract.ONE_BURST;
	}

	public void setValue(long value) {
		doubleValue = value + value;
	}

	public static void main(String[] args) {
		new EmulatorWindow(LocalVar.class);
	}
}
