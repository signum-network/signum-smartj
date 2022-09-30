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
 * given timeout. The auction can have a max price set or it can also be a Dutch
 * stile auction.
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
public class SignumArt2 extends Contract {

	// Main status variables
	Address owner;
	long status;
	long currentPrice;
	
	// Platform configuration
	Address platformAddress;
	long platformFee;
	long royaltiesFee;
	Address royaltiesOwner;
	
	// Tracker addresses
	Address trackSetNotForSale;
	Address trackSetForSale;
	Address trackAuctionOpened;	
	Address trackNewAuctionBid;
	Address trackNewOwner;
	Address trackOwnershipTransferred;
	Address trackDutchAuctionOpened;
	Address trackOfferReceived;
	Address trackOfferRemoved;
	Address trackLikeReceived;
	Address trackRoyaltiesTransfer;
	
	// Auction variables
	Timestamp auctionTimeout;
	Address highestBidder;
	long auctionMaxPrice;
	
	// Offer for an item even when it is not for sale/auction
	Address offerAddress;
	long offerPrice;
	
	// Dutch auction variables
	long duchStartHeight;
	long startPrice;
	long priceDropPerBlock;
	long reservePrice;

	// Additional statistics
	long totalTimesSold;
	long totalBidsReceived;
	long totalRoyaltiesFee;
	long totalPlatformFee;
	long totalLikes;
	
	// Intermediate variables
	long amountToRoyalties;
	long amountToPlatform;

	// Soulbound feature
	boolean UseSoulbound;
	boolean unbound = true;
	// Other constants
	private static final long ZERO = 0;
	private static final long STATUS_NOT_FOR_SALE = ZERO;
	private static final long STATUS_FOR_SALE = 1;
	private static final long STATUS_FOR_AUCTION = 2;
	
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
		if (owner.equals(this.getCurrentTxSender()) && unbound) {
			// only the current owner can transfer
			sendMessage(owner.getId(), newOwner.getId(), trackOwnershipTransferred);	
			owner = newOwner;
			if (UseSoulbound) {
				unbound = false;
			}
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
			sendMessage(royaltiesOwner.getId(), newRoyaltiesOwner.getId(), trackRoyaltiesTransfer);	
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
			sendMessage(owner.getId(), trackSetNotForSale);
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
		if (highestBidder==null && owner.equals(this.getCurrentTxSender()) && unbound) {
			// only if there is no bidder and it is the current owner
			status = STATUS_FOR_SALE;
			currentPrice = priceNQT;
			duchStartHeight = ZERO;
			sendMessage(owner.getId(), currentPrice, trackSetForSale);
		}
	}
	
	/**
	 * Put this token for a Duch auction style.
	 * 
	 * Buyer needs to transfer at least the current price, calculated as a function of block height.
	 * At every new block the price should be subtracted by the {@link #priceDropPerBlock} until
	 * the {@link #reservePrice} is reached.
	 * 
	 * @param startPrice the starting price
	 * @param priceDropPerBlock the amount to be subtracted from the price per block
	 * @param reservePrice the minimum accepted price
	 */
	public void putForDuchAuction(long startPrice, long reservePrice, long priceDropPerBlock) {
		if (highestBidder==null && owner.equals(this.getCurrentTxSender()) && unbound) {
			// only if there is no bidder and it is the current owner
			status = STATUS_FOR_SALE;
			duchStartHeight = getBlockHeight();
			currentPrice = startPrice;
			this.startPrice = startPrice;
			this.priceDropPerBlock = priceDropPerBlock;
			this.reservePrice = reservePrice;
			
			sendMessage(owner.getId(), startPrice, reservePrice, priceDropPerBlock, trackDutchAuctionOpened);
		}
	}

	/**
	 * Put this token for auction with the minimum bid price and/or a max price
	 * 
	 * Bidders need to transfer more than current highest to become the new
	 * highest bidder. Previous highest bidder is refunded in case of a new
	 * highest bid arrives.
	 * 
	 * @param priceNQT the minimum bid price in NQT==1E-8 SIGNA (buyer needs to
	 *                 transfer at least this amount + contract fees)
	 * @param timeout  how many minutes the sale will be available
	 */
	public void putForAuction(long priceNQT, long maxPrice, int timeout) {
		if (highestBidder==null && owner.equals(this.getCurrentTxSender()) && unbound) {
			// only if there is no bidder and it is the current owner
			status = STATUS_FOR_AUCTION;
			auctionTimeout = getBlockTimestamp().addMinutes(timeout);
			currentPrice = priceNQT;
			auctionMaxPrice = maxPrice;
			sendMessage(owner.getId(), currentPrice, auctionMaxPrice, auctionTimeout.getValue(), trackAuctionOpened);
		}
	}
	
	/**
	 * Make an offer for an item even if that is not for sale/auction.
	 * 
	 * This allows for potential buyers to make an offer even for items that are not for sale.
	 * The offer amount is locked in the contract until cancelled/accepted or a higher offer is received.
	 * 
	 * This offer has to be higher than a previous offer and can be cancelled later.
	 * The owner can accept the offer and the offer is cancelled/refunded if the item is
	 * actually sold or a running auction ends.
	 * 
	 */
	public void makeOffer() {
		if(getCurrentTxAmount() > offerPrice && highestBidder==null && unbound )  {
			if(offerAddress != null) {
				// send back the latest offer
				sendAmount(offerPrice, offerAddress);
				sendMessage(offerAddress.getId(), offerPrice, trackOfferRemoved);
			}
			offerAddress = getCurrentTxSender();
			offerPrice = getCurrentTxAmount();
			sendMessage(offerAddress.getId(), offerPrice, trackOfferReceived);
			
			return;
		}
		// send back funds of an invalid order
		sendAmount(getCurrentTxAmount(), getCurrentTxSender());
	}
	
