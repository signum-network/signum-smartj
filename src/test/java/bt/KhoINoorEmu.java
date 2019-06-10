package bt;

import bt.sample.KhoINoor;
import bt.ui.EmulatorWindow;

/**
 * Test on emulator.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class KhoINoorEmu {

	public static void main(String[] args) throws Exception {
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();
		Address creator = Emulator.getInstance().getAddress("CREATOR");
		Address buyer1 = Emulator.getInstance().getAddress("B1");
		Address buyer2 = Emulator.getInstance().getAddress("B2");

		emu.airDrop(creator, 1000 * Contract.ONE_BURST);
		emu.airDrop(buyer1, 10000 * Contract.ONE_BURST);
		emu.airDrop(buyer2, 10000 * Contract.ONE_BURST);

		Address diamond = Emulator.getInstance().getAddress("DIAMOND");
		emu.createConctract(creator, diamond, KhoINoor.class, KhoINoor.ACTIVATION_FEE);
		emu.forgeBlock();

		emu.send(buyer1, diamond, 5000 * Contract.ONE_BURST);
		emu.forgeBlock();

		emu.send(buyer2, diamond, 5500 * Contract.ONE_BURST);
		emu.forgeBlock();

		new EmulatorWindow(KhoINoor.class);
	}
}
