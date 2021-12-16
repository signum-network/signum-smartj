package bt.dapps;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A Non-Fungible Token smart contract with sale and auction capabilities.
 * 
 * The contract has an owner that can transfer the ownership to another address.
 * Additionally, the owner can put the token up for sale or for auction with a
 * given timeout.
 * 
 * All operations are handled directly by the contract, so it is completely
 * decentralized.
 *
 * At every sale, the original creator can receive royalties and
 * the platform can get a fee. The current royalties holder can also transfer
 * the rights to another address (or contract).
 * 
 * All fees are configurable at the creation of the contract.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class SignumArt extends Contract {

	Address owner;
	Address platformAddress;
	long platformFee;
	long royaltiesFee;
	Address royaltiesOwner;
	
	Address tracker;

	long status;
	long currentPrice;
	Timestamp auctionTimeout;
	Address highestBidder;
	
	long totalTimesSold;
	long totalBidsReceived;
	long totalRoyaltiesFee;
	long totalPlatformFee;
	
	long amountToRoyalties;
	long amountToPlatform;

	private static final long ZERO = 0;
	private static final long STATUS_NOT_FOR_SALE = ZERO;
	private static final long STATUS_FOR_SALE = 1;
	private static final long STATUS_FOR_AUCTION = 2;
	private static final long NEW_AUCTION_BID = 3;
	private static final long NEW_OWNER = 4;
	private static final long TRANSFER_OWNERSHIP = 5;
	private static final long THOUSAND = 1000;
	
	/** Use a contract fee of 0.3 SIGNA */
	public static final long CONTRACT_FEES = 30000000;

	/**
	 * Transfers the ownership of this token.
	 * 
	 * Only the current owner can transfer the ownership.
	 * 
	 * @param newOwner
	 */
	public void transfer(Address newOwner) {
		if (owner.equals(this.getCurrentTxSender())) {
			// only the current owner can transfer
			owner = newOwner;
			sendMessage(TRANSFER_OWNERSHIP, owner.getId(), tracker);			
		}
	}
	
	/**
	 * Transfers the royalties ownership of this token.
	 * 
	 * Only the current royalties owner can transfer the ownership.
	 * 
	 * @param newRoyaltiesOwner
	 */
	public void transferRoyalties(Address newRoyaltiesOwner) {
		if (royaltiesOwner.equals(this.getCurrentTxSender())) {
			// only the current owner can transfer
			royaltiesOwner = newRoyaltiesOwner;
		}
	}

	/**
	 * Cancels an open for sale or auction and sets it as not for sale.
	 */
	public void setNotForSale(){
		if (highestBidder==null && owner.equals(this.getCurrentTxSender())) {
			// only if there is no bidder and it is the current owner
			status = STATUS_NOT_FOR_SALE;
			sendMessage(status, tracker);
		}
	}

	/**
	 * Put this token for sale for the given price.
	 * 
	 * Buyer needs to transfer at least the asked amount.
	 * 
	 * @param priceNQT the price in NQT==1E-8 SIGNA (buyer needs to transfer at least
	 *                 this amount + gas fees)
	 */
	public void putForSale(long priceNQT) {
		if (highestBidder==null && owner.equals(this.getCurrentTxSender())) {
			// only if there is no bidder and it is the current owner
			status = STATUS_FOR_SALE;
			currentPrice = priceNQT;
			sendMessage(status, owner.getId(), currentPrice, ZERO, tracker);
		}
	}

	/**
	 * Put this token for auction with the minimum bid price.
	 * 
	 * Bidders need to transfer more than current highest to become the new
	 * highest bidder. Previous highest bidder is refunded in case of a new
	 * highest bid arrives.
	 * 
	 * @param priceNQT the minimum bid price in NQT==1E-8 SIGNA (buyer needs to
	 *                 transfer at least this amount + contract fees)
	 * @param timeout  how many minutes the sale will be available
	 */
	public void putForAuction(int priceNQT, int timeout) {
		if (highestBidder==null && owner.equals(this.getCurrentTxSender())) {
			// only if there is no bidder and it is the current owner
			status = STATUS_FOR_AUCTION;
			auctionTimeout = getBlockTimestamp().addMinutes(timeout);
			currentPrice = priceNQT;
			sendMessage(status, owner.getId(), currentPrice, auctionTimeout.getValue(), tracker);
		}
	}

	/**
	 * If this contract is for sale or for auction, this method handles the payment/bid.
	 * 
	 * A buyer needs to transfer the asked price to the contract.
	 * 
	 * If the token is for auction, a bidder needs to transfer more than the current highest
	 * bid to become the new highest bidder. Previous highest bidder is then refunded (minus
	 * the contract fee). After the auction timeout, any transaction received will trigger
	 * the ownership transfer.
	 * 
	 * If the token was not for sale or the amount is not enough, the order is
	 * refunded (minus the contract fees).
	 */
	public void txReceived() {
		if (status == STATUS_FOR_SALE) {
			if (getCurrentTxAmount() >= currentPrice) {
				// Conditions match, let's execute the sale
				pay(); // pay the current owner
				owner = getCurrentTxSender(); // new owner
				status = STATUS_NOT_FOR_SALE;
				sendMessage(NEW_OWNER, owner.getId(), getCurrentTxAmount(), ZERO, tracker);
				return;
			}
		}
		if (status == STATUS_FOR_AUCTION) {
			if (getBlockTimestamp().ge(auctionTimeout)) {
				// auction timed out, apply the transfer if any
				if (highestBidder != null) {
					pay(); // pay the current owner
					owner = highestBidder; // new owner
					highestBidder = null;
					status = STATUS_NOT_FOR_SALE;
					sendMessage(NEW_OWNER, owner.getId(), currentPrice, ZERO, tracker);
				}
				// current transaction will be refunded below
			} else if (getCurrentTxAmount() > currentPrice) {
				// Conditions match, let's register the bid

				// refund previous bidder, if some
				if (highestBidder != null)
					sendAmount(currentPrice, highestBidder);

				highestBidder = getCurrentTxSender();
				currentPrice = getCurrentTxAmount();
				totalBidsReceived++;
				sendMessage(NEW_AUCTION_BID, highestBidder.getId(), currentPrice, ZERO, tracker);
				return;
			}
		}
		// send back funds of an invalid order
		sendAmount(getCurrentTxAmount(), getCurrentTxSender());
	}
	
	private void pay() {
		amountToPlatform = currentPrice * platformFee / THOUSAND;
		amountToRoyalties = currentPrice * royaltiesFee / THOUSAND;
		
		totalPlatformFee += amountToPlatform;
		totalRoyaltiesFee += amountToRoyalties;
		totalTimesSold++;
		
		sendMessage(STATUS_NOT_FOR_SALE, owner.getId(), currentPrice, ZERO, tracker);
		sendAmount(amountToRoyalties, royaltiesOwner);
		sendAmount(currentPrice - amountToPlatform - amountToRoyalties, owner);
	}
	
	protected void blockFinished(){
		// The platform fee is the remaining balance
		sendBalance(platformAddress);
	}

	/**
	 * A main function for debugging purposes only.
	 * 
	 * This function is not compiled into bytecode and do not go to the blockchain.
	 */
	public static void main(String[] args) throws Exception {
		BT.activateCIP20(true);
		
		// some initialization code to make things easier to debug
		Emulator emu = Emulator.getInstance();
		Address creator = Emulator.getInstance().getAddress("CREATOR");
		emu.airDrop(creator, 1000 * Contract.ONE_BURST);

		Address token1 = Emulator.getInstance().getAddress("TOKEN");
		emu.createConctract(creator, token1, SignumArt.class, Contract.ONE_BURST);

		emu.forgeBlock();

		new EmulatorWindow(SignumArt.class);
	}
}
