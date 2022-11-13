package bt.dapps;

import bt.Contract;
import bt.Register;
import bt.Timestamp;
import bt.Transaction;

/**
 * A staking contract for the Signum Blockchain
 * 
 * The creator defines which token can be staked on this contract
 * The contract will mint 1:1 for each token a stakingToken
 * And send to the address which send the token.
 * Any income of Signa to this contract will be distributed to the stakingToken holders.
 * Also another token can be automated distributed to the holders.
 * The contract can have an end-date; after a final payment nothing more is paid out.
 *
 * In the contract the following can be defined:
 * dthinterval = Minimum blocks between distribution; 0 = no waiting
 * dthMinimumAmount = Minimum Amount of Signa needed on the contract to trigger a distribution
 * dthMaximumAmount = Maximum Amount of Signa which will be distributed when distribution is triggered
 * dthMinimumQuantity = Minimum number of stakingToken needed to be eligible for the distribution
 * timeout = Staking end-time in minutes ( 0 = infinite)
 * distributeToken = Token to distribute by default
 * distributionTokenMinAmount = Minimum quantity nedded before distributed
 * distributionTokenMaxAmount = Maximum quantity distributed 
 * 
 * @author frank_the_tank
 */
public class StakingContract extends Contract {
    //stakingToken 
    long name;
	long decimalPlaces;

    //Token for staking
    long token;

    //Token to distribute by default
    long distributeToken;
    long distributionTokenMinAmount;
    long distributionTokenMaxAmount;

    //Distribution parameter
    long dthMinimumAmount;
    long dthMaximumAmount; 
    long dthinterval;
    long dthMinimumQuantity; 
    long MinimumTokenXY;
    int timeout;
    Timestamp stakingTimeout;    
    boolean stakingTimeoutLastPayment;
    
    long stakingToken;

    // temporary variables
    Timestamp lastProcessedTx;
    Register arguments;
    long staked;
    long totalstaked = 0;
    long stakingholders;
    long distributedAmount;
    long distributedQuantity;
	Transaction tx;
    long lastBlockDistributed=0;
    long checkPlanckForDistribution;

    public static final long ZERO = 0;
    public static final long ONE = 1;
    public static final long TWO = 2;
    public static final long DISTRIBUTE_TOKEN_BALANCE = 1;
    public static final long DISTRIBUTION_FEE_PER_HOLDER = 1000000;
    public static final long DISTRIBUTION_FEE_MINIMUM_HOLDER = 10000000;
    public static final long DISTRIBUTION_FEE_MINIMUM = 20000000;
	/** Use a contract fee of XX SIGNA */
    /**To do set the correct fee currently 1.2 Signa */
	public static final long CONTRACT_FEES = 120000000;

    public StakingContract() {
	    // constructor, runs when the first TX arrives
	    stakingToken = issueAsset(name, 0L, decimalPlaces);
        if(timeout > ZERO){
            stakingTimeout= getBlockTimestamp().addMinutes(timeout);
        }
    }

