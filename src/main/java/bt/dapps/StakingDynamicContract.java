package bt.dapps;
import bt.Address;
import bt.BT;
import bt.Contract;
import bt.Emulator;
import bt.Register;
import bt.Timestamp;
import bt.Transaction;
import bt.ui.EmulatorWindow;

/**
 * A staking contract for the Signum Blockchain
 * 
 * The creator defines which token can be staked on this contract
 * The contract will mint 1:1 for each token a stakingToken
 * And send to the address which send the token in.
 * 
 * Any income of Signa to this contract will be distributed to the stakingToken holders.
 * Also another token can be automated distributed to the holders.
 * The contract can have an end-date; after a final payment nothing more is paid out.
 *
 * In the contract the following can be defined:
 * 
 * dtninterval = Minimum blocks between distribution; 0 = no waiting
 * dthMinimumAmount = Minimum Amount of Signa needed on the contract to trigger a distribution
 * dtnMaximumAmount = Maximum Amount of Signa which will be distributed when distribution is triggered
 * dtnMinimumQuantity = Minimum number of stakingToken needed to be eligible for the distribution
 * timeout = Staking end-time in minutes ( 0 = infinite)
 * distributeToken = Token to distribute by default
 * dtnTokenMinQuantity = Minimum quantity nedded before distributed
 * dtnTokenMaxQuantity = Maximum quantity distributed 
 * signaRatio = Ratio for the distribution stakingToken:1 Signa
 * tokenRatio = Ratio for the distribution stakingRoken:1 distributeToken 
 * If signaRatio = 0 dtnMaximumAmount is used as static value
 * If tokenRatio = 0 dtnTokenMaxQuantity is used as static value
 * 
 * @author frank_the_tank
 */
public class StakingDynamicContract extends Contract {
    // stakingToken parameter
    long name;
	long decimalPlaces;
    // Decimals should be the same as from the token

    // Token for staking
    long token;
    long digitsFactorToken;

    // Token to distribute by default
    long distributeToken;
    long digitsFactorDisToken;
    // digit 0 = 1 ; digit 1 = 10 ... digit 8 = 100000000

    // Minimum Quantity for any other token (without digit adjustment)
    long MinimumTokenXY;

    // Distribution parameter
    long dtninterval;
    int timeout;
    long dtnMinimumQuantity;
    long dtnMinimumAmount;
    long dtnTokenMinQuantity;

    // Distribution parameter for fixed payouts
    long dtnMaximumAmount; 
    long dtnTokenMaxQuantity;

    // Distribution parameter for dynamic payouts
    long signaRatio; 
    // Example: 100 stakedToken getting 1 SIGNA = 100:1 = 100
    long tokenRatio; 
    // Exampüle: 1000 stakedToken getting 0.2 distribute Token = 200 : 1 = 200

     // lockPeriod - If set user can´t convert stakingToken into token
    int lockPeriodInMinutes;
    Timestamp lockPeriodTimeEnd;

   // stakingToken created by contract
    long stakingToken;

    // temporary variables
    Timestamp stakingTimeout; 
    boolean stakingTimeoutLastPayment;
    Timestamp lastProcessedTx;
    Register arguments;
    long totalstaked ;
    long stakingholders;
    long distributedAmount;
    long distributedQuantity;
	Transaction tx;
    long lastBlockDistributed;
    long checkPlanckForDistribution;
    long distributionFee;
    long blockheight;
    long quantityCheck;
    long balanceCheck;
    long lockUpCheck;
    boolean distributionDone;

    

    public static final long ZERO = 0;
    public static final long ONE = 1;
    public static final long TWO = 2;
    public static final long THREE = 3;
    public static final long FOUR = 4;
    public static final long DISTRIBUTE_TOKEN_BALANCE = 99;
    public static final long CLEANUP_BY_CREATOR =100 ;    
    public static final long DISTRIBUTION_FEE_PER_HOLDER = 100000;
    public static final long DISTRIBUTION_FEE_MINIMUM_HOLDER = 1000000;
    public static final long DISTRIBUTION_FEE_MINIMUM = 2000000;
    public static final long PLANCK_TO_SIGNA = 100000000;
    /** Use a contract fee of XX SIGNA */
    /**To do set the correct fee currently 1.2 Signa */
	public static final long CONTRACT_FEES = 70000000;

    public StakingDynamicContract() {
	    // constructor, runs when the first TX arrives
        stakingToken = issueAsset(name, 0L, decimalPlaces);
        if ( timeout > ZERO){
            stakingTimeout= getBlockTimestamp().addMinutes(timeout);
        }
        if(lastBlockDistributed == ZERO){
            lastBlockDistributed = this.getBlockHeight();
        }
        if (lockPeriodInMinutes > ZERO){
            lockPeriodTimeEnd = getBlockTimestamp().addMinutes(lockPeriodInMinutes);
        }
    }

