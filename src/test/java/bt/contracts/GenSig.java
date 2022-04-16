package bt.contracts;

import bt.BT;
import bt.Contract;
import bt.Register;
import bt.ui.EmulatorWindow;

public class GenSig extends Contract {

	long genSig1;
	Register genSig;

	@Override
	public void txReceived() {
		genSig1 = this.getPrevBlockGenSig1();
		
		genSig = this.getPrevBlockGenSig();
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);
		new EmulatorWindow(GenSig.class);
	}
}
