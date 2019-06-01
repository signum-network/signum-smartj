package bt;

import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * Sleep one block and then send back the amount received.
 */
public class Sleep extends Contract {

	@Override
	public void txReceived() {
		sleep(null);
		sendBalance(getCurrentTx().getSenderAddress());
	}

	public static void main(String[] args) throws Exception {
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();

		Address creator = Emulator.getInstance().getAddress("CREATOR");
		emu.airDrop(creator, 1000 * ONE_BURST);

		Address contract = Emulator.getInstance().getAddress("C");
		emu.createConctract(creator, contract, Sleep.class, ONE_BURST);

		emu.forgeBlock();

		emu.send(creator, contract, 100*ONE_BURST);
		emu.forgeBlock();
		emu.forgeBlock();
		emu.forgeBlock();

		new EmulatorWindow(Sleep.class);
	}
}
