package bt.dapps;

import bt.Contract;
import bt.Register;
import bt.Timestamp;
import bt.Transaction;

/**
 * 
 * @author frank_the_tank
 */
public class StakingContract extends Contract {
    //stakingToken 
    long name;
	long decimalPlaces;
	long stakingToken;

    //Token for staking
    long token;

    //Distribution parameter
    long dthMinimumAmount;
    long dthMaximumAmount; 
    long dthinterval;
    long dthMinimumQuantity; 
    long MinimumTokenXY;

    // temporary variables
    Timestamp lastProcessedTx;
    Register arguments;
    long staked;
    long distributedAmount;
	Transaction tx;
    long lastBlockDistributed=0;
    public static final long ZERO = 0;
    public static final long ONE = 1;
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
            }
            //User removes stakingToken
            if(tx.getAmount(stakingToken) > ZERO){
                sendAmount(token, tx.getAmount(stakingToken), tx.getSenderAddress());
                staked -= tx.getAmount(token);
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
                        // no clue how to call the function for real
                        // params : tokenToDistribut;MinTokenSize,stakingToken
                      

                      distributeToHolders(stakingToken, dthMinimumQuantity, ZERO,
                          arguments.getValue2(), getCurrentBalance(arguments.getValue2()));
                    }
                }
            }

        }
        //Check distribution and distribute - no clue how to call the function
        // Min Intervall needs to be checked before paying (in blocks)
        // hope parameter are clear
        if( this.getBlockHeight() - lastBlockDistributed >= dthinterval){
            if(this.getCurrentBalance() >= dthMinimumAmount){
                if (this.getCurrentBalance() > dthMaximumAmount){
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
        }
        // stroe the distribution if any
        if (distributedAmount >= ZERO){
		    setMapValue(ONE, this.getBlockHeight(), distributedAmount);
        }
    }

    @Override
    public void txReceived() {
      // do nothing, since we are using a loop on blockStarted over all transactions
    }
}
