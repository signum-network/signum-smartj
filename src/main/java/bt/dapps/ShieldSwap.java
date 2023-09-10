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
 * An Automated Market Maker (AMM) or liquidity pool smart contract.
 * 
 * There is a pair of token X and token Y that are kept inside the contract.
 * 
 * When adding liquidity an investor gets back the token XY. This token can
 * be used just as a regular token. The investor can later remove liquidity
 * by sending back the XY tokens.
 * 
 * About the liquidity token, XY, it is issued by the contract when it first
 * runs. Be sure to send at least the SIGNA amount for the token issuance fee
 * in a first transaction.
 * 
 * New tokens are minted by code when liquidity is added and burnt when liquidity is
 * removed.
 * 
 * The code is "shielded", not allowing the "sandwich attack", with all swaps
 * in a given block paying the same price and all liquidity addition/removal going
 * before any trades.
 * 
 * @author jjos
 *
 */
public class ShieldSwap extends Contract {
	//Variables to setup LP	
	long name;
	long tokenX;
	long tokenY;
	// portal fee address
	Address platformContract;
	//address to change swap fee for LPs
	Address swapFeeAddress;
	boolean isSwapFeeDynamic;
	// Fee 1% = 100
	long swapFee;
	long platformFeeSet;

	// Internal variabels used by the contract
	long tokenXY;
	long reserveX;
	long reserveY;
	long totalSupply;
	
	long reserveXBlock, reserveYBlock;
	long priceTimesReserveMaxX, priceTimesReserveMaxY, priceTimesReserve;
	Timestamp lastProcessedLiquidity;
	Timestamp lastProcessedSwapCheck;
	Timestamp lastProcessedSwap;
	long platformFee;
	long platformFeeBlockX;
	long platformFeeBlockY;
	long lpFeeBlockX;
	long lpFeeBlockY;
	long swapVolumeX;
	long swapVolumeY;
	
	Transaction tx;
	Register arguments;
	boolean txApproved;
	long minOut;
	
	// temporary variables
	long dx, dy;
	long liquidity;
	long liquidity2;
	long fee, x1, y1;
	long slippage;
	long checkFirstLP;
	long checkFirstLPFlag;
	 Address SenderAccount; 
	// We want the sqrt, so power is 0.5 = 5000_0000 / 10000_0000;
	private static final long SQRT_POW = 5000_0000;

	private static final long KEY_PROCESS_SWAP = 0;
	private static final long KEY_RESERVE_X = 1;
	private static final long KEY_RESERVE_Y = 2;
	private static final long KEY_LP_FEE_X  = 3;
	private static final long KEY_LP_FEE_Y  = 4;
	private static final long KEY_PF_FEE_X  = 5;
	private static final long KEY_PF_FEE_Y  = 6;
	private static final long KEY_SWAP_X_VOLUME   = 7;
	private static final long KEY_SWAP_Y_VOLUME   = 8;

	public static final long ADD_LIQUIDITY_METHOD = 1;
	public static final long REMOVE_LIQUIDITY_METHOD = 2;
	public static final long SWAP_XY_METHOD = 3;
	public static final long SWAP_YX_METHOD = 4;
	public static final long CHANGE_SWAP_FEE = 10;
	
	public static final long ZERO = 0;
	public static final long ONE = 1;
	public static final long TWO = 1;
	public static final long TENTHOUSAND = 10000;	
	public static final long minSlippage= 1010;	

	private static final long LP_CHECK_1 = 100000000;
	private static final long LP_CHECK_2 = 10000000;
	private static final long LP_CHECK_3 = 1000000;

	// 1010 means  minSlippage needs to be 0.1%
	public ShieldSwap() {
		// constructor, runs when the first TX arrives
		tokenXY = issueAsset(name, 0L, ZERO);
	}
	
