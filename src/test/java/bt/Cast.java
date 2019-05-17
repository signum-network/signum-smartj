package bt;

import bt.Contract;
import bt.ui.EmulatorWindow;

public class Cast extends Contract {

	long longValue;
	int intValue;
	//byte byteValue;

	@Override
	public void txReceived() {
		longValue = 100000000012345L;
		intValue = 276459577;
		//byteValue = 57;

		if (intValue == (int) longValue)
			sendMessage("Cast int is working", getCurrentTx().getSenderAddress());
		//if (byteValue == (byte) longValue)
		//	sendMessage("Cast byte is working", getCurrentTx().getSenderAddress());
	}

	public static void main(String[] args) {

		System.out.println(100000000012345L);
        System.out.println((int)100000000012345L);
		
		new EmulatorWindow(Cast.class);
	}
}
