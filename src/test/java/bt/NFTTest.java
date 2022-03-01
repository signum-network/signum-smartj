package bt;

import signumj.entity.response.AT;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.sample.NFT2;

import static org.junit.Assert.*;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 * 
 * @author jjos
 */
public class NFTTest extends BT {

    public static void main(String[] args) throws Exception {
        NFTTest t = new NFTTest();

        t.testNFT();
    }

    @Test
    public void testNFT() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(NFT2.class);

        String name = NFT2.class.getSimpleName() + System.currentTimeMillis();
        SignumAddress creator = BT.getAddressFromPassphrase(BT.PASSPHRASE);

        BT.registerContract(BT.PASSPHRASE, comp, name, name, SignumValue.fromNQT(NFT2.ACTIVATION_FEE),
                SignumValue.fromSigna(0.1), 1000).blockingGet();
        BT.forgeBlock();

        AT contract = BT.findContract(creator, name);
        System.out.println(contract.getId().getID());

        long startBid = 1000 * Contract.ONE_BURST;
        int timeout = 40;

        // put the contract for auction
        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("putForAuction"), SignumValue.fromNQT(NFT2.ACTIVATION_FEE),
                SignumValue.fromSigna(0.1), 1000, startBid, timeout);
        BT.forgeBlock();
        BT.forgeBlock();

        long ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        long statusChain = BT.getContractFieldValue(contract, comp.getField("status").getAddress());
        long timeoutChain = BT.getContractFieldValue(contract, comp.getField("saleTimeout").getAddress());
        long highestBidderChain = BT.getContractFieldValue(contract, comp.getField("highestBidder").getAddress());
        long highestBidChain = BT.getContractFieldValue(contract, comp.getField("highestBid").getAddress());

        // convert back to a timeout in minutes
        timeoutChain = timeoutChain >> 32; // only the block part
        timeoutChain -= contract.getCreationHeight()+2; // timeout was set 2 blocks after creation
        timeoutChain *= 4; // blocks to minutes

        assertEquals(creator.getSignedLongId(), ownerChain);
        assertEquals(timeout, timeoutChain);
        assertEquals(NFT2.STATUS_FOR_AUCTION, statusChain);
        assertEquals(0, highestBidderChain);
        assertEquals(startBid, highestBidChain);

        SignumAddress bidder = BT.getAddressFromPassphrase(BT.PASSPHRASE2);
        SignumAddress bidder2 = BT.getAddressFromPassphrase(BT.PASSPHRASE3);
        BT.forgeBlock(BT.PASSPHRASE2, 100);
        BT.forgeBlock(BT.PASSPHRASE3, 100);

        // send a bid short on amount
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(startBid));
        BT.forgeBlock();
        BT.forgeBlock();
        highestBidderChain = BT.getContractFieldValue(contract, comp.getField("highestBidder").getAddress());
        highestBidChain = BT.getContractFieldValue(contract, comp.getField("highestBid").getAddress());
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        // no changes are expected
        assertEquals(0, highestBidderChain);
        assertEquals(startBid, highestBidChain);
        assertEquals(creator.getSignedLongId(), ownerChain);

        // send a higher bid
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(startBid*2 + NFT2.ACTIVATION_FEE));
        BT.forgeBlock();
        BT.forgeBlock();
        long debug = BT.getContractFieldValue(contract, comp.getField("debug").getAddress());
        highestBidderChain = BT.getContractFieldValue(contract, comp.getField("highestBidder").getAddress());
        highestBidChain = BT.getContractFieldValue(contract, comp.getField("highestBid").getAddress());
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        // there should be updates here
        assertEquals(bidder.getSignedLongId(), highestBidderChain);
        assertEquals(startBid*2, highestBidChain);
        assertEquals(creator.getSignedLongId(), ownerChain);
        
        // send an even higher bid from another address
        BT.sendAmount(BT.PASSPHRASE3, contract.getId(), SignumValue.fromNQT(startBid*3 + NFT2.ACTIVATION_FEE));
        BT.forgeBlock();
        BT.forgeBlock();
        highestBidderChain = BT.getContractFieldValue(contract, comp.getField("highestBidder").getAddress());
        highestBidChain = BT.getContractFieldValue(contract, comp.getField("highestBid").getAddress());
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        // there should be updates here
        assertEquals(bidder2.getSignedLongId(), highestBidderChain);
        assertEquals(startBid*3, highestBidChain);
        assertEquals(creator.getSignedLongId(), ownerChain);
    }
}