	/**
	 * We process all the swap transactions that will be approved so all swaps will pay the same price.
	 * 
	 * This avoids the "sandwich attack" present in most liquidity pools available today.
	 */
	@Override
	protected void blockStarted() {
		if(tokenXY == 0L && tokenX == tokenY){
			// pool not initialized, and token x and y are the same (for any reason) do nothing
			return;
		}
		// First we iterate to add/remove liquidity
		while(true) {
			tx = getTxAfterTimestamp(lastProcessedLiquidity);
			if(tx == null) {
				break;
			}
			lastProcessedLiquidity = tx.getTimestamp();
			arguments = tx.getMessage();
			// we check also for a swapfee change
			if ((arguments.getValue1() == CHANGE_SWAP_FEE) && tx.getSenderAddress() == swapFeeAddress && isSwapFeeDynamic){
				if(arguments.getValue2() >= ZERO){
					swapFee = arguments.getValue2();
				}
			}
			
			if(arguments.getValue1() == ADD_LIQUIDITY_METHOD) {
				dx = tx.getAmount(tokenX);
				dy = tx.getAmount(tokenY);
				SenderAccount = tx.getSenderAddress();
				if(totalSupply == ZERO) {
					checkFirstLPFlag = ZERO;
					liquidity = calcPow(dx, SQRT_POW)*calcPow(dy, SQRT_POW);
					checkFirstLP = liquidity/LP_CHECK_1;
					if (checkFirstLP > ZERO ){
						liquidity = checkFirstLP;
						checkFirstLPFlag = ONE;
					}
					checkFirstLP = liquidity/LP_CHECK_2;
					if (checkFirstLP > ZERO && checkFirstLPFlag == ZERO ){
						liquidity = checkFirstLP;
						checkFirstLPFlag = ONE;
					}
					checkFirstLP = liquidity/LP_CHECK_3;
					if (checkFirstLP > ZERO && checkFirstLPFlag == ZERO ){
						liquidity = checkFirstLP;
					}
				}
				else {
					liquidity = calcMultDiv(dx, totalSupply, reserveX);
					liquidity2 = calcMultDiv(dy, totalSupply, reserveY);
					if(liquidity2 < liquidity)
						liquidity = liquidity2;
				}
				
				mintAsset(tokenXY, liquidity);
				sendAmount(tokenXY, liquidity, SenderAccount);
				
				totalSupply = totalSupply + liquidity;
				reserveX += dx;
				reserveY += dy;
			}
			else if(arguments.getValue1() == REMOVE_LIQUIDITY_METHOD) {
		        liquidity = tx.getAmount(tokenXY);
				SenderAccount = tx.getSenderAddress();
		        dx = calcMultDiv(liquidity, reserveX, totalSupply);
		        dy = calcMultDiv(liquidity, reserveY, totalSupply);
		        totalSupply = totalSupply - liquidity;
		        reserveX -= dx;
		        reserveY -= dy;
		        
		        sendAmount(tokenX, dx,SenderAccount);
		        sendAmount(tokenY, dy, SenderAccount);
		        // burn the XY token
		        sendAmount(tokenXY, liquidity, getAddress(ZERO));
		    }
		}

		// Now we iterate to check which swaps should be accepted and what should be
		// the reserve changes within the block
		reserveXBlock = reserveX;
		reserveYBlock = reserveY;
		priceTimesReserveMaxX = ZERO;
		priceTimesReserveMaxY = ZERO;
		platformFeeBlockX = ZERO;
		platformFeeBlockY = ZERO;
		lpFeeBlockX = ZERO;
		lpFeeBlockY = ZERO;
		swapVolumeX = ZERO;
		swapVolumeY = ZERO;

		while(true) {
			tx = getTxAfterTimestamp(lastProcessedSwapCheck);
			if(tx == null) {
				break;
			}
			lastProcessedSwapCheck = tx.getTimestamp();
			
			if(totalSupply == ZERO) {
				// no liquidity to operate
				continue;
			}
			// Set dx and dy to ZERO otherwise double count at line 289
			dx = ZERO;
			dy = ZERO;
			txApproved = false;
			arguments = tx.getMessage();
			minOut = arguments.getValue2();
			if(minOut > ZERO) {
				if(arguments.getValue1() == SWAP_XY_METHOD) {
					dx = tx.getAmount(tokenX);
					fee = calcMultDiv(dx, swapFee,TENTHOUSAND);
					platformFee =calcMultDiv(dx, platformFeeSet,TENTHOUSAND);
					x1 = reserveXBlock + dx;
					y1 = calcMultDiv(reserveXBlock, reserveYBlock, x1 - fee - platformFee);

					dy = y1 - reserveYBlock;
					priceTimesReserve = calcMultDiv(dx, reserveY, minOut);
					
					if(-dy >= minOut && priceTimesReserve > ZERO) {
						if (priceTimesReserveMaxX == ZERO) {
							// first accepted swap in this direction, check for a minimum slippage
							slippage = calcMultDiv(priceTimesReserve, 1000, reserveX);
							if(slippage < minSlippage) {
								// below minimum slippage
								continue;
							}
							priceTimesReserveMaxX = priceTimesReserve;
						}
						if (priceTimesReserve <= priceTimesReserveMaxX){
							txApproved = true;
							platformFeeBlockX += platformFee;
							lpFeeBlockX += fee;
							swapVolumeX += dx;
						}
					}
				}
				else if(arguments.getValue1() == SWAP_YX_METHOD) {
					dy = tx.getAmount(tokenY);
					
					fee = calcMultDiv(dy, swapFee,TENTHOUSAND);;
					platformFee = calcMultDiv(dy, platformFeeSet,TENTHOUSAND);
					y1 = reserveYBlock + dy;
					x1 = calcMultDiv(reserveXBlock, reserveYBlock, y1 - fee - platformFee);
					
					dx = x1 - reserveXBlock;
					priceTimesReserve = calcMultDiv(dy, reserveX, minOut);

					if(-dx >= minOut && priceTimesReserve > ZERO) {
						if (priceTimesReserveMaxY == ZERO) {
							// first accepted swap in this direction, check for a minimum slippage
							slippage = calcMultDiv(priceTimesReserve, 1000, reserveY);
							if(slippage < minSlippage) {
								// below minimum slippage
								continue;
							}
							priceTimesReserveMaxY = priceTimesReserve;
						}
						if (priceTimesReserve <= priceTimesReserveMaxY){
							txApproved = true;
							platformFeeBlockY += platformFee;
							lpFeeBlockY += fee;
							swapVolumeY += dy;
						}
					}
				}
				
				if(txApproved) {
					// Update the amount exchanged and store the tx as processed
					reserveXBlock += dx;
					reserveYBlock += dy;
					setMapValue(KEY_PROCESS_SWAP, tx.getId(), minOut);
				}
			}			
		}
		
		// finally, we execute the accepted swaps with the liquid changes, all paying the same price
		while(true) {
			tx = getTxAfterTimestamp(lastProcessedSwap);
			if(tx == null) {
				break;
			}
			lastProcessedSwap = tx.getTimestamp();
			SenderAccount = tx.getSenderAddress();
			arguments = tx.getMessage();
			minOut = arguments.getValue2();
			if(arguments.getValue1() == SWAP_XY_METHOD || arguments.getValue1() == SWAP_YX_METHOD) {
				if(getMapValue(KEY_PROCESS_SWAP, tx.getId()) == ZERO) {
					// this swap was not approved, refund
					sendAmount(tokenX, tx.getAmount(tokenX), SenderAccount);
					sendAmount(tokenY, tx.getAmount(tokenY), SenderAccount);
				}
				else {
					if(arguments.getValue1() == SWAP_XY_METHOD) {
						dx = tx.getAmount(tokenX);
						fee = calcMultDiv(dx, swapFee,TENTHOUSAND)+ calcMultDiv(dx, platformFeeSet,TENTHOUSAND);
						dx -= fee;
						dy = calcMultDiv(-dx, reserveY, reserveXBlock);
							
						sendAmount(tokenY, -dy, SenderAccount);
					}
					else {
						// swap YX
						dy = tx.getAmount(tokenY);
						
						fee = calcMultDiv(dy, swapFee,TENTHOUSAND) + calcMultDiv(dy, platformFeeSet,TENTHOUSAND);
						dy -= fee;
						dx = calcMultDiv(-dy, reserveX, reserveYBlock);
						
						sendAmount(tokenX, -dx, SenderAccount);
					}
				}
			}
		}
		if(platformFeeBlockX > ZERO) {
			sendAmount(tokenX, platformFeeBlockX, platformContract);
		}
		if(platformFeeBlockY > ZERO) {
			sendAmount(tokenY, platformFeeBlockY, platformContract);
		}
		// store the price on this block
		setMapValue(KEY_RESERVE_X, this.getBlockHeight(), reserveXBlock);
		setMapValue(KEY_RESERVE_Y, this.getBlockHeight(), reserveYBlock);
		// store the platform fee on this block
		setMapValue(KEY_PF_FEE_X ,this.getBlockHeight(), platformFeeBlockX);
		setMapValue(KEY_PF_FEE_Y, this.getBlockHeight(), platformFeeBlockY);
		// store the  swap volume X and Y
		setMapValue(KEY_SWAP_X_VOLUME ,this.getBlockHeight(), swapVolumeX);		
		setMapValue(KEY_SWAP_Y_VOLUME ,this.getBlockHeight(), swapVolumeY);	
		// store the lp fee for x and Y
		setMapValue(KEY_LP_FEE_X ,this.getBlockHeight(), lpFeeBlockX);		
		setMapValue(KEY_LP_FEE_Y ,this.getBlockHeight(), lpFeeBlockY);		
		// update the reserves when the block finishes to reconcile any dust/revenue
		reserveX = this.getCurrentBalance(tokenX);
		reserveY = this.getCurrentBalance(tokenY);
		if(totalSupply == ZERO && tokenXY != 0L ){
			if(tokenX != 0L && tokenY != 0L ) {
				dx =  getCurrentBalance() - getActivationFee();
				if (dx > ZERO) {
					sendAmount(dx, platformContract);
				}
			}
		}
	}
	
