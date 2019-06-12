package bt;

import bt.compiler.Compiler;
import bt.sample.KohINoor;
import bt.ui.EmulatorWindow;

/**
 * Test on emulator.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class KohINoorEmu {

	public static void main(String[] args) throws Exception {

		Compiler comp = new Compiler(KohINoor.class);
		comp.compile();
		comp.link();

		System.out.println("creator:" + comp.getField("creator").getAddress());
		System.out.println("owner:" + comp.getField("owner").getAddress());
		System.out.println("price:" + comp.getField("price").getAddress());

		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();
		Address creator = Emulator.getInstance().getAddress("CREATOR");
		Address buyer1 = Emulator.getInstance().getAddress("B1");
		Address buyer2 = Emulator.getInstance().getAddress("B2");

		emu.airDrop(creator, 1000 * Contract.ONE_BURST);
		emu.airDrop(buyer1, 10000 * Contract.ONE_BURST);
		emu.airDrop(buyer2, 10000 * Contract.ONE_BURST);

		Address diamond = Emulator.getInstance().getAddress("DIAMOND");
		emu.createConctract(creator, diamond, KohINoor.class, KohINoor.ACTIVATION_FEE);
		emu.forgeBlock();

		emu.send(buyer1, diamond, 5000 * Contract.ONE_BURST);
		emu.forgeBlock();

		emu.send(buyer2, diamond, 5500 * Contract.ONE_BURST);
		emu.forgeBlock();

		new EmulatorWindow(KohINoor.class);
	}
}
