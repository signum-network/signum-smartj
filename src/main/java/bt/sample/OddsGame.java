package bt.sample;

import bt.Address;
import bt.Contract;
import bt.Emulator;
import bt.Timestamp;
import bt.Transaction;
import bt.ui.EmulatorWindow;

/**
 * An 'odds and evens game' contract that pays double or nothing on a 50% chance.
 * 
 * From the amount sent to the contract the activation fee is subtracted and this resulting
 * amount is doubled if the sender wins. Let's say the activation fee was set as 10 BURST,
 * a winning bet of 100 BURST will receive back (100-10)*2 == 180 BURST. A winning bet
 * of 1000 BURST will receive back (1000-10)*2 = 1980 BURST.
 * 
 * Every transaction sent to this contract has an even or odd value attributed
 * according to the transaction number. A block in future is used to decide the winning
 * value (even or odd), chosen based on the block hash (random source).
 * 
 * Winners of previous bets are computed as new bets come in, so people need to keep
 * betting to keep the system working.
 * 
 * A value in the future is used as source for randomness to difficult tampering
 * from malicious miners. For the same reason, a high activation fee is also advisable
 * and a MAX_PAYMENT is set.
 * 
 * @author jjos
 */
public class OddsGame extends Contract {

	Timestamp lastTimestamp;
	Timestamp nextBetTimestamp;
	Transaction nextTX;
	Address developer;

	long blockOdd, pay, amount;
	Timestamp prevTimestamp;

	static final long MAX_PAYMENT = 2000*ONE_BURST;
	static final String DEV_ADDRESS = "BURST-JJQS-MMA4-GHB4-4ZNZU";

	/**
	 * This method is executed every time a transaction is received by the contract.
	 */
	@Override
	public void txReceived() {
		// Previous block hash is the random value we use
		blockOdd = getPrevBlockHash();
		prevTimestamp = getPrevBlockTimestamp();
		blockOdd &= 0xffL; // bitwise AND to get the last part of the number and avoid negative values
		blockOdd %= 2; // MOD 2 to get just 1 or 0

		// We start with the transaction after the last one (from the previous round)
		nextTX = getTxAfterTimestamp(lastTimestamp);

		while (nextTX != null) {
			nextBetTimestamp = nextTX.getTimestamp();
			if (nextBetTimestamp.ge(prevTimestamp))
				break; // only bets before previous block can run now

			lastTimestamp = nextBetTimestamp;

			pay = (lastTimestamp.getValue() % 2) - blockOdd;

			if (pay == 0) {
				// pay double (amount already has the activation fee subtracted)
				amount = nextTX.getAmount() * 2;
				if(amount > MAX_PAYMENT)
					amount = MAX_PAYMENT;
				sendAmount(amount, nextTX.getSenderAddress());
			}

			nextTX = getTxAfterTimestamp(lastTimestamp);
		}

		if(getCurrentBalance() > MAX_PAYMENT*3){
			// In the unlikely event we accumulate balance on the contract,
			// tip the developer.
			sendAmount(MAX_PAYMENT, parseAddress(DEV_ADDRESS));
		}
	}

	/**
	 * Main function for debbuging purposes only, not exported to bytecode.
	 */
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

		// another transaction to trigger the sorting mechanism
		emu.send(bet1, odds, 10*ONE_BURST);
		emu.forgeBlock();
		emu.send(bet1, odds, 20*ONE_BURST);
		emu.forgeBlock();
		emu.forgeBlock();
		emu.send(bet1, odds, 30*ONE_BURST);
		emu.forgeBlock();
		emu.forgeBlock();

		new EmulatorWindow(OddsGame.class);
	}
}
