package bt.sample;

import bt.Address;
import bt.BT;
import bt.Contract;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A 'Farming' smart contract.
 * 
 * Farmers rent the land seeking for yield. If it 'grows'
 * (there is a new farmer interested) then the previous one
 * gets back his deposit plus the yield of 5% (minus a
 * fee of 0.5 %).
 * 
 * If there is a 'drought' (no new farmer interested), the
 * current farmer can always get back his\her deposit
 * with 5% loss.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class Farming extends Contract {
	
	public static final long GAS_FEE = FEE_QUANT * 40;
	public static final long START_DEPOSIT = 1000 * ONE_BURST;

	long yieldPercent = 5;
	long deposit;
	Address farmer;
	Address patron;
	long patronDeposit;
	long yield, fee, feePatron;
	long oldDeposit;
	Address creator;
	long gasFee = GAS_FEE;
	long feePercent = 8;
	long feePatronPercent = 2;
	long zero = 0;
	long two = 2;
	long hundred = 100;
	long startDeposit = START_DEPOSIT;

	/**
	 * A new farmer needs to make the required deposit.
	 * 
	 * Current farmer will receive his deposit back plus yield.
	 * The sender becomes the new farmer and there is a new deposit price.
	 * 
	 * In case of a 'drought' the current farmer can withdraw his
	 * deposit with less than 5% loss.
	 * 
	 */
	public void txReceived() {
		if (deposit == zero && getCurrentTxAmount() + gasFee >= startDeposit) {
			// Initialization
			oldDeposit = 0;
			yield = (startDeposit * yieldPercent) / hundred;
			deposit = startDeposit + yield * two;
			return;
		}
		if (getCurrentTxSender() == farmer && getCurrentTx().getMessage1() != zero) {
			// Current farmer sent a message, reset the contract to 1k and send the balance back
			if(deposit > patronDeposit) {
				// We have a new patron
				patron = farmer;
				patronDeposit = deposit;
			}
			oldDeposit = 0;
			yield = (startDeposit * yieldPercent) / hundred;
			deposit = startDeposit + yield * two;
			// Send back all but the initial value
			sendAmount(getCurrentBalance() - startDeposit - gasFee, farmer);
			farmer = creator;
			return;
		}
		if (getCurrentTxAmount() + gasFee >= deposit) {
			// We have a new farmer, send the deposit back plus yield, minus fee
			fee = (yield * feePercent / hundred);
			feePatron = (yield * feePatronPercent) / hundred;
			sendAmount(oldDeposit + yield - fee - feePatron - gasFee, farmer);
			sendAmount(fee, creator);
			sendAmount(feePatron, patron);
			// new farmer
			farmer = getCurrentTxSender();
			
			// update the deposit
			oldDeposit = deposit;
			yield = (oldDeposit * yieldPercent) / hundred;
			deposit = deposit + yield * two;
			
			return;
		}
		// send back funds of an invalid transactions
		sendAmount(getCurrentTxAmount(), getCurrentTxSender());
	}
	
	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public Farming() {
		creator = getCreator();
		farmer = creator;
		patron = creator;
	}
	
	public static void main(String[] args) {
		BT.activateCIP20(true);
		new EmulatorWindow(Farming.class);
	}
}