	/**
	 * This method removes/cleans-up an unwanted token.
	 * 
	 * @param tokenId the token ID
	 */
	public void cleanToken(long tokenId) {
		if(tokenId == 0L || tokenId == tokenX || tokenId == tokenY) {
			// invalid, so we do nothing
			return;
		}
		sendAmount(tokenId, getCurrentBalance(tokenId), platformContract);
	}
	
	/**
	 * Allows to update the platform contract account.
	 * 
	 * @param newPlatformContractId
	 */
	public void upgradePlatformContract(long newPlatformContractId) {
		if(getCurrentTxSender() == platformContract) {
			// only the current platform can upgrade itself to a new account
			platformContract = getAddress(newPlatformContractId);
		}
	}

	@Override
	public void txReceived() {
		// do nothing
	}
	
	public static void main(String[] args) throws Exception {
		BT.activateSIP37(true);
		
		Emulator emu = Emulator.getInstance();
		
		Address buyer1 = emu.getAddress("BUYER1");
		emu.airDrop(buyer1, 1000*Contract.ONE_SIGNA);
		Address buyer2 = emu.getAddress("BUYER2");
		emu.airDrop(buyer2, 1000*Contract.ONE_SIGNA);

		Address lpProvider = emu.getAddress("LP_PROVIDER");
		emu.airDrop(lpProvider, 10000*Contract.ONE_SIGNA);
		long BTC_ASSET_ID = emu.issueAsset(lpProvider, 11111, 0, 8);
		emu.mintAsset(lpProvider, BTC_ASSET_ID, 2*Contract.ONE_SIGNA);
		
		Address lp = Emulator.getInstance().getAddress("LP");
		emu.createConctract(lpProvider, lp, ShieldSwap.class, Contract.ONE_SIGNA);
		emu.forgeBlock();
		ShieldSwap contract = (ShieldSwap) lp.getContract();
		contract.tokenY = BTC_ASSET_ID;
		contract.swapFee = Long.MAX_VALUE;
		contract.platformFeeSet = Long.MAX_VALUE;
		// contract.minSlippage = 1010;

		long reserveX = 10000*Contract.ONE_SIGNA;
		long reserveY = 2*Contract.ONE_SIGNA;
		emu.send(lpProvider, lp, reserveX, BTC_ASSET_ID, reserveY, Register.newInstance(ADD_LIQUIDITY_METHOD, 0, 0, 0));
		emu.forgeBlock();		
		
		long sendX = 100*Contract.ONE_SIGNA;
		long expectedY = (long)(reserveY * sendX * 0.9) / reserveX ;
		
		emu.send(buyer1, lp, sendX,	Register.newInstance(SWAP_XY_METHOD, expectedY, 0, 0));
		emu.send(buyer2, lp, sendX,	Register.newInstance(SWAP_XY_METHOD, expectedY, 0, 0));
		emu.forgeBlock();
		emu.forgeBlock();
		
		new EmulatorWindow(ShieldSwap.class);
	}
	
}