    @Override
    protected void blockStarted() {
		if(stakingToken == 0L || distributeToken == token) {
			// stakingpool not initialized, do nothing
            // distributionToken = Token is not possible , never start
			return;
		}
        distributedAmount = ZERO;
        distributedQuantity = ZERO;
        distributionDone = false;
        if(lockPeriodInMinutes > ZERO){
            lockPeriodTimeEnd = getBlockTimestamp().addMinutes(lockPeriodInMinutes);
            if(timeout > ZERO && lockPeriodTimeEnd.ge(stakingTimeout) ){
                lockPeriodTimeEnd = stakingTimeout;
            }
        }
        while(true) {
            tx = getTxAfterTimestamp(lastProcessedTx);
            if(tx == null) {
				break;
			}
            arguments = tx.getMessage();
            lastProcessedTx = tx.getTimestamp();
            //User is adding Token
            quantityCheck = tx.getAmount(token);
            if(quantityCheck > ZERO){
                mintAsset(stakingToken,quantityCheck);
                sendAmount(stakingToken,quantityCheck, tx.getSenderAddress());
                totalstaked += quantityCheck;
                if( lockPeriodInMinutes > ZERO){
                    setMapValue(FOUR,tx.getSenderAddress().getId(),lockPeriodTimeEnd.getValue());
                }

            }
            //User removes stakingToken
            quantityCheck = tx.getAmount(stakingToken);

            if(quantityCheck > ZERO){
                lockUpCheck=getMapValue(FOUR,tx.getSenderAddress().getId());
                if (lockUpCheck > ZERO){
                    if (getTimestamp(lockUpCheck).le(getBlockTimestamp())){
                        sendAmount(token, quantityCheck, tx.getSenderAddress());
                        totalstaked -= quantityCheck;
                        // burn stakingToken
                        sendAmount(stakingToken, quantityCheck, getAddress(ZERO));
                    }
                    else{
                        //send back stakingtokens - lockup period not reached
                        sendAmount(stakingToken, quantityCheck, tx.getSenderAddress());
                    }
                }
                else{
                    //send back stakingtokens
                    sendAmount(stakingToken, quantityCheck, tx.getSenderAddress());
                    //regsiter address 
                    setMapValue(FOUR,tx.getSenderAddress().getId(),lockPeriodTimeEnd.getValue());
                }
            }
            // User calls the distribution method for any token beside Token, StakingToken or distrubteToken
            if(arguments.getValue1() == DISTRIBUTE_TOKEN_BALANCE){
                // only execute if token-id is not stakingToken or Token or distributeToken
                if (arguments.getValue2() != stakingToken && arguments.getValue2() !=token && arguments.getValue2() !=distributeToken && arguments.getValue2() != ZERO){
                    //check balance of contract - only execute if above MimimumSize
                    if (this.getCurrentBalance(arguments.getValue2())>= MinimumTokenXY && this.getCurrentBalance(arguments.getValue2()) > ZERO){
                        stakingholders = getAssetHoldersCount(dtnMinimumQuantity, stakingToken);
                        if(tx.getAmount() >= distributionFee(stakingholders)){
                            //distribute the token
                            if(!distributionDone){
                                distributeToHolders(dtnMinimumQuantity,stakingToken, ZERO,arguments.getValue2(), this.getCurrentBalance(arguments.getValue2()));
                                distributionDone =true;
                            }

                        }
                    }
                }
            }
            // after timeout and last payment the creator can get the balance 
            if (arguments.getValue1() == CLEANUP_BY_CREATOR && stakingTimeoutLastPayment &&  tx.getSenderAddress().equals(getCreator())){
                sendAmount(distributeToken,getCurrentBalance(distributeToken),getCreator());
                sendBalance(getCreator());
            }
        }
        blockheight = this.getBlockHeight();
        if(timeout == ZERO){
            if(blockheight - lastBlockDistributed >= dtninterval ){
                checkRatio();
                distributeToStakingToken();
            }
        }
        else if(getBlockTimestamp().le(stakingTimeout)){
            if(blockheight - lastBlockDistributed >= dtninterval ){
                checkRatio();
                distributeToStakingToken();
            }
        }
        else if(!stakingTimeoutLastPayment){
            checkRatio();
            distributeToStakingToken();
            // last payment is done - no payment anymore
            stakingTimeoutLastPayment = true;
        }

        // Storing current total staking
        setMapValue(ONE, blockheight, totalstaked);
        // store the distribution of SIGNA if any
        if (distributedAmount > ZERO){
		    setMapValue(TWO, blockheight, distributedAmount);
        }
        // store distribution of distrubteToken if any
        if (distributedQuantity > ZERO){
            setMapValue(THREE, blockheight, distributedQuantity);
        }
    }
    private long distributionFee(long stakingholders){
        checkPlanckForDistribution = (stakingholders * DISTRIBUTION_FEE_PER_HOLDER)+ DISTRIBUTION_FEE_MINIMUM_HOLDER;
        if(checkPlanckForDistribution < DISTRIBUTION_FEE_MINIMUM){
            checkPlanckForDistribution = DISTRIBUTION_FEE_MINIMUM;
        }
        return (checkPlanckForDistribution);
    }

