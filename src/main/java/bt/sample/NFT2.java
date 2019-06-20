package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A Non-Fungible Token smart contract with DEX and auction capabilities.
 * 
 * This contract represents a non-fungible or unique token for Burst blockchain.
 * 
 * When created the contract is <i>owned</i> by the creator. After that the
 * owner can transfer the ownership to another address. Additionally, the owner
 * can put the token for sale or for auction with a given timeout.
 *
 * Although this was inspired by http://erc721.org/, the mechanics are very
 * different. For instance, there is one contract <i>instance</i> per token and
 * each token is already a decentralized exchange (DEX) with absolutely no
 * middle man.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class NFT2 extends Contract {

	public static final int STATUS_NOT_FOR_SALE = 0;
	public static final int STATUS_FOR_SALE = 1;
	public static final int STATUS_FOR_AUCTION = 2;

	public static final long ACTIVATION_FEE = ONE_BURST * 10;

	int status;
	Address owner;

	long salePrice;
	Timestamp auctionTimeout;
	Address highestBidder;
	long highestBid;

	/**
	 * Constructor, when in blockchain the constructor is called when the first TX
	 * reaches the contract.
	 */
	public NFT2() {
		// Initially token is owned by the creator
		owner = getCreator();
		status = STATUS_NOT_FOR_SALE;
	}

	/**
	 * Transfers the ownership of this token.
	 * 
	 * Only the current owner can transfer the ownership.
	 * 
	 * @param newOwner
	 */
	public void transfer(Address newOwner) {
		if (highestBidder==null && owner.equals(this.getCurrentTx().getSenderAddress())) {
			// only if there is no bidder and it is the current owner
			owner = newOwner;
			status = STATUS_NOT_FOR_SALE;
		}
	}

	/**
	 * Cancels an open for sale or auction and sets it as not for sale.
	 */
	public void setNotForSale(){
		if (highestBidder==null && owner.equals(this.getCurrentTx().getSenderAddress())) {
			// only if there is no bidder and it is the current owner
			status = STATUS_NOT_FOR_SALE;
			salePrice = 0;
		}
	}

	/**
	 * Put this token for sale for the given price.
	 * 
	 * Buyer needs to transfer at least the asked amount (plus activation fee).
	 * 
	 * @param priceNQT the price in NQT==1E-8 BURST (buyer needs to transfer at least
	 *                 this amount plus activation fee)
	 */
	public void putForSale(long priceNQT) {
		if (highestBidder==null && owner.equals(this.getCurrentTx().getSenderAddress())) {
			// only if there is no bidder and it is the current owner
			status = STATUS_FOR_SALE;
			salePrice = priceNQT;
		}
	}

	/**
	 * Put this token for auction with the minimum bid price.
	 * 
	 * Bidders need to transfer more than current highest to become the new
	 * highest bidder. Previous highest bidder is refunded in case of a new
	 * highest bid arrives.
	 * 
	 * @param priceNQT the minimum bid price in NQT==1E-8 BURST (buyer need to
	 *                 transfer at least this amount)
	 * @param timeout  how many minutes the sale will be available
	 */
	public void putForAuction(int priceNQT, int timeout) {
		if (highestBidder==null && owner.equals(this.getCurrentTx().getSenderAddress())) {
			// only if there is no bidder and it is the current owner
			status = STATUS_FOR_AUCTION;
			auctionTimeout = getBlockTimestamp().addMinutes(timeout);
			highestBid = priceNQT;
		}
	}

	/**
	 * If this contract is for sale or for auction, this method handles the payment/bid.
	 * 
	 * A buyer needs to transfer the asked price (plus activation fee) to the contract.
	 * 
	 * If the token is for auction, a bidder need to transfer more than the current highest
	 * bid to become the new highest bidder. Previous highest bidder is then refunded (minus
	 * the activation fee). After the auction timeout, any transaction received will trigger
	 * the ownershipt transfer.
	 * 
	 * If the token was not for sale or the amount is not enough, the order is
	 * refunded (minus the contract activation fee).
	 */
	public void txReceived() {
		if (status == STATUS_FOR_SALE) {
			if (getCurrentTx().getAmount() >= salePrice) {
				// Conditions match, let's execute the sale
				sendAmount(salePrice, owner); // pay the current owner
				owner = getCurrentTx().getSenderAddress(); // new owner
				status = STATUS_NOT_FOR_SALE;
				return;
			}
		}
		if (status == STATUS_FOR_AUCTION) {
			if (getBlockTimestamp().ge(auctionTimeout)) {
				// auction timed out, apply the transfer if any
				if (highestBidder != null) {
					sendAmount(highestBid, owner); // pay the current owner
					owner = highestBidder; // new owner
					highestBidder = null;
					status = STATUS_NOT_FOR_SALE;
				}
				// current transaction will be refunded below
			} else if (getCurrentTx().getAmount() > highestBid) {
				// Conditions match, let's register the bid

				// refund previous bidder, if some
				if (highestBidder != null)
					sendAmount(highestBid, highestBidder);

				highestBidder = getCurrentTx().getSenderAddress();
				highestBid = getCurrentTx().getAmount();
				return;
			}
		}
		// send back funds of an invalid order
		refund();
	}

	/**
	 * Send back funds of the current transaction.
	 */
	void refund() {
		// send back funds of an invalid order
		sendAmount(getCurrentTx().getAmount(), getCurrentTx().getSenderAddress());
	}

	/**
	 * A main function for debugging purposes only.
	 * 
	 * This function is not compiled into bytecode and do not go to the blockchain.
	 */
	public static void main(String[] args) throws Exception {
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();
		Address creator = Emulator.getInstance().getAddress("CREATOR");
		emu.airDrop(creator, 1000 * Contract.ONE_BURST);

		Address token1 = Emulator.getInstance().getAddress("TOKEN");
		emu.createConctract(creator, token1, NFT2.class, Contract.ONE_BURST);

		emu.forgeBlock();

		new EmulatorWindow(NFT2.class);
	}
}
