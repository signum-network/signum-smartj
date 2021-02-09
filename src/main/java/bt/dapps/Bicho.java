package bt.sample;

import bt.Contract;
import bt.Emulator;
import bt.Register;
import bt.ui.EmulatorWindow;
import bt.Timestamp;
import bt.Transaction;
import bt.Address;
import bt.BT;

/**
 * "Animal Game" smart contract ("Jogo do Bicho") with Group, Tens, Hundreds,
 * and Thousands as possible bets.
 * 
 * The bet type is actually given by the message attached to every transaction
 * received. These messages are encoded on a numeric variable of type 'long'.
 * The bet type is identified by bit flags:
 *  - Group:    0x10000000
 *  - Tens:     0x20000000
 *  - Hundreds: 0x40000000
 *  - Thousands:0x80000000
 * 
 * Group bet (Bicho): choose a number from 1 to 25, pays 18 times the bet value.
 * Valid messages would be: 0x1000001, 0x1000002, ..., 0x1000019.
 * 
 * Tens (Dezena) bet: choose a number from 0 to 99, pays 70 times the bet value.
 * Valid messages would be: 0x20000000, 0x20000001, 0x20000003, ..., 0x20000063.
 * 
 * Hundreds (Centena): choose a number from 0 to 999, pays 700 times the bet
 * value. Valid messages would be: 0x40000000, 0x40000001, ..., 0x400003e7.
 * 
 * Thousands (Milhar): choose a number from 0 to 9999, pays 7000 times the bet
 * value. Valid messages would be: 0x80000000, 0x80000001, ..., 0x80000270f.
 * 
 * The sorting process takes place every 6 hours. All bets up to 40 minutes
 * before the sorting process are considered, otherwise they run on
 * the next round.
 * 
 * Minimum bet amount is 2 BURST and invalid bets, not following the above convention,
 * are ignored.
 * 
 * Also check for instance https://en.wikipedia.org/wiki/Jogo_do_Bicho
 * 
 * @author jjos
 */
public class Bicho extends Contract {
  
	long roundNumber;
	long betsProcessed;
	long prize1;

	Timestamp lastRunning;
	long nextRoundBlock;

	long thousands;
	long hundreds;
	long tens;
	long group;

	Timestamp lastPaid;
	Address nextToPay;

	long hash;
	long hash1;
	long hashCounter;

	Transaction nextTx;
	Register msg;
	Address paymentAddress;
	long bet;
	long betNum;
	long Tmp;
	long prizeFactor;
	long maxPrize;
	long amount;
	
	Address holdersContract;

	long ZERO = 0;
	long ONE = 1;
	long TWO = 2;
	long THREE = 3;
	long FOUR = 4;
	long TEN = 10;
	long TWENTY = 20;
	long TWENTY_FIVE = 25;
	long HUNDRED = 100;
	long THOUSAND = 1_000;
	long TEN_THOUSAND = 10_000;
	long HUND_THOUSAND = 100_000;
	
	long PRIZE_GROUP = 18;
	long PRIZE_TENS = 70;
	long PRIZE_HUNDS = 700;
	long PRIZE_THOUS = 7000;

	long BET_GROUP = 0x10000000L;
	long BET_TENS  = 0x20000000L;
	long BET_HUNDS = 0x40000000L;
	long BET_THOUS = 0x80000000L;
	long BET_MASK =  0x0fffffffL;

	static final long HASH_MASK = 0x0FFFFFFFFFFFFFFFL;

	// The minimum bet, values smaller than this are ignored
	public static final long ACTIVATION_FEE = 2 * Contract.ONE_BURST;

	// Number of blocks used to get the draw numbers hash
	long HASH_BLOCKS = 9;

	// time difference between rounds in minutes (6h = 360 minutes)
	public static final long DELTA_T = 360;
	long DELTA_T_BLOCKS = DELTA_T/4;
	long DELTA_LAST = HASH_BLOCKS*4;
	
	// Number of rounds to distribute a fraction of the balance
	long DISTRIBUTION_ROUNDS = 28; // 7*24*60/DELTA_T;

	// Initialization, cost is less than 1 BURST
	public Bicho() {
		// Set up the last running deadline
		lastRunning = getBlockTimestamp().addMinutes(DELTA_T - DELTA_LAST);
		holdersContract = getCreator();
		
		nextRoundBlock = getBlockHeight() + DELTA_T_BLOCKS - HASH_BLOCKS;

		// Infinite loop sorting at every DELTA_T blocks
		while(true) {
			// Avoid any delay if we need more than one block to run all the bets
			sleep(nextRoundBlock - getBlockHeight());
			roundRun();
			
			// update the deadline
			nextRoundBlock = nextRoundBlock + DELTA_T_BLOCKS;
			lastRunning = lastRunning.addMinutes(DELTA_T);
		}
	}