    @Override
    protected void blockStarted() {
		if(stakingToken == 0L && distributeToken == token) {
			// stakingpool not initialized, do nothing
            // distributionToken = Token is not possible , never start
			return;
		}
        staked = ZERO;
        distributedAmount = ZERO;
        distributedQuantity = ZERO;
        while(true) {
            tx = getTxAfterTimestamp(lastProcessedTx);
            if(tx == null) {
				break;
			}
            arguments = tx.getMessage();
            lastProcessedTx = tx.getTimestamp();
            //User is adding Token
            if(tx.getAmount(token) > ZERO){
                mintAsset(stakingToken,tx.getAmount(token));
                sendAmount(stakingToken, tx.getAmount(token), tx.getSenderAddress());
                staked += tx.getAmount(token);
                totalstaked += tx.getAmount(token);
                stakingholders += ONE;
            }
            //User removes stakingToken
            if(tx.getAmount(stakingToken) > ZERO){
                sendAmount(token, tx.getAmount(stakingToken), tx.getSenderAddress());
                staked -= tx.getAmount(stakingToken);
                totalstaked -= tx.getAmount(stakingToken);
                stakingholders -= ONE;
                if (totalstaked == ZERO || stakingholders < ZERO){
                    stakingholders = ZERO;
                }
                // burn stakingToken
		        sendAmount(stakingToken, tx.getAmount(stakingToken), getAddress(ZERO));
            }
            // User calls the distribution method for any token beside Token, StakingToken or distrubteToken
            if  (arguments.getValue1() == DISTRIBUTE_TOKEN_BALANCE){
                // only execute if token-id is not stakingToken or Token or distributeToken
                if (arguments.getValue2() != stakingToken && arguments.getValue1() !=token && arguments.getValue1() !=distributeToken){
                    //check ballance of contract - only execute if above MimimumSize
                    if (this.getCurrentBalance(arguments.getValue2())>= MinimumTokenXY){
                        if(getCurrentTxAmount() >= DistributionFee()){
                            //distribute the token
                            distributeToHolders(stakingToken, dthMinimumQuantity, ZERO,arguments.getValue2(), getCurrentBalance(arguments.getValue2()));
                        }
                    }
                }
            }
        }
        //Check interval/minAmount and distribute Signa
        if(timeout == ZERO){
            if(this.getBlockHeight() - lastBlockDistributed >= dthinterval ){
                DistributeToStakingToken();
            }
        }
        if(getBlockTimestamp().ge(stakingTimeout)){
            if(this.getBlockHeight() - lastBlockDistributed >= dthinterval ){
                DistributeToStakingToken();
            }
        }
        else if(stakingTimeoutLastPayment == false){
            DistributeToStakingToken();
            // last payment is done - no payment anymore
            stakingTimeoutLastPayment = true;

        }

        // Adding new stake / remove stake to maps also store the stake delta if any
        if (staked != ZERO){
            setMapValue(ZERO, this.getBlockHeight(), staked);
            setMapValue(ONE, this.getBlockHeight(), totalstaked);
        }
        // store the distribution of SIGNA if any
        if (distributedAmount > ZERO){
		    setMapValue(TWO, this.getBlockHeight(), distributedAmount);
        }
        // store distribution of distrubteToken if any
        if (distributedQuantity > ZERO){
            setMapValue(distributeToken, this.getBlockHeight(), distributedQuantity);
        }
    }
    private long DistributionFee(){
        checkPlanckForDistribution = (stakingholders * DISTRIBUTION_FEE_PER_HOLDER)+ DISTRIBUTION_FEE_MINIMUM_HOLDER;
        if(checkPlanckForDistribution < DISTRIBUTION_FEE_MINIMUM){
            checkPlanckForDistribution = DISTRIBUTION_FEE_MINIMUM;
        }
        return (checkPlanckForDistribution);
    }

    private void DistributeToStakingToken(){
        if(this.getCurrentBalance()-DistributionFee()-CONTRACT_FEES  >= ZERO){
            if(this.getCurrentBalance()-DistributionFee()-CONTRACT_FEES  >= dthMinimumAmount){
                if (this.getCurrentBalance()-DistributionFee()-CONTRACT_FEES > dthMaximumAmount && dthMaximumAmount != ZERO){
                    distributedAmount = dthMaximumAmount;
                }
                else{
                    distributedAmount = this.getCurrentBalance()-DistributionFee()-CONTRACT_FEES;

            }
            lastBlockDistributed = this.getBlockHeight();
            if (this.getCurrentBalance(distributeToken)>= distributionTokenMinAmount && distributeToken != ZERO ) {
                if( this.getCurrentBalance(distributeToken)> distributionTokenMaxAmount && distributionTokenMaxAmount != ZERO){
                    distributedQuantity = distributionTokenMaxAmount;
                    distributeToHolders(stakingToken, dthMinimumQuantity, distributedAmount, distributeToken, distributionTokenMaxAmount);
                }
                else{
                    distributedQuantity =  this.getCurrentBalance(distributeToken);
                    distributeToHolders(stakingToken, dthMinimumQuantity, distributedAmount, distributeToken, this.getCurrentBalance(distributeToken));
                }
            }
            else{
                distributeToHolders(stakingToken, dthMinimumQuantity, distributedAmount, ZERO, ZERO);
            }
            }
        }
    }

    @Override
    public void txReceived() {
      // do nothing, since we are using a loop on blockStarted over all transactions
    }
}