    private void checkRatio(){
        if(signaRatio > ZERO){
            dtnMaximumAmount = calcMultDiv(totalstaked, PLANCK_TO_SIGNA, digitsFactorToken) / signaRatio;
            if (dtnMaximumAmount < dtnMinimumAmount){
                dtnMaximumAmount = dtnMinimumAmount;
            }
        }
        if(tokenRatio > ZERO){
            dtnTokenMaxQuantity = (totalstaked/ digitsFactorToken) * digitsFactorDisToken / tokenRatio;
            if (dtnTokenMaxQuantity < dtnTokenMinQuantity){
                dtnTokenMaxQuantity = dtnTokenMinQuantity;
            }
        }
    }
    private void distributeToStakingToken(){
        stakingholders = getAssetHoldersCount(dtnMinimumQuantity, stakingToken);
        if (stakingholders > ZERO){
            distributionFee = distributionFee(stakingholders);
            balanceCheck = this.getCurrentBalance() -distributionFee-CONTRACT_FEES ;
            if(balanceCheck  >= dtnMinimumAmount ){
                if (balanceCheck > dtnMaximumAmount && dtnMaximumAmount != ZERO){
                    distributedAmount = dtnMaximumAmount;
                }
                else{
                    distributedAmount = balanceCheck;               
                }
            }
            quantityCheck = this.getCurrentBalance(distributeToken);
            if ( quantityCheck >= dtnTokenMinQuantity && distributeToken != ZERO ) {
                if( quantityCheck > dtnTokenMaxQuantity && dtnTokenMaxQuantity != ZERO){
                    distributedQuantity = dtnTokenMaxQuantity;
                }
                else{
                    distributedQuantity = quantityCheck;
                }
            }
            if(distributedAmount + distributedQuantity > ZERO){
                if(!distributionDone){
                    lastBlockDistributed =blockheight;
                    distributeToHolders( dtnMinimumQuantity, stakingToken, distributedAmount, distributeToken, distributedQuantity);                         
                }
            }   
        }
    }

    @Override
    public void txReceived() {
      // do nothing, since we are using a loop on blockStarted over all transactions
    }
    
    public static void main(String[] args) throws Exception {
        BT.activateSIP37(true);
        
        Emulator emu = Emulator.getInstance();

        Address creator = emu.getAddress("CREATOR");
        emu.airDrop(creator, 10000*Contract.ONE_SIGNA);

        Address staker = emu.getAddress("STAKER");
        emu.airDrop(staker, 1000*Contract.ONE_SIGNA);

        long TOKEN_ID = emu.issueAsset(creator, 11111, 0, 4);
        emu.mintAsset(creator, TOKEN_ID, 2000_0000);
        
        emu.send(creator, staker, 0, TOKEN_ID, 1000_0000, false);
        emu.forgeBlock();
        
        Address contractAddress = Emulator.getInstance().getAddress("CONTRACT");
        emu.createConctract(creator, contractAddress, StakingDynamicContract.class, Contract.ONE_SIGNA);
        emu.forgeBlock();
        StakingDynamicContract contract = (StakingDynamicContract)contractAddress.getContract();
        contract.token = TOKEN_ID;
		contract.distributeToken = 0;
		contract.digitsFactorToken  = 1000000;
		contract.digitsFactorDisToken =  100000000;
		contract.MinimumTokenXY = 10000;
		contract.dtninterval = 5;
	    contract.timeout = 0;
    	contract.dtnMinimumQuantity = 10000000;
    	contract.dtnMinimumAmount = 10000000000L;
        contract.dtnTokenMinQuantity = 100 ;
		contract.dtnMaximumAmount =0; 
		contract.dtnTokenMaxQuantity =0;
		contract.signaRatio = 100; 
		contract.tokenRatio = 200; 
        emu.forgeBlock();        

        emu.send(staker, contractAddress, Contract.ONE_SIGNA, TOKEN_ID, 1000_000, false);
        emu.forgeBlock();
        
    	new EmulatorWindow(StakingDynamicContract.class);
    }
}

