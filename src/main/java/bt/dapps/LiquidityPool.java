package bt.dapps;

import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

/**
 * A liquidity pool smart contract.
 * 
 * There is a pair of token X and token Y that are kept inside the contract.
 * 
 * When adding liquidity a investor gets back the token XY. This token can
 * be used just as a regular token. The investor can later remove liquidity
 * by sending back the XY tokens.
 * 
 * About the liquidity token, XY, it is issued by the contract when it first
 * runs. Be sure to send at least the SIGNA amount to the token issuance fee
 * in a first transaction.
 * 
 * New tokens are minted by code when liquidity is added and burnt when liquidity is
 * removed.
 * 
 * @author jjos
 *
 */
public class LiquidityPool extends Contract {
	
	long namePart1;
	long namePart2;
	long decimalPlaces;
	
	long tokenX;
	long tokenY;
	
	long swapFeeDiv;
	
	long tokenXY;
	long reserveX;
	long reserveY;
	long totalSupply;
		
	// temporary variables
	long dx, dy;
	long liquidity;
	long liquidity2;
	long fee, x1, y1;
	
	// We want the sqrt, so power is 0.5 = 5000_0000 / 1_0000_0000;
	private final long SQRT_POW = 5000_0000;
	private final long ZERO = 0;
	
	public LiquidityPool() {
		// constructor, runs when the first TX arrives
		tokenXY = issueAsset(namePart1, namePart2, decimalPlaces);
	}
	
	/**
	 * Should send token X and token Y and will get back token XY.
	 */
	public void addLiquidity() {
		dx = getCurrentTxAmount(tokenX);
		dy = getCurrentTxAmount(tokenY);
		
		if(totalSupply == ZERO) {
			liquidity = calcPow(dx, SQRT_POW)*calcPow(dy, SQRT_POW);
		}
		else {
			liquidity = calcMultDiv(dx, totalSupply, reserveX);
			liquidity2 = calcMultDiv(dy, totalSupply, reserveY);
			if(liquidity2 < liquidity)
				liquidity = liquidity2;
		}
		
		mintAsset(liquidity, tokenXY);
		sendAmount(tokenXY, liquidity, getCurrentTxSender());
		
		totalSupply = totalSupply + liquidity;
		reserveX = reserveX + dx;
		reserveY = reserveY + dy;
	}
	
	/**
	 * Should send the XY token and get back X and Y.
	 */
    public void removeLiquidity() {
        liquidity = getCurrentTxAmount(tokenXY);

        dx = calcMultDiv(liquidity, reserveX, totalSupply);
        dy = calcMultDiv(liquidity, reserveY, totalSupply);

        totalSupply = totalSupply - liquidity;
        reserveX = reserveX - dx;
        reserveY = reserveY - dy;
        
        sendAmount(tokenX, dx, getCurrentTxSender());
        sendAmount(tokenY, dy, getCurrentTxSender());
        
        // burn the XY token
        sendAmount(tokenXY, liquidity, getAddress(ZERO));
    }
    
	/**
	 * Send token X and expects at least a minimum amount of Y.
	 * 
	 * @param yMinOut the minimum expected for Y, otherwise refunds.
	 */
	public void swapXY(long yMinOut) {
		dx = getCurrentTxAmount(tokenX);
		fee = dx/swapFeeDiv;
		x1 = reserveX + dx;
		y1 = calcMultDiv(reserveX, reserveY, x1 - fee);
			
		dy = y1 - reserveY;
		
		if(-dy < yMinOut) {
			refund();
			return;
		}
		
		executeSwap();
	}
	
	/**
	 * Send token Y and expects at least a minimum amount of X.
	 * 
	 * @param xMinOut the minimum expected for X, otherwise refunds.
	 */
	public void swapYX(long xMinOut) {
		dy = getCurrentTxAmount(tokenY);
		
		fee = dy/swapFeeDiv;
		y1 = reserveY + dy;
		x1 = calcMultDiv(reserveX, reserveY, y1 - fee);
		
		dx = x1 - reserveX;

		if(-dx < xMinOut) {
			refund();
			return;
		}
		
		executeSwap();
	}

	private void executeSwap() {
		// update the reserves
		reserveX = reserveX + dx;
		reserveY = reserveY + dy;

		if(dx < ZERO) {
			sendAmount(tokenX, -dx, getCurrentTxSender());
		}
		if(dy < ZERO) {
			sendAmount(tokenY, -dy, getCurrentTxSender());
		}
	}

	private void refund() {
		sendAmount(tokenX, getCurrentTxAmount(tokenX), getCurrentTxSender());
		sendAmount(tokenY, getCurrentTxAmount(tokenY), getCurrentTxSender());
	}
	
	@Override
	protected void blockFinished() {
		// update the reserves when the block finishes to reconcile any dust/revenue
		reserveX = this.getCurrentBalance(tokenX);
		reserveY = this.getCurrentBalance(tokenY);
	}

	@Override
	public void txReceived() {
		// do nothing
	}
	
	public static void main(String[] args) throws Exception {
		BT.activateSIP37(true);
		
		new EmulatorWindow(LiquidityPool.class);
	}
	
}
