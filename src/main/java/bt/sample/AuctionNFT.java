package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * Auction smart contract with a beneficiary and with 1% fee for creator.
 * 
 * An Auction smart contract that will allow people to send funds which will be
 * refunded back to them (minus the activation fee) if someone else sends more.
 * 
 * This smart contract is initially closed and the beneficiary (the current owner)
 * is the creator. Then one can call the {@link #open(Address, long, long)} method.
 * 
 * After the auction times-out any new transaction received will trigger the auction
 * closing logic. When closing, the current beneficiary receive the highest bid amount
 * and the highest bidder becomes the new beneficiary. This new beneficiary can
 * choose to open the auction again with a new timeout and new minimum bid value.
 *
 * Inspired by http://ciyam.org/at/at_auction.html
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class AuctionNFT extends Contract {

    public static final long MIN_BALANCE = ONE_BURST * 4;
    public static final long ACTIVATION_FEE = ONE_BURST * 30;

    boolean isOpen;
    Address beneficiary;
    long highestBid;
    Address highestBidder;
    Timestamp timeout;
    long newBid, fee;

    /**
     * Constructor, when in blockchain the constructor is called when the first TX
     * reaches the contract.
     */
    public AuctionNFT() {
        beneficiary = getCreator();
    }

    /**
     * Private function for checking if the timeout expired.
     * 
     * @return true if this contract expired
     */
    private boolean expired() {
        if (!isOpen)
            return true;

        if (getBlockTimestamp().ge(timeout)) {
            isOpen = false;
            if (highestBidder != null) {
                // we have a bidder
                fee = highestBid / 100; // 1% fee
                sendAmount(fee, getCreator());

                // send the balance to the current beneficiary
                sendAmount(getCurrentBalance() - MIN_BALANCE, beneficiary);

                // set the new beneficiary
                beneficiary = highestBidder;
            }
        }
        return !isOpen;
    }

    /**
     * Opens the auction giving a beneficiary, a timeout and a minimum price.
     * 
     * @param timeout the number of minutes this auction will time out
     * @param minBid   the new minimum bid
     * @param beneficiary the beneficiary of this auction, or null to keep the current one
     */
    public void open(long timeout, long minBid, Address beneficiary) {
        if (!isOpen && getCurrentTxSender() == this.beneficiary) {
            // only the current beneficiary can re-open the auction
            if(beneficiary!=null)
                this.beneficiary = beneficiary;
            this.timeout = getBlockTimestamp().addMinutes(timeout);
            highestBid = minBid;
            highestBidder = null;
            isOpen = true;
        }
    }

    /**
     * Bids will be handled by this function, which is called when transactions are received.
     */
    public void txReceived() {
        if (expired()) {
            // Send back this transaction funds
            refund();
            return;
        }

        newBid = getCurrentTxAmount() + ACTIVATION_FEE;
        if (newBid > highestBid) {
            // we have a new higher bid, return the previous one
            if (highestBidder != null) {
                sendAmount(highestBid - ACTIVATION_FEE, highestBidder);
            }
            highestBidder = getCurrentTxSender();
            highestBid = newBid;
        } else {
            // Bid too low, just send back
            refund();
        }
    }

    /**
     * Refunds the last received transaction
     */
    private void refund() {
        sendAmount(getCurrentTxAmount(), getCurrentTxSender());
    }

    public static void main(String[] args) throws Exception {
        // some initialization code to make things easier to debug
        Emulator emu = Emulator.getInstance();

        Address creator = Emulator.getInstance().getAddress("BENEFICIARY");
        emu.airDrop(creator, 1000 * Contract.ONE_BURST);
        Address auction = Emulator.getInstance().getAddress("AUCTION");
        emu.createConctract(creator, auction, AuctionNFT.class, Contract.ONE_BURST);

        emu.forgeBlock();

        new EmulatorWindow(AuctionNFT.class);
    }
}