	/**
	 * Cancel a previously posted offer getting back the offer amount.
	 * 
	 * Only the offer maker can cancel an open offer.
	 */
	public void cancelOffer() {
		if(getCurrentTxSender().equals(offerAddress)) {
			cancelOfferIfPresent();
		}
	}
	
	private void cancelOfferIfPresent() {
		if(offerAddress == null)
			return;
		sendMessage(offerAddress.getId(), offerPrice, trackOfferRemoved);
		sendAmount(offerPrice, offerAddress);
		offerAddress = null;
		offerPrice = ZERO;
	}
	
	/**
	 * The owner accepts a posted offer.
	 */
	public void acceptOffer() {
		if(highestBidder==null && getCurrentTxSender().equals(owner) && offerPrice > ZERO && unbound) {
			currentPrice = offerPrice;
			pay();
			if (status != STATUS_NOT_FOR_SALE) {
				sendMessage(owner.getId(), trackSetNotForSale);
			}
			sendMessage(offerAddress.getId(), currentPrice,owner.getId(), trackNewOwner);
			owner = offerAddress;
			offerAddress = null;
			offerPrice = ZERO;
			status = STATUS_NOT_FOR_SALE;
			if (UseSoulbound) {
				unbound = false;
			}

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
			if (duchStartHeight > ZERO) {
				// Duch auction style, calculate the current price
				currentPrice = startPrice - (getBlockHeight() - duchStartHeight) * priceDropPerBlock;
				if(currentPrice < reservePrice) {
					currentPrice = reservePrice;
				}
			}
			if (getCurrentTxAmount() >= currentPrice) {
				// Conditions match, let's execute the sale
				pay(); // pay the current owner
				sendMessage(getCurrentTxSender().getId(), getCurrentTxAmount(), owner.getId(),trackNewOwner);
				owner = getCurrentTxSender(); // new owner
				status = STATUS_NOT_FOR_SALE;
				cancelOfferIfPresent();
				if (UseSoulbound) {
					unbound = false;
				}
				return;
			}
		}
		if (status == STATUS_FOR_AUCTION) {
			if (getBlockTimestamp().ge(auctionTimeout)) {
				// auction timed out, apply the transfer if any
				if (highestBidder != null) {
					pay(); // pay the current owner
					sendMessage(highestBidder.getId(), currentPrice,owner.getId(), trackNewOwner);
					owner = highestBidder; // new owner
					highestBidder = null;
					status = STATUS_NOT_FOR_SALE;
					auctionMaxPrice = ZERO;
					cancelOfferIfPresent();
					if (UseSoulbound) {
						unbound = false;
					}
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
				sendMessage( highestBidder.getId(), currentPrice, trackNewAuctionBid);
				
				if(auctionMaxPrice > ZERO && currentPrice >= auctionMaxPrice) {
					// max price reached, so we also end the auction
					pay(); // pay the current owner
					sendMessage(highestBidder.getId(), currentPrice,owner.getId(), trackNewOwner);
					owner = highestBidder; // new owner
					highestBidder = null;
					status = STATUS_NOT_FOR_SALE;
					auctionMaxPrice = ZERO;
					cancelOfferIfPresent();
					if (UseSoulbound) {
						unbound = false;
					}
				}
				return;
			}
		}
		// send back funds of an invalid order
		sendAmount(getCurrentTxAmount(), getCurrentTxSender());
	}
	
	public void likeIt() {
		totalLikes++;
		sendMessage(getCurrentTxSender().getId(), trackLikeReceived);
	}
	public SetMetaDataAlias() {
		if(!getCurrentTx().getSenderAddress().equals(getCreator())){
		  MetaAlias = get.tx.message.argument1();
		  saveValue(1,1,value:MetaAlias);
		}
	}
	
	public SetValues() {
		if(!getCurrentTx().getSenderAddress().equals(getCreator())){
		keyvalue1 = get.tx.message.argument1();
		keyvalue2 = get.tx.message.argument2();
		value = get.tx.message.argument2();
		if (keyvalue1 != 1){
			saveValue(keyvalue1,keyvalue2,value);
		   }
		}
  	}
  
	private void saveValue(long key1, long key2, long value) {
		setMapValue(key1, key2, value);
	}

	private void pay() {
		amountToPlatform = currentPrice * platformFee / THOUSAND;
		amountToRoyalties = currentPrice * royaltiesFee / THOUSAND;
		
		totalPlatformFee += amountToPlatform;
		totalRoyaltiesFee += amountToRoyalties;
		totalTimesSold++;
		
		sendAmount(amountToRoyalties, royaltiesOwner);
		sendAmount(currentPrice - amountToPlatform - amountToRoyalties, owner);
	}
	
	protected void blockFinished(){
		// The platform fee is the remaining balance
		if(status == STATUS_NOT_FOR_SALE && offerAddress == null) {
			sendAmount(getCurrentBalance(), platformAddress);
		}
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
		emu.createConctract(creator, token1, SignumArt2.class, Contract.ONE_BURST);

		emu.forgeBlock();

		new EmulatorWindow(SignumArt2.class);
	}
}