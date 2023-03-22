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
 * The creator defines which token can be staked on this contract aka pledgeToken
 * The contract will mint 1:1 for each pledgeToken a stakingToken and send it to the address that submitted the pledgeToken.
 * 
 * Any income of Signa to this contract will be distributed to the stakingToken holders.
 * Also, another token aka yieldToken can be automated distributed to the stakingToken holders.
 * The contract can have an end-date; after a final payment nothing more is paid out.
 *
 * In the contract the following can be defined:
 * 
 * paymentInterval = Minimum blocks between distribution; 0 = no waiting
 * minAmountToDistributeSigna = Minimum Amount of Signa needed on the contract to trigger a distribution
 * maxAmountPerPayment = Maximum Amount of Signa which will be distributed when distribution is triggered
 * qualifiedMinimumQuantity = Minimum number of stakingToken needed to be eligible for the distribution
 * contractExpiryInMinutes = Staking end-time in minutes ( 0 = infinite)
 * yieldToken = Token to distribute if set
 * minQuantityToDistributeYieldToken = Minimum quantity of yieldToken nedded before distributed per paymentInterval
 * maxQuantityPerPayment = Maximum quantity of yieldToken which will be distributed per paymentInterval
 * signaRatio = Ratio for the distribution = stakingToken:1  for Signa
 * tokenRatio = Ratio for the distribution = stakingToken:1  for yieldToken 
 * If signaRatio = 0 maxAmountPerPayment is used as static value otherwise pledgeToken on contract balance will define the maximum
 * If tokenRatio = 0 maxQuantityPerPayment is used as static value otherwise pledgeToken on contract balance  will define the maximum
 * 
 * lockPeriodInMinutes = Time of lock period in minutes for every token transfer send to the contract ( 0 = no lockup period set)
 * If contractExpiryInMinutes is set, the lockPeriodTimeEnd will be at most equal to the time of contractExpiryInMinutes calculaled as datetime. 
 * 
 * @author frank_the_tank
 */
public class StakingDynamicContract extends Contract {
    // stakingToken parameter
    long stakingTokenTicker;
	long stakingTokenDecimals;
    // Decimals should be the same as from the token

    // Token to pledge
    long pledgeToken;
    long digitsFactorPledgeToken;

    // yieldToken to distribute by default
    long yieldToken;
    long digitsFactorYieldToken;
    // digit 0 = 1 ; digit 1 = 10 ... digit 8 = 100000000

    // Minimum Quantity for any other token (without digit adjustment)
    long airdroppedTokenMinimumQuantity;

    // Distribution parameter
    long paymentInterval;
    int contractExpiryInMinutes;
    long qualifiedMinimumQuantity;
    long minAmountToDistributeSigna;
    long minQuantityToDistributeYieldToken;

    // Distribution parameter for fixed payouts
    long maxAmountPerPayment; 
    long maxQuantityPerPayment;

    // Distribution parameter for dynamic payouts
    long signaRatio; 
    // Example: 100 stakedToken getting 1 SIGNA = 100:1 = 100
    long tokenRatio; 
    // Example: 1000 stakedToken getting 0.2 distribute Token = 200 : 1 = 200

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
    boolean contractActiveCheck;
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
    /**To do set the correct fee currently 0.9 Signa */
	public static final long CONTRACT_FEES = 90000000;

