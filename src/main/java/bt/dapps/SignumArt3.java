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
public class SignumArt3 extends Contract {

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
	boolean useSoulbound;
	boolean unbound = true;

	//DynamicNFT
	long MetaAlias;

	// Bulk NFT
	boolean bulkNFT;
	long bulkSize;
	long buffer;
	Register arguments;
	Address seller;
	long maxSaleSize;
	long salePrice;
	long buying;
	long quantity;
	long position;
	
	//Internal variables
	long accountId;
	private static final long INDEX_OWNERS = 0;
	private static final long INDEX_ONSALE = 1;
	private static final long INDEX_PRICE_SELLER = 2;

	// Index likes
	private static final long INDEX_Likes = 3;

	// Other constants
	private static final long ZERO = 0;
	private static final long FOUR = 4;
	private static final long STATUS_NOT_FOR_SALE = ZERO;
	private static final long STATUS_FOR_SALE = 1;
	private static final long STATUS_FOR_AUCTION = 2;
	private static final long STATUS_FOR_BULKNFT = 3;

	private static final long THOUSAND = 1000;
	private static final long ONE = 1;
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
		if (owner.equals(this.getCurrentTxSender()) && unbound && bulkNFT == false) {
			// only the current owner can transfer
			sendMessage(owner.getId(), newOwner.getId(), trackOwnershipTransferred);	
			owner = newOwner;
			if (useSoulbound) {
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
		if (highestBidder==null && owner.equals(this.getCurrentTxSender()) && unbound && bulkNFT == false) {
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
		if (highestBidder==null && owner.equals(this.getCurrentTxSender()) && unbound && bulkNFT == false) {
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
		if (highestBidder==null && owner.equals(this.getCurrentTxSender()) && unbound && bulkNFT == false) {
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
		if(getCurrentTxAmount() > offerPrice && highestBidder==null && unbound && bulkNFT == false )  {
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
		if(highestBidder==null && getCurrentTxSender().equals(owner) && offerPrice > ZERO && unbound && bulkNFT == false) {
			currentPrice = offerPrice;
			pay();
			if (status != STATUS_NOT_FOR_SALE) {
				sendMessage(owner.getId(), trackSetNotForSale);
			}
			sendMessage(offerAddress.getId(), currentPrice, owner.getId(), ZERO, trackNewOwner);
			owner = offerAddress;
			offerAddress = null;
			offerPrice = ZERO;
			status = STATUS_NOT_FOR_SALE;
			if (useSoulbound) {
				unbound = false;
			}

		}
	}

	// All functions needes for an BulkNFT
	//Inital mint of the BulkSize to the owner
	public void mintFromStack (long size){
		if (getCurrentTxSender() == owner && size <= bulkSize-buffer && bulkNFT){
			buffer += size;
			position =  getValue(INDEX_OWNERS, getCurrentTxSender().getId());
			saveValue(INDEX_OWNERS, getCurrentTxSender().getId(), position+size);
		}
	}

	public void transferNFTs (Address newOwner , long size ) {
		if (bulkNFT){
			// Allow transfer only from owner if soulbound is activated (true)
			if (getCurrentTxSender() == owner || !useSoulbound){
				position =  getValue(INDEX_OWNERS, getCurrentTxSender().getId());
				if (size <= position){
					// we transfer on the maps the positions
					saveValue(INDEX_OWNERS,getCurrentTxSender().getId(), position-size);
					position =  getValue(INDEX_OWNERS, newOwner.getId());
					saveValue(INDEX_OWNERS, newOwner.getId(), position+size);
				}
			}
		}
	}
	// In this function we manage sale/ reduce & not for sale 
	// If MaxSaleSize == 0 no sale even a price may still be set
	public void bulkNFTSale(long size,long salePrice){
		if (bulkNFT){
			// Allow sales only from owner if soulbound is activated (true)
			if (getCurrentTxSender() == owner || !useSoulbound){
				accountId = getCurrentTxSender().getId();
				maxSaleSize = getValue(INDEX_ONSALE, accountId);
				position =  getValue(INDEX_OWNERS, accountId);
				//Saving new Sales price
				saveValue(INDEX_PRICE_SELLER, accountId, salePrice);
				//Check for quantity change
				if  (size <= maxSaleSize){
					//Remove from Sale
					saveValue(INDEX_OWNERS,accountId, position+maxSaleSize-size);
					saveValue(INDEX_ONSALE, accountId, size);
				}
				else{
					//Add to Sale
					if(size <= position){
					saveValue(INDEX_OWNERS, accountId, position-size-maxSaleSize);
					saveValue(INDEX_ONSALE, accountId, size);
					}
				}
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
				sendMessage(getCurrentTxSender().getId(), getCurrentTxAmount(), owner.getId(), ZERO, trackNewOwner);
				owner = getCurrentTxSender(); // new owner
				status = STATUS_NOT_FOR_SALE;
				cancelOfferIfPresent();
				if (useSoulbound) {
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
					sendMessage(highestBidder.getId(), currentPrice, owner.getId(), ZERO, trackNewOwner);
					owner = highestBidder; // new owner
					highestBidder = null;
					status = STATUS_NOT_FOR_SALE;
					auctionMaxPrice = ZERO;
					cancelOfferIfPresent();
					if (useSoulbound) {
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
					sendMessage(highestBidder.getId(), currentPrice,owner.getId(), ZERO, trackNewOwner);
					owner = highestBidder; // new owner
					highestBidder = null;
					status = STATUS_NOT_FOR_SALE;
					auctionMaxPrice = ZERO;
					cancelOfferIfPresent();
					if (useSoulbound) {
						unbound = false;
					}
				}
				return;
			}
		}
		if (status == STATUS_FOR_BULKNFT) {
			arguments = getCurrentTx().getMessage();
			seller = getAddress(arguments.getValue1());
			maxSaleSize = getValue(INDEX_ONSALE, seller.getId());
			salePrice = getValue(INDEX_PRICE_SELLER, seller.getId());
			buying = getCurrentTxAmount()/salePrice;
			if (buying > ZERO && buying <= maxSaleSize ){
				//Remmove size from sale
				accountId = getCurrentTxSender().getId();
				saveValue(INDEX_ONSALE, seller.getId(), maxSaleSize-buying);
				//Add Size to buyer
				quantity =  getValue(INDEX_OWNERS, accountId);
				saveValue(INDEX_OWNERS, accountId, buying+quantity);
				//Execution of Buy
				currentPrice =  getCurrentTxAmount();
				amountToPlatform = currentPrice * platformFee / THOUSAND;
				amountToRoyalties = currentPrice * royaltiesFee / THOUSAND;
				totalPlatformFee += amountToPlatform;
				totalRoyaltiesFee += amountToRoyalties;
				totalTimesSold += buying;
				sendAmount(amountToRoyalties, royaltiesOwner);
				sendAmount(currentPrice - amountToPlatform - amountToRoyalties, seller);
				// We cant send messages to trackerAccount as we could have several sales in on block:( 

			}
			return;

		}

		// send back funds of an invalid order
		sendAmount(getCurrentTxAmount(), getCurrentTxSender());
	}
	
	public void likeIt() {
		//Checking with maps that one account can only make one like
		// I assume that if nothing is set for the key-value it returns 0
		accountId = getCurrentTxSender().getId();
		if 	(getValue(INDEX_Likes,accountId) == ZERO){
			totalLikes++;
			saveValue(INDEX_Likes, accountId, ONE);
			sendMessage(accountId, trackLikeReceived);
		}
	}
	
	public void setMetaDataAlias() {
		if(getCurrentTx().getSenderAddress().equals(getCreator())){
		  saveValue(FOUR,ONE,getCurrentTx().getMessage().getValue1());
		}
	}
	
	public void setValues() {
		if(getCurrentTx().getSenderAddress().equals(getCreator())){
		if (getCurrentTx().getMessage().getValue1() > FOUR){
			saveValue(getCurrentTx().getMessage().getValue1(),getCurrentTx().getMessage().getValue2(),getCurrentTx().getMessage().getValue3());
		   }
		}
  	}
  
	private void saveValue(long key1, long key2, long value) {
		setMapValue(key1, key2, value);
	}
	
	private long getValue(long key1, long key2) {
		return getMapValue(key1, key2);
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