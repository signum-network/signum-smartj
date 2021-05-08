package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * An betting game where each participant has a chance of winning proportional to the amount bet.
 * 
 * Someone creates this contract and then send some amount to initialize it. Let's say the creator
 * send 1000 BURST plus fees. Then, say a challenger bet against the creator by sending 100 BURST
 * plus fees.
 * 
 * Based on the next block hash (on future) a random number will be generated between 1 and 1000+100.
 * Any number between 1-1000 will make the creator the winner and any number between 1001-1100
 * will make the challenger the winner. So, if you bet with a larger amount you increase your
 * chances but put more at stake.
 * 
 * This code tips the developer 0.5% when the creator is the winner and 1% when the challenger
 * is the winner. This difference is to make incentives in putting bets available.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class BurstGame extends Contract {

	Address challenger;

	long challengerAmount;
	long creatorAmount;
	long balance;
	long blockHash;

	static final String DEV_ADDRESS = "BURST-JJQS-MMA4-GHB4-4ZNZU";

	public void txReceived(){
		challenger = getCurrentTx().getSenderAddress();
		if(challenger == getCreator()){
			if(getCurrentTx().getAmount() == 0){
				// If creator sends a message with 0 amount (exactly the activation fee)
				// we withdraw the current balance
				sendBalance(challenger);
			}
			// do nothing, creator is just increasing his amount
			return;
		}

		challengerAmount = getCurrentTx().getAmount();

		balance = getCurrentBalance();
		creatorAmount = balance - challengerAmount;

		// sleep two blocks
		sleep(2);

		blockHash = getPrevBlockHash().getValue1();
		blockHash &= 0x0FFFFFFFFFFFFFFFL; // avoid negative values
		blockHash %= balance;
		
		if(blockHash < creatorAmount){
			// tip developer with 0.5 percent
			sendAmount(balance/200, parseAddress(DEV_ADDRESS));

			// creator wins
			sendBalance(getCreator());
		}
		else {
			// tip developer with 1 percent
			sendAmount(balance/100, parseAddress(DEV_ADDRESS));

			// challenger wins
			sendBalance(challenger);
		}
	}

	/**
	 * Main function, for debbuging purposes only, not exported to bytecode.
	 */
	public static void main(String[] args) throws Exception {
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();

		Address creator = Emulator.getInstance().getAddress("CREATOR");
		Address challenger = Emulator.getInstance().getAddress("CHALLENGER");
		emu.airDrop(creator, 1000 * ONE_BURST);
		emu.airDrop(challenger, 1000 * ONE_BURST);

		Address odds = Emulator.getInstance().getAddress("GAME");
		emu.createConctract(creator, odds, BurstGame.class, ONE_BURST);

		emu.forgeBlock();

		// 10 bets each
		emu.send(challenger, odds, 100*ONE_BURST);
		emu.send(challenger, odds, 100*ONE_BURST);
		emu.send(challenger, odds, 100*ONE_BURST);
		emu.send(challenger, odds, 100*ONE_BURST);
		emu.send(challenger, odds, 100*ONE_BURST);
		emu.send(challenger, odds, 100*ONE_BURST);
		emu.send(challenger, odds, 100*ONE_BURST);
		emu.send(challenger, odds, 100*ONE_BURST);
		emu.send(challenger, odds, 100*ONE_BURST);
		emu.send(challenger, odds, 100*ONE_BURST);

		emu.forgeBlock();
		emu.forgeBlock();

		// another transaction to trigger the sorting mechanism
		emu.send(challenger, odds, 10*ONE_BURST);
		emu.forgeBlock();
		emu.send(challenger, odds, 20*ONE_BURST);
		emu.forgeBlock();
		emu.forgeBlock();
		emu.send(challenger, odds, 30*ONE_BURST);
		emu.forgeBlock();
		emu.forgeBlock();

		new EmulatorWindow(BurstGame.class);
	}
}
