package bt.dapps;
import bt.Contract;
import bt.ui.EmulatorWindow;
import bt.Timestamp;
import bt.Transaction;
import bt.Address;
import bt.BT;

/**
 * A fully  decentralized and unstoppable lottery.
 * 
 * This contract runs nearly by the same rules as the popular Powerball lottery.
 * Users should select five numbers from 1 to 69 for the balls; then select one
 * number from 1 to 26 for the Cryptoball. The jackpot grows until it is won.
 * Players win a prize by matching one of the 9 ways to win. Activate Powerplay
 * to boost your win up to 10 times. All prizes are capped by 50% of the contract
 * balance, aka Jackpot.
 * 
 * A special random generator using 10 blocks and multiple hashes is used to
 * make any miner manipulation unprofitable. All bets placed during the hash
 * seed generation are moved to the next round.
 * 
 * @author frank, jjos
 *
 */
public class Cryptoball extends Contract {
    
    public static final long CHECK_POWERPLAY10 = 10_000_000 * ONE_BURST;
    public static final long GAS_FEE = 2 * ONE_BURST;
    public static final long POWERPLAY = 1 * ONE_BURST;
    Timestamp lastRunning;
	long nextRoundBlock;
    Address holdersContract;
    Transaction nextTx;
    Timestamp lastPaid;
    Address nextToPay;
    long betnumbers;
    // Winning Balls
    long ball1,ball2,ball3,ball4,ball5;
    long cryptoball;
    long powerplay;
    //Helper for drawing
    long hash,hash1,hashBase;
    long hashCounter;
    long roundNumber;
    long drawNumber;
     // Bet number
    long playBall1,playBall2,playBall3,playBall4,playBall5;
    long playCryptoball;
    long playMultiPlay;
    long playPowerplay;
    //Helper for Bets
    long playcheck;
    long checkPlayNumber;
    long winningAmount;
    long playWinsCryptoball;
    long prize;
    long maxPrize;
    long ballsHit;
    // Number of blocks used to get the draw numbers hash
	long HASH_BLOCKS = 9;
    //static numbers
    long ZERO = 0;
    long ONE = 1;
    long TWO = 2;
    long THREE = 3;
    long FOUR = 4;
    long FIVE = 5;
    long SIX = 6;
    long SEVEN =7;
    long EIGHT = 8;
    long NINE = 9;
    long TEN =10;
    long HUNDRED =100;
    long TEN_THOUSAND =100;
    long FIFTY_THOUSAND = 50_000;
    long ONE_MILLION = 1_000_000;
    long TWENTY_SIX =26;
    long DISTRIBUTION_ROUNDS = 7;
    long SIXTY_NINE = 69;

	// time difference between rounds in minutes (24h = 1440 minutes)
	public static final long DELTA_T = 1440;
	long DELTA_T_BLOCKS = DELTA_T/4;
    long DELTA_LAST = HASH_BLOCKS*4;
    
    static final long HASH_MASK = 0x0FFFFFFFFFFFFFFFL;

    public Cryptoball(){
        holdersContract = getCreator();
        lastRunning = getBlockTimestamp().addMinutes(DELTA_T - DELTA_LAST);
        nextRoundBlock = getBlockHeight() + DELTA_T_BLOCKS - HASH_BLOCKS;
        // Infinite loop sorting at every DELTA_T blocks
		while(true) {
			// Avoid any delay if we need more than one block to run all the bets
			sleep(nextRoundBlock - getBlockHeight());
            drawing();
            checkBets();
			nextRoundBlock = nextRoundBlock + DELTA_T_BLOCKS;
            lastRunning = lastRunning.addMinutes(DELTA_T);
            // ONE percent for holders on every week
            if(roundNumber % DISTRIBUTION_ROUNDS == ZERO) {
                //sendAmount(getCurrentBalance() * ONE / HUNDRED,TRT_Holders);
                sendAmount(getCurrentBalance() * ONE / HUNDRED, this.holdersContract);
            }
		}
    }
    
    private void drawing(){
 		// construct the winning seed based on 9 different blocks
        hash1 = getPrevBlockHash1();
        hash = hash1;
        hashCounter = ONE;
        while (hashCounter < HASH_BLOCKS){
            sleep(ONE);
            hash *= TWO; // shift by one bit
            hash += (getPrevBlockHash1() & ONE);
            hashCounter += ONE;
         }
         hash = performSHA256_64(hash, hash1);
         roundNumber += ONE;
         hashBase = hash;
         // Reset and Get all balls
         drawNumber = ZERO;
         ball1 = ZERO;
         ball2 = ZERO;
         ball3 = ZERO;
         ball4 = ZERO;
         ball5 = ZERO;
         ball1 = drawNumber();
         ball2 = drawNumber();
         ball3 = drawNumber();
         ball4 = drawNumber();
         ball5 = drawNumber();
         // Cryptoball
         hash = performSHA256_64(hash, hash);
         hash1 = hash & HASH_MASK;
         cryptoball = hash1 % TWENTY_SIX;
         cryptoball = cryptoball + ONE;
         // Powerplay
         powerplay = (hash1/TEN_THOUSAND) % FOUR;
         powerplay +=TWO;
         if(getCurrentBalance() > CHECK_POWERPLAY10 && powerplay == FIVE ){
             powerplay = TEN;
         }
        //Announce the winning seed (all numbers derived form it)
        sendMessage(hashBase, powerplay, this.holdersContract);
    }
    
