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
	
	long name;
	long decimalPlaces;
	
	long tokenX;
	long tokenY;
	
	Address platformContract;
	
	long swapFeeDiv;
	long platformFeeDiv;
	long minSlippage;
	
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
	
	// We want the sqrt, so power is 0.5 = 5000_0000 / 10000_0000;
	private static final long SQRT_POW = 5000_0000;

	private static final long KEY_PROCESS_SWAP = 0;
	private static final long KEY_RESERVE_X = 1;
	private static final long KEY_RESERVE_Y = 2;
	private static final long KEY_LP_FEE_X  = 3;
	private static final long KEY_LP_FEE_Y  = 4;
	private static final long Key_PF_FEE_X  = 5;
	private static final long Key_PF_FEE_Y  = 6
	private static final long KEY_SWAP_X_VOLUME   = 7;
	private static final long KEY_SWAP_Y_VOLUME   = 8;

	public static final long ADD_LIQUIDITY_METHOD = 1;
	public static final long REMOVE_LIQUIDITY_METHOD = 2;
	public static final long SWAP_XY_METHOD = 3;
	public static final long SWAP_YX_METHOD = 4;
	
	public static final long ZERO = 0;
			
	public ShieldSwap() {
		// constructor, runs when the first TX arrives
		tokenXY = issueAsset(name, 0L, decimalPlaces);
	}
	
	/**
	 * We process all the swap transactions that will be approved so all swaps will pay the same price.
	 * 
	 * This avoids the "sandwich attack" present in most liquidity pools available today.
	 */
	@Override
	protected void blockStarted() {
		if(tokenXY == 0L) {
			// pool not initialized, do nothing
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
			
			if(arguments.getValue1() == ADD_LIQUIDITY_METHOD) {
				dx = tx.getAmount(tokenX);
				dy = tx.getAmount(tokenY);
				
				if(totalSupply == ZERO) {
					liquidity = calcPow(dx, SQRT_POW)*calcPow(dy, SQRT_POW);
				}
				else {
					liquidity = calcMultDiv(dx, totalSupply, reserveX);
					liquidity2 = calcMultDiv(dy, totalSupply, reserveY);
					if(liquidity2 < liquidity)
						liquidity = liquidity2;
				}
				
				mintAsset(tokenXY, liquidity);
				sendAmount(tokenXY, liquidity, tx.getSenderAddress());
				
				totalSupply = totalSupply + liquidity;
				reserveX += dx;
				reserveY += dy;
			}
			else if(arguments.getValue1() == REMOVE_LIQUIDITY_METHOD) {
		        liquidity = tx.getAmount(tokenXY);

		        if(tokenX != 0L && tokenY != 0L) {
		        	// this is a token-token pool, so we send out also the SIGNA balance "pro rata"
		        	dx = calcMultDiv(liquidity, getCurrentBalance() - getActivationFee(), totalSupply);
		        	sendAmount(dx, tx.getSenderAddress());
		        }

		        dx = calcMultDiv(liquidity, reserveX, totalSupply);
		        dy = calcMultDiv(liquidity, reserveY, totalSupply);
		        
		        totalSupply = totalSupply - liquidity;
		        reserveX -= dx;
		        reserveY -= dy;
		        
		        sendAmount(tokenX, dx, tx.getSenderAddress());
		        sendAmount(tokenY, dy, tx.getSenderAddress());
		        
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
			txApproved = false;
			arguments = tx.getMessage();
			minOut = arguments.getValue2();
			if(minOut > ZERO) {
				if(arguments.getValue1() == SWAP_XY_METHOD) {
					dx = tx.getAmount(tokenX);
					fee = dx/swapFeeDiv;
					platformFee = dx/platformFeeDiv;
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
					
					fee = dy/swapFeeDiv;
					platformFee = dy/platformFeeDiv;
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
			
			arguments = tx.getMessage();
			minOut = arguments.getValue2();
			if(arguments.getValue1() == SWAP_XY_METHOD || arguments.getValue1() == SWAP_YX_METHOD) {
				if(getMapValue(KEY_PROCESS_SWAP, tx.getId()) == ZERO) {
					// this swap was not approved, refund
					sendAmount(tokenX, tx.getAmount(tokenX), tx.getSenderAddress());
					sendAmount(tokenY, tx.getAmount(tokenY), tx.getSenderAddress());
				}
				else {
					if(arguments.getValue1() == SWAP_XY_METHOD) {
						dx = tx.getAmount(tokenX);
						fee = dx/swapFeeDiv + dx/platformFeeDiv;
						dx -= fee;
						dy = calcMultDiv(-dx, reserveY, reserveXBlock);
							
						sendAmount(tokenY, -dy, tx.getSenderAddress());
					}
					else {
						// swap YX
						dy = tx.getAmount(tokenY);
						
						fee = dy/swapFeeDiv + dy/platformFeeDiv;
						dy -= fee;
						dx = calcMultDiv(-dy, reserveX, reserveYBlock);
						
						sendAmount(tokenX, -dx, tx.getSenderAddress());
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
		setMapValue(Key_PF_FEE_X ,this.getBlockHeight(), platformFeeBlockX);
		setMapValue(Key_PF_FEE_Y, this.getBlockHeight(), platformFeeBlockY);
		// store the  swap volume X and Y
		setMapValue(KEY_SWAP_X_VOLUME ,this.getBlockHeight(), swapVolumeX);		
		setMapValue(KEY_SWAP_Y_VOLUME ,this.getBlockHeight(), swapVolumeY);	
		// store the lp fee for x and Y
		setMapValue(KEY_LP_FEE_X ,this.getBlockHeight(), lpFeeBlockX);		
		setMapValue(KEY_LP_FEE_Y ,this.getBlockHeight(), lpFeeBlockY);		
		// update the reserves when the block finishes to reconcile any dust/revenue
		reserveX = this.getCurrentBalance(tokenX);
		reserveY = this.getCurrentBalance(tokenY);
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
		contract.swapFeeDiv = Long.MAX_VALUE;
		contract.platformFeeDiv = Long.MAX_VALUE;
		contract.minSlippage = 1010;

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
