package bt.dapps;

import bt.Contract;
import bt.Register;
import bt.Timestamp;
import bt.Transaction;

/**
 * A staking contract for the Signum Blockchain
 * 
 * The creator can define which token can be staked on this contract
 * Any income of Signa to this contract will be distributed to the stakingToken holders.
 * In the contract the following can be defined:
 * dthinterval = Minimum blocks between distribution; 0 = no waiting
 * dthMinimumAmount = Minimum Amount of Signa needed on the contract to trigger a distribution
 * dthMaximumAmount = Maximum Amount of Signa which will be distributed when distribution is triggered
 * dthMinimumQuantity = Minimum number of stakingToken needed to be eligible for the distribution
 * 
 * @author frank_the_tank
 */
public class StakingContract extends Contract {
    //stakingToken 
    long name;
	long decimalPlaces;

    //Token for staking
    long token;

    //Distribution parameter
    long dthMinimumAmount;
    long dthMaximumAmount; 
    long dthinterval;
    long dthMinimumQuantity; 
    long MinimumTokenXY;
    
    long stakingToken;

    // temporary variables
    Timestamp lastProcessedTx;
    Register arguments;
    long staked;
    long totalstaked = 0;
    long distributedAmount;
	Transaction tx;
    long lastBlockDistributed=0;

    public static final long ZERO = 0;
    public static final long ONE = 1;
    public static final long TWO = 2;
    public static final long DISTRIBUTE_TOKEN_BALANCE = 1;

    public StakingContract() {
	    // constructor, runs when the first TX arrives
	    stakingToken = issueAsset(name, 0L, decimalPlaces);
    }

    @Override
    protected void blockStarted() {
		if(stakingToken == 0L) {
			// stakingpool not initialized, do nothing
			return;
		}
        staked = ZERO;
        distributedAmount = ZERO;
        while(true) {
            tx = getTxAfterTimestamp(lastProcessedTx);
            arguments = tx.getMessage();
            if(tx == null) {
				break;
			}
            lastProcessedTx = tx.getTimestamp();
            //User is adding Token
            if(tx.getAmount(token) > ZERO){
                mintAsset(stakingToken,tx.getAmount(token));
                sendAmount(stakingToken, tx.getAmount(token), tx.getSenderAddress());
                staked += tx.getAmount(token);
                totalstaked += tx.getAmount(token);
            }
            //User removes stakingToken
            if(tx.getAmount(stakingToken) > ZERO){
                sendAmount(token, tx.getAmount(stakingToken), tx.getSenderAddress());
                staked -= tx.getAmount(token);
                totalstaked -= tx.getAmount(token);
                // burn stakingToken
		        sendAmount(stakingToken, tx.getAmount(stakingToken), getAddress(ZERO));
            }
            // User calls the distribution method for any token beside Token, StakingToken
            if  (arguments.getValue1() == DISTRIBUTE_TOKEN_BALANCE){
                // only execute if token-id is not stakingToken or Token
                if (arguments.getValue2() != stakingToken && arguments.getValue1() !=token){
                //check ballance of contract - only execute if above MimimumSize
                    if (this.getCurrentBalance(arguments.getValue2())>= MinimumTokenXY){
                        //distribute the token
                        distributeToHolders(stakingToken, dthMinimumQuantity, ZERO,arguments.getValue2(), getCurrentBalance(arguments.getValue2()));
                    }
                }
            }
        }
        //Check interval/minAmount and distribute Signa
        if( this.getBlockHeight() - lastBlockDistributed >= dthinterval){
            if(this.getCurrentBalance() >= dthMinimumAmount){
                lastBlockDistributed = this.getBlockHeight();
                 if (this.getCurrentBalance() > dthMaximumAmount && dthMaximumAmount != ZERO){
                    distributedAmount = dthMaximumAmount;
                    distributeToHolders(stakingToken, dthMinimumQuantity, distributedAmount, ZERO, ZERO);
                }
                else{
                    distributedAmount = this.getCurrentBalance();
                    distributeToHolders(stakingToken, dthMinimumQuantity, distributedAmount, ZERO, ZERO);
                }
            }
         }
        //Adding new stake / remove stake to maps also 
        // store the stake delta if any
        if (staked != ZERO){
            setMapValue(ZERO, this.getBlockHeight(), staked);
            setMapValue(ONE, this.getBlockHeight(), totalstaked);
        }
        // store the distribution if any
        if (distributedAmount > ZERO){
		    setMapValue(TWO, this.getBlockHeight(), distributedAmount);
        }
    }

    @Override
    public void txReceived() {
      // do nothing, since we are using a loop on blockStarted over all transactions
    }
}
