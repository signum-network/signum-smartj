package bt.contracts;

import bt.Contract;
import bt.ui.EmulatorWindow;

public class Cast extends Contract {

	long longValue;
	int intValue;
	boolean worked;
	//byte byteValue;

	@Override
	public void txReceived() {
		longValue = 100000000012345L;
		intValue = 276459577;
		//byteValue = 57;

		if (intValue == (int) longValue) {
			worked = true;
		}
	}

	public static void main(String[] args) {

		System.out.println(100000000012345L);
        System.out.println((int)100000000012345L);
		
		new EmulatorWindow(Cast.class);
	}
}
