package bt.sample;

import bt.Address;
import bt.Contract;
import bt.Emulator;
import bt.Timestamp;
import bt.Transaction;
import bt.ui.EmulatorWindow;

/**
 * A contract that pays double or nothing on a 50% chance.
 * 
 * @author jjos
 */
public class OddsGame extends Contract {

	Timestamp lastTimestamp;
	Timestamp next;
	Transaction nextTX;

	/**
	 * This method is executed every time a transaction is received by the contract.
	 */
	@Override
	public void txReceived() {
		// Previous block hash is the random value we use
		long blockOdd = getPrevBlockHash();
		Timestamp timeLimit = getPrevBlockTimestamp();
		blockOdd &= 0xffL; // bitwise AND to avoid negative values
		blockOdd %= 2; // MOD 2 to get just 1 or 0

		nextTX = getTxAfterTimestamp(lastTimestamp);

		while (nextTX != null) {
			next = nextTX.getTimestamp();
			if (next.ge(timeLimit))
				break; // only bets two blocks on past can run

			lastTimestamp = next;

			long pay = (lastTimestamp.getValue() % 2) - blockOdd;

			if (pay == 0) {
				// pay double, minus 5% fees
				long amount = nextTX.getAmount();
				amount = amount * 2;
				sendAmount(amount, nextTX.getSenderAddress());
			}

			nextTX = getTxAfterTimestamp(lastTimestamp);
		}
	}

	public static void main(String[] args) throws Exception {
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();

		Address creator = Emulator.getInstance().getAddress("CREATOR");
		Address bet1 = Emulator.getInstance().getAddress("BET1");
		Address bet2 = Emulator.getInstance().getAddress("BET2");
		emu.airDrop(creator, 1000 * ONE_BURST);
		emu.airDrop(bet1, 1000 * ONE_BURST);
		emu.airDrop(bet2, 1000 * ONE_BURST);
		Address odds = Emulator.getInstance().getAddress("ODDS");
		emu.createConctract(creator, odds, OddsGame.class.getName(), ONE_BURST);

		emu.forgeBlock();

		// 10 bets each
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);
		emu.send(bet1, odds, 100*ONE_BURST);

		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);
		emu.send(bet2, odds, 100*ONE_BURST);

		emu.forgeBlock();
		emu.forgeBlock();
		emu.forgeBlock();

		// another transaction to trigger the sorting mechanism
		emu.send(creator, odds, 10*ONE_BURST);
		emu.forgeBlock();

		new EmulatorWindow(OddsGame.class);
	}
}
