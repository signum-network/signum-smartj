package bt;

import bt.Contract;
import bt.ui.EmulatorWindow;

public class TestIf extends Contract {

	long longValue;

	@Override
	public void txReceived() {
		if(getCurrentTx().getAmount() > ONE_BURST){
			longValue = getCurrentTx().getAmount();
		}
	}

	public static void main(String[] args) {
		new EmulatorWindow(TestIf.class);
	}
}