	private long drawNumber(){
        while(drawNumber==ball1 || drawNumber==ball2 || drawNumber==ball3 || drawNumber==ball4 || drawNumber==ball5){
			hash = performSHA256_64(hash, hash);
			drawNumber =  hash & HASH_MASK;
			drawNumber = drawNumber % SIXTY_NINE;
            drawNumber +=ONE;
        }
		return drawNumber;
    }
	
    private void checkBets(){
        while (true) {
            nextTx = getTxAfterTimestamp(lastPaid);
			if (nextTx == null || nextTx.getTimestamp().ge(lastRunning))
				break;
            nextToPay = nextTx.getSenderAddress();
            betnumbers = nextTx.getMessage1();
            playcheck = checkPlay();
            lastPaid = nextTx.getTimestamp();
            if (playcheck == ONE){
                playWinsCryptoball =ZERO;
                if (playCryptoball == cryptoball){
                	playWinsCryptoball = ONE;
                }
                // Check the number of balls hit
			    ballsHit = ZERO;
			    checkBallHit(playBall1);
			    checkBallHit(playBall2);
			    checkBallHit(playBall3);
			    checkBallHit(playBall4);
                checkBallHit(playBall5);
                winningAmount = ZERO;
                prize = ZERO;
                if (ballsHit == FIVE){
                    prize = (ONE_MILLION + (playPowerplay * ONE_MILLION)) * ONE_BURST;
                }
                else if (ballsHit + playWinsCryptoball == FIVE){
                    winningAmount = FIFTY_THOUSAND;
                }
                else if (ballsHit + playWinsCryptoball == FOUR){
                    winningAmount = TEN_THOUSAND;
                }
                else if (ballsHit + playWinsCryptoball == THREE){
                    winningAmount = SEVEN;
                }
                else if (playWinsCryptoball == ONE){
                    winningAmount = FOUR;
                }
                if(winningAmount > ZERO && winningAmount <= FIFTY_THOUSAND){
                    prize = winningAmount * playMultiPlay;
                    prize += winningAmount * playPowerplay * (powerplay-ONE) * playMultiPlay;
                    prize = prize * ONE_BURST;
                }
                maxPrize = getCurrentBalance() / TWO;
                if (ballsHit + playWinsCryptoball == SIX){
                    prize = maxPrize;
                }
                else if (prize > maxPrize){
                	prize = maxPrize;
                }
                if(prize > ZERO){
                	sendAmount(prize, nextTx.getSenderAddress());
                }
            }
        }
    }
    
    private long checkPlay(){
        // check Powerplay
        playPowerplay = betnumbers % TEN;
        if (playPowerplay != ONE && playPowerplay != ZERO){
           return ZERO;
        }
        betnumbers = betnumbers /TEN;
        // set  Multiplay by Amount send to contract
        playMultiPlay = (nextTx.getAmount() + GAS_FEE)/( GAS_FEE + (playPowerplay * POWERPLAY));
        // check Cryptoball
        playCryptoball = betnumbers % HUNDRED;
        if (playCryptoball < ONE  && playCryptoball > TWENTY_SIX){
            return ZERO;
        }
        //Check Balls 5 to 1
        playBall5 = getPlayNumber();
        playBall4 = getPlayNumber();
        playBall3 = getPlayNumber();
        playBall2 = getPlayNumber();
        playBall1 = getPlayNumber();
        //Check all Balls 1 to 5 are unique
        if(playBall1 == playBall2 || playBall1 == playBall3 || playBall1 == playBall4 || playBall1 == playBall5
        		|| playBall2 == playBall3 || playBall2 == playBall4 || playBall2 == playBall5
        		|| playBall3 == playBall4 || playBall3 == playBall5
        		|| playBall4 == playBall5){ 
            return ZERO; 
        }
        return ONE;
    }
    
    private long getPlayNumber(){
        betnumbers = betnumbers /HUNDRED;
        checkPlayNumber = betnumbers % HUNDRED;
        return checkPlayNumber;
    }
  
    private void checkBallHit(long playerBall){
		if (playerBall == ball1 ||  playerBall == ball2 ||  playerBall == ball3 ||  playerBall == ball4 ||  playerBall == ball5){
			ballsHit += ONE;
		}
	}
    
    public void txReceived(){
    	// Not needed, since the contract never leaves the loop on constructor.
    }

    public static void main(String[] args) {
    	BT.activateCIP20(true);
    	new EmulatorWindow(Cryptoball.class);
    }
}
