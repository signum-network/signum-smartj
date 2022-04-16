package bt.contracts;

import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

public class CalcMultDiv96 extends Contract {

	long result;

	@Override
	public void txReceived() {
		result = calcMultDiv(getCurrentTxAmount(), 9600000000000000L, 10000000000000000L);
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);
		new EmulatorWindow(CalcMultDiv96.class);
	}
}
