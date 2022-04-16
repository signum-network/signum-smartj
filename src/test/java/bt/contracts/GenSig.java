package bt.contracts;

import bt.BT;
import bt.Contract;
import bt.Register;
import bt.ui.EmulatorWindow;

public class GenSig extends Contract {

	long genSig;
	Register genSigReg;

	@Override
	public void txReceived() {
		genSig = this.getPrevBlockGenSig1();
		
		genSigReg = this.getPrevBlockGenSig();
	}

	public static void main(String[] args) {
		BT.activateSIP37(true);
		new EmulatorWindow(GenSig.class);
	}
}
