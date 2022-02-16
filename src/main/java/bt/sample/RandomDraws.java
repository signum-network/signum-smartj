package bt.sample;

import bt.Address;
import bt.Contract;
import bt.Emulator;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class RandomDraws extends Contract {
	static final long HASH_MASK = 0x0FFFFFFFFFFFFFFFL;
	String gameMessage = "";
	long hash;

	@Override
	public void txReceived() {
		buildSeed();
		gameMessage += " -John draws " + randomNumber(100);
		gameMessage += " -Karen draws " + randomNumber(100);
		sendMessage(gameMessage, getCurrentTx().getSenderAddress());
	}

	private long randomNumber(int max) {
		hash = performSHA256_64(hash, hash);
		long drawNumber = hash & HASH_MASK;
		drawNumber %= max;
		drawNumber += 1;
		return drawNumber;
	}

	private void buildSeed() {
		long hash1 = getPrevBlockHash1();
		hash = hash1;
		for (int i = 1; i <= 10; i++) {// number of previous blocks to generate random from hash
			hash *= 2; // shift by one bit
			hash += (getPrevBlockHash1() & 1);
		}
		hash = performSHA256_64(hash, hash1);
	}

	public static void main(String[] args) throws Exception {
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();

		Address creatorAdr = Emulator.getInstance().getAddress("CREATOR");
		Address playerAdr = Emulator.getInstance().getAddress("CHALLENGER");
		emu.airDrop(creatorAdr, 1000 * ONE_BURST);
		emu.airDrop(playerAdr, 1000 * ONE_BURST);

		Address bjRobotAdr = Emulator.getInstance().getAddress("BJROBOT");
		emu.createConctract(creatorAdr, bjRobotAdr, RandomDraws.class, ONE_BURST);
		emu.forgeBlock();
		emu.send(playerAdr, bjRobotAdr, 10 * ONE_BURST);
		emu.forgeBlock();
		emu.send(playerAdr, bjRobotAdr, 10 * ONE_BURST);
		emu.forgeBlock();
		emu.send(playerAdr, bjRobotAdr, 10 * ONE_BURST);
		emu.forgeBlock();
		emu.send(playerAdr, bjRobotAdr, 10 * ONE_BURST);
		emu.forgeBlock();

		new EmulatorWindow(RandomDraws.class);
	}
}
