package bt.contracts;

import bt.Contract;
import bt.ui.EmulatorWindow;

public class CalcSqrt extends Contract {

	long sqrt;

	@Override
	public void txReceived() {
		sqrt = calcPow(getCurrentTx().getAmount(), 5000);
	}

	public static void main(String[] args) {
		new EmulatorWindow(CalcSqrt.class);
	}
}
