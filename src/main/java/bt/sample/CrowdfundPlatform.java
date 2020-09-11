package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * Crowd-funding smart contract to be used in a crowd-funding platform.
 * 
 * If a target amount is achieved before the given number of blocks, then the entire
 * balance will be sent to the beneficiary account (minus a platform fee).
 * If target is not achieved, then all transactions are refunded (minus the gas fee).
 *
 * Inspired by http://ciyam.org/at/at_crowdfund.html
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class CrowdfundPlatform extends Contract {
	
	Address beneficiary;
	long targetAmount;
	long nBlocksSleep;
	Address platform;
	long platformFeePerThousand;
	
	boolean successful;
	long fee;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public CrowdfundPlatform(){
		// We immediately put the contract to sleep by the specified number of blocks
		sleep(nBlocksSleep);
		
		// After the sleep, we check if it is successful or not
		successful = getCurrentBalance() >= targetAmount;
	}

	/**
	 * Iterates over every transaction received
	 */
	@Override
	public void txReceived(){
		if(!successful){
			// Send back funds since it failed
			sendAmount(getCurrentTxAmount(), getCurrentTxSender());
		}
	}
	
	@Override
	protected void blockFinished() {
		if(successful) {
			fee = getCurrentBalance()*platformFeePerThousand/1000;
			sendAmount(fee, platform);
			sendBalance(beneficiary);
		}
		else {
			// Send the remaining gas fee to the platform
			sendBalance(platform);
		}
	}

	public static void main(String[] args) {
		new EmulatorWindow(CrowdfundPlatform.class);
	}
}


