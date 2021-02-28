package bt.sample;

import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * A contract that runs on every block until there is no more balance.
 * 
 * @author jjos
 */
public class AlwaysRunning extends Contract {
  
	// Initialization, cost is less than 1 BURST
	public AlwaysRunning() {
		while(true) {
			sleep(1);
		}
	}

	@Override
	public void txReceived() {
		// we do nothing, as it never leaves the constructor method infinite loop
	}

	public static void main(String[] args) {
		BT.activateCIP20(true);
		BT.setNodeAddress(BT.NODE_TESTNET);
		
		new EmulatorWindow(AlwaysRunning.class);
	}
}
