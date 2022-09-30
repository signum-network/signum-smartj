package bt.contracts;

import bt.Contract;
import bt.ui.EmulatorWindow;

public class CalcSqrt extends Contract {

	long sqrt;

	@Override
	public void txReceived() {
		// the exponent is divided by 1_0000_0000, so 0.5 is 5000_0000
		sqrt = calcPow(getCurrentTx().getAmount(), 5000_0000);
	}

	public static void main(String[] args) {
		new EmulatorWindow(CalcSqrt.class);
	}
}
