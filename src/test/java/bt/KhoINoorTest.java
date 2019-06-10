package bt;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.sample.KhoINoor;
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
public class KhoINoorTest extends BT {

    public static void main(String[] args) throws Exception {
        KhoINoorTest t = new KhoINoorTest();
        t.setup();

        t.testTheOne();
    }

    @BeforeClass
    public void setup() {
        // forge a fitst block to get some balance
        forgeBlock();
    }

    @Test
    public void testTheOne() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(KhoINoor.class);

        String name = "KhoINoor" + System.currentTimeMillis();
        BurstAddress creator = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE);

        BT.registerContract(BT.PASSPHRASE, comp, name, name, BurstValue.fromPlanck(KhoINoor.ACTIVATION_FEE),
                BurstValue.fromBurst(0.1), 1000).blockingGet();
        BT.forgeBlock();

        ATResponse contract = BT.findContract(creator, name);
        System.out.println(contract.getAt().getID());

        long price = KhoINoor.INITIAL_PRICE;

        // initialize the contract
        BT.sendAmount(BT.PASSPHRASE, contract.getAt(), BurstValue.fromPlanck(KhoINoor.ACTIVATION_FEE)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        long creatorChain = BT.getContractFieldValue(contract, comp.getField("creator").getAddress(), true);
        long ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress(), true);
        long priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress(), true);

        assertEquals(creator.getBurstID().getSignedLongId(), ownerChain);
        assertEquals(creator.getBurstID().getSignedLongId(), creatorChain);
        assertEquals(price, priceChain);

        BurstAddress bidder = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2);
        BurstAddress bidder2 = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE3);
        BT.forgeBlock(BT.PASSPHRASE2, 100);
        BT.forgeBlock(BT.PASSPHRASE3, 100);

        // send a short amount
        BT.sendAmount(BT.PASSPHRASE2, contract.getAt(), BurstValue.fromPlanck(price / 2)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        creatorChain = BT.getContractFieldValue(contract, comp.getField("creator").getAddress(), true);
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress(), true);
        priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress(), true);
        // no changes are expected
        assertEquals(creator.getBurstID().getSignedLongId(), ownerChain);
        assertEquals(creator.getBurstID().getSignedLongId(), creatorChain);
        assertEquals(price, priceChain);

        // send the asked amount
        BT.sendAmount(BT.PASSPHRASE2, contract.getAt(), BurstValue.fromPlanck(price)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        creatorChain = BT.getContractFieldValue(contract, comp.getField("creator").getAddress(), true);
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress(), true);
        priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress(), true);
        // check the changes
        price += 10 * price / 100;
        assertEquals(bidder.getBurstID().getSignedLongId(), ownerChain);
        assertEquals(creator.getBurstID().getSignedLongId(), creatorChain);
        assertEquals(price, priceChain);

        // send the asked amount, another buyer
        BT.sendAmount(BT.PASSPHRASE3, contract.getAt(), BurstValue.fromPlanck(price)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        creatorChain = BT.getContractFieldValue(contract, comp.getField("creator").getAddress(), true);
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress(), true);
        priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress(), true);
        // check the changes
        price += 10 * price / 100;
        assertEquals(bidder2.getBurstID().getSignedLongId(), ownerChain);
        assertEquals(creator.getBurstID().getSignedLongId(), creatorChain);
        assertEquals(price, priceChain);
    }
}
