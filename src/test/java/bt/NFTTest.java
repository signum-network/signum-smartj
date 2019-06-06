package bt;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.sample.NFT2;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.ATResponse;

import static org.junit.Assert.*;

import org.junit.BeforeClass;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 * 
 * @author jjos
 */
public class NFTTest extends BT {

    public static void main(String[] args) throws Exception {
        NFTTest t = new NFTTest();
        t.setup();

        t.testNFT();
    }

    @BeforeClass
    public void setup() {
        // forge a fitst block to get some balance
        forgeBlock();
    }

    @Test
    public void testNFT() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(NFT2.class);

        String name = NFT2.class.getSimpleName() + System.currentTimeMillis();
        BurstAddress creator = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE);

        BT.registerContract(BT.PASSPHRASE, comp, name, name, BurstValue.fromPlanck(LocalVar.FEE),
                BurstValue.fromBurst(0.1), 1000).blockingGet();
        BT.forgeBlock();

        ATResponse contract = BT.findContract(creator, name);
        System.out.println(contract.getAt().getID());

        long startBid = 1000 * Contract.ONE_BURST;
        int timeout = 40;

        // put the contract for auction
        BT.callMethod(BT.PASSPHRASE, contract.getAt(), comp.getMethod("putForAuction"), BurstValue.fromBurst(1),
                BurstValue.fromBurst(0.1), 1000, startBid, timeout).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        long ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress(), true);
        long statusChain = BT.getContractFieldValue(contract, comp.getField("status").getAddress(), true);
        long timeoutChain = BT.getContractFieldValue(contract, comp.getField("saleTimeout").getAddress(), true);
        long highestBidderChain = BT.getContractFieldValue(contract, comp.getField("highestBidder").getAddress(), true);
        long highestBidChain = BT.getContractFieldValue(contract, comp.getField("highestBid").getAddress(), true);

        // convert back to a timeout in minutes
        timeoutChain = timeoutChain >> 32; // only the block part
        timeoutChain -= contract.getCreationBlock()+2; // timeout was set 2 blocks after creation
        timeoutChain *= 4; // blocks to minutes

        assertEquals(creator.getBurstID().getSignedLongId(), ownerChain);
        assertEquals(timeout, timeoutChain);
        assertEquals(NFT2.STATUS_FOR_AUCTION, statusChain);
        assertEquals(0, highestBidderChain);
        assertEquals(startBid, highestBidChain);

        BurstAddress bidder = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2);

        // send a bid short on amount
        BT.forgeBlock(BT.PASSPHRASE2, 100);
        BT.sendAmount(BT.PASSPHRASE2, contract.getAt(), BurstValue.fromPlanck(startBid)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        highestBidderChain = BT.getContractFieldValue(contract, comp.getField("highestBidder").getAddress(), true);
        highestBidChain = BT.getContractFieldValue(contract, comp.getField("highestBid").getAddress(), true);
        // no changes are expected
        assertEquals(0, highestBidderChain);
        assertEquals(startBid, highestBidChain);

        // send a higher bid
        BT.sendAmount(BT.PASSPHRASE2, contract.getAt(), BurstValue.fromPlanck(startBid*2)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        highestBidderChain = BT.getContractFieldValue(contract, comp.getField("highestBidder").getAddress(), true);
        highestBidChain = BT.getContractFieldValue(contract, comp.getField("highestBid").getAddress(), true);
        // there should be updates here
        assertEquals(bidder.getBurstID().getSignedLongId(), highestBidderChain);
        assertEquals(startBid, highestBidChain);
        

    }
}