    public StakingDynamicContract() {
	    // constructor, runs when the first TX arrives
        stakingToken = issueAsset(stakingTokenTicker, 0L, stakingTokenDecimals);
        if ( contractExpiryInMinutes > ZERO){
            stakingTimeout= getBlockTimestamp().addMinutes(contractExpiryInMinutes);
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
		if(stakingToken == 0L || yieldToken == pledgeToken) {
			// stakingpool not initialized, do nothing
            // distributionToken = Token is not possible , never start
			return;
		}
        distributedAmount = ZERO;
        distributedQuantity = ZERO;
        distributionDone = false;
        contractActiveCheck = true;
        if(lockPeriodInMinutes > ZERO){
            lockPeriodTimeEnd = getBlockTimestamp().addMinutes(lockPeriodInMinutes);
            if(contractExpiryInMinutes > ZERO && lockPeriodTimeEnd.ge(stakingTimeout) ){
                lockPeriodTimeEnd = stakingTimeout;
            }
        }
        if(contractExpiryInMinutes > ZERO){
            if(stakingTimeout.le(getBlockTimestamp())){
                contractActiveCheck =false;
            }
        }
        while(true) {
            tx = getTxAfterTimestamp(lastProcessedTx);
            if(tx == null) {
				break;
			}
            arguments = tx.getMessage();
            lastProcessedTx = tx.getTimestamp();
            //Pledge Token check
            quantityCheck = tx.getAmount(pledgeToken);
            if(quantityCheck > ZERO){
                if(contractActiveCheck){
                    mintAsset(stakingToken,quantityCheck);
                    sendAmount(stakingToken,quantityCheck, tx.getSenderAddress());
                    totalstaked += quantityCheck;
                    if( lockPeriodInMinutes > ZERO){
                        setMapValue(FOUR,tx.getSenderAddress().getId(),lockPeriodTimeEnd.getValue());
                    }
                }
                else{
                    //Send back pledgeToken as contract ended
                    sendAmount(pledgeToken,quantityCheck, tx.getSenderAddress());
                }
            }
            //Withdrawal staking Token check
            quantityCheck = tx.getAmount(stakingToken);

            if(quantityCheck > ZERO){
                lockUpCheck=getMapValue(FOUR,tx.getSenderAddress().getId());
                if (lockUpCheck > ZERO){
                    if (getTimestamp(lockUpCheck).le(getBlockTimestamp())){
                        sendAmount(pledgeToken, quantityCheck, tx.getSenderAddress());
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
                    if (lockPeriodInMinutes == ZERO){
                        sendAmount(pledgeToken, quantityCheck, tx.getSenderAddress());
                        totalstaked -= quantityCheck;
                        // burn stakingToken
                        sendAmount(stakingToken, quantityCheck, getAddress(ZERO));
                    }
                    else{
                        //send back stakingtokens
                        sendAmount(stakingToken, quantityCheck, tx.getSenderAddress());
                        //regsiter address 
                        setMapValue(FOUR,tx.getSenderAddress().getId(),lockPeriodTimeEnd.getValue());
                    }
                }
            }
            // Distribute airdropped tokens - calls the distribution method for airdropped tokens beside pledgeToken, stakingToken or yieldToken
            if(arguments.getValue1() == DISTRIBUTE_TOKEN_BALANCE){
                // only execute if token-id is not stakingToken or Token or distributeToken
                if (arguments.getValue2() != stakingToken && arguments.getValue2() !=pledgeToken && arguments.getValue2() !=yieldToken && arguments.getValue2() != ZERO){
                    //check balance of contract - only execute if above MimimumSize
                    if (this.getCurrentBalance(arguments.getValue2())>= airdroppedTokenMinimumQuantity && this.getCurrentBalance(arguments.getValue2()) > ZERO){
                        stakingholders = getAssetHoldersCount(qualifiedMinimumQuantity, stakingToken);
                        if(tx.getAmount() >= distributionFee(stakingholders)){
                            //distribute the token
                            if(!distributionDone){
                                distributeToHolders(qualifiedMinimumQuantity,stakingToken, ZERO,arguments.getValue2(), this.getCurrentBalance(arguments.getValue2()));
                                distributionDone =true;
                            }

                        }
                    }
                }
            }
            //Cleanup - after timeout and last payment the creator can get the balance 
            if (arguments.getValue1() == CLEANUP_BY_CREATOR && stakingTimeoutLastPayment &&  tx.getSenderAddress().equals(getCreator())){
                sendAmount(yieldToken,getCurrentBalance(yieldToken),getCreator());
                sendBalance(getCreator());
            }
        }
        blockheight = this.getBlockHeight();
        if(contractExpiryInMinutes == ZERO){
            if(blockheight - lastBlockDistributed >= paymentInterval ){
                checkRatio();
                distributeToStakingToken();
            }
        }
        else if(getBlockTimestamp().le(stakingTimeout)){
            if(blockheight - lastBlockDistributed >= paymentInterval ){
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
        if (distributedAmount > ZERO && !distributionDone){
		    setMapValue(TWO, blockheight, distributedAmount);
        }
        // store distribution of distrubteToken if any
        if (distributedQuantity > ZERO && !distributionDone){
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
            maxAmountPerPayment = calcMultDiv(totalstaked, PLANCK_TO_SIGNA, digitsFactorPledgeToken) / signaRatio;
            if (maxAmountPerPayment < minAmountToDistributeSigna){
                maxAmountPerPayment = minAmountToDistributeSigna;
            }
        }
        if(tokenRatio > ZERO){
            maxQuantityPerPayment = (totalstaked/ digitsFactorPledgeToken) * digitsFactorYieldToken / tokenRatio;
            if (maxQuantityPerPayment < minQuantityToDistributeYieldToken){
                maxQuantityPerPayment = minQuantityToDistributeYieldToken;
            }
        }
    }
    private void distributeToStakingToken(){
        stakingholders = getAssetHoldersCount(qualifiedMinimumQuantity, stakingToken);
        if (stakingholders > ZERO){
            distributionFee = distributionFee(stakingholders);
            balanceCheck = this.getCurrentBalance() -distributionFee-CONTRACT_FEES ;
            if(balanceCheck  >= minAmountToDistributeSigna ){
                if (balanceCheck > maxAmountPerPayment && maxAmountPerPayment != ZERO){
                    distributedAmount = maxAmountPerPayment;
                }
                else{
                    distributedAmount = balanceCheck;               
                }
            }
            quantityCheck = this.getCurrentBalance(yieldToken);
            if ( quantityCheck >= minQuantityToDistributeYieldToken && yieldToken != ZERO ) {
                if( quantityCheck > maxQuantityPerPayment && maxQuantityPerPayment != ZERO){
                    distributedQuantity = maxQuantityPerPayment;
                }
                else{
                    distributedQuantity = quantityCheck;
                }
            }
            if(distributedAmount + distributedQuantity > ZERO){
                if(!distributionDone){
                    lastBlockDistributed =blockheight;
                    distributeToHolders(qualifiedMinimumQuantity, stakingToken, distributedAmount, yieldToken, distributedQuantity);                         
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
        contract.pledgeToken = TOKEN_ID;
		contract.yieldToken = 0;
		contract.digitsFactorPledgeToken  = 1000000;
		contract.digitsFactorYieldToken =  100000000;
		contract.airdroppedTokenMinimumQuantity = 10000;
		contract.paymentInterval = 5;
	    contract.contractExpiryInMinutes = 0;
    	contract.qualifiedMinimumQuantity = 10000000;
    	contract.minAmountToDistributeSigna = 10000000000L;
        contract.minQuantityToDistributeYieldToken = 100 ;
		contract.maxAmountPerPayment =0; 
		contract.maxQuantityPerPayment =0;
		contract.signaRatio = 100; 
		contract.tokenRatio = 200; 
        emu.forgeBlock();        

        emu.send(staker, contractAddress, Contract.ONE_SIGNA, TOKEN_ID, 1000_000, false);
        emu.forgeBlock();
        
    	new EmulatorWindow(StakingDynamicContract.class);
    }
}