	/**
	 * Function for running the next round, costs around 0.25 BURST per bet
	 */
	private void roundRun() {		
		// construct the winning number based on 9 different blocks
		hash1 = getPrevBlockHash1();
		hash = hash1;
		hashCounter = ONE;
		// now one additional bit each block
		while (hashCounter < HASH_BLOCKS) {
			sleep(ONE);
			
			hash *= TWO; // shift by one bit
			hash += (getPrevBlockHash1() & ONE);
			hashCounter += ONE;
		}
		hash = performSHA256_64(hash, hash1);
		roundNumber += ONE;

		prize1 = hash & HASH_MASK;
		// Announce the winner number for this round
		sendMessage(roundNumber, prize1, this.holdersContract);
		prize1 = prize1 % HUND_THOUSAND;
		thousands = prize1 % TEN_THOUSAND;
		hundreds = prize1 % THOUSAND;
		tens = prize1 % HUNDRED;
		group = (tens + THREE) / FOUR;
		if (group == ZERO) // Number 00 belongs to group 25 (cow)
			group = TWENTY_FIVE;

		// iterate the bets
		while (true) {
			nextTx = getTxAfterTimestamp(lastPaid);
			if (nextTx == null || nextTx.getTimestamp().ge(lastRunning))
				break;

			nextToPay = nextTx.getSenderAddress();

			msg = nextTx.getMessage();
			bet = msg.getValue1();
			betNum = bet & BET_MASK;
			
			if(msg.getValue2() != ZERO) {
				// We have an affiliate address, getting 5% of the bet amount
				sendAmount((nextTx.getAmount()+ACTIVATION_FEE)/TWENTY, getAddress(msg.getValue2()));
			}

			prizeFactor = ZERO;
			if ((bet & BET_GROUP) == BET_GROUP) {
				// group
				Tmp = betNum - group;
				prizeFactor = PRIZE_GROUP;
			} else if ((bet & BET_TENS) == BET_TENS) {
				// tens
				Tmp = betNum - tens;
				prizeFactor = PRIZE_TENS;
			} else if ((bet & BET_HUNDS) == BET_HUNDS) {
				// hundreds
				Tmp = betNum - hundreds;
				prizeFactor = PRIZE_HUNDS;
			} else if ((bet & BET_THOUS) == BET_THOUS) {
				// thousands
				Tmp = betNum - thousands;
				prizeFactor = PRIZE_THOUS;
			}

			if (prizeFactor > ZERO && Tmp == ZERO) {
				// pay the prize
				amount = nextTx.getAmount() + ACTIVATION_FEE;
				amount *= prizeFactor;
				
				// limit to a max prize
				maxPrize = getCurrentBalance() / TWO;
				if(amount > maxPrize) {
					amount = maxPrize;
				}

				paymentAddress = nextTx.getSenderAddress();
				if(msg.getValue3() != ZERO) {
					// We have a different payment address
					paymentAddress = getAddress(msg.getValue3());
				}
				sendAmount(amount, paymentAddress);
			}

			lastPaid = nextTx.getTimestamp();
			betsProcessed += ONE;
		}

		// Two percent for holders on every week
		if(roundNumber % DISTRIBUTION_ROUNDS == 0) {
			sendAmount(getCurrentBalance() * TWO / HUNDRED, holdersContract);
		}
	}

	@Override
	public void txReceived() {
		// we do nothing, as it never leaves the constructor method infinite loop
	}

	public static void main(String[] args) throws Exception {
		BT.activateCIP20(true);
		BT.setNodeAddress(BT.NODE_TESTNET);
		
		Emulator emu = Emulator.getInstance();
		
		Address creator = emu.getAddress("CREATOR");
		emu.airDrop(creator, 1000*Contract.ONE_BURST);
		Address player = Emulator.getInstance().getAddress("PLAYER");
		Address bicho = Emulator.getInstance().getAddress("BICHO");
		emu.createConctract(creator, bicho, Bicho.class, Contract.ONE_BURST);
		emu.forgeBlock();
		
		emu.send(player, bicho, Contract.ONE_BURST*10);
		emu.forgeBlock();

		new EmulatorWindow(Bicho.class);
	}
}
