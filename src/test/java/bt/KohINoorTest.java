package bt;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.sample.KohINoor;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;

import static org.junit.Assert.*;

import java.util.ArrayList;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 * 
 * @author jjos
 */
public class KohINoorTest extends BT {

    public static void main(String[] args) throws Exception {
        KohINoorTest t = new KohINoorTest();

        t.testTheOne();
    }

    @Test
    public void testTheOne() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(KohINoor.class);

        String name = "KohINoor" + System.currentTimeMillis();
        SignumAddress creator = BT.getAddressFromPassphrase(BT.PASSPHRASE);

        AT contract = BT.registerContract(comp, name, SignumValue.fromNQT(KohINoor.ACTIVATION_FEE));
        System.out.println(contract.getId().getID());

        long price = KohINoor.INITIAL_PRICE;

        // initialize the contract
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromNQT(KohINoor.ACTIVATION_FEE));
        BT.forgeBlock();
        BT.forgeBlock();

        long ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        long priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress());

        assertEquals(creator.getSignedLongId(), ownerChain);
        assertEquals(price, priceChain);
        assertTrue(BT.getContractBalance(contract).doubleValue()*Contract.ONE_BURST < KohINoor.ACTIVATION_FEE);

        SignumAddress bidder = BT.getAddressFromPassphrase(BT.PASSPHRASE2);
        SignumAddress bidder2 = BT.getAddressFromPassphrase(BT.PASSPHRASE3);
        BT.forgeBlock(BT.PASSPHRASE2, 100);
        BT.forgeBlock(BT.PASSPHRASE3, 100);

        // send a short amount
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(price / 2));
        BT.forgeBlock();
        BT.forgeBlock();
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress());
        // no changes are expected
        assertEquals(creator.getSignedLongId(), ownerChain);
        assertEquals(price, priceChain);
        assertTrue(BT.getContractBalance(contract).doubleValue()*Contract.ONE_BURST < KohINoor.ACTIVATION_FEE);

        // send the asked amount
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(price));
        BT.forgeBlock();
        BT.forgeBlock();
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress());
        // check the changes
        price += 10 * price / 100;
        assertEquals(bidder.getSignedLongId(), ownerChain);
        assertEquals(price, priceChain);
        assertTrue(BT.getContractBalance(contract).doubleValue()*Contract.ONE_BURST < KohINoor.ACTIVATION_FEE);

        // send the asked amount, another buyer
        BT.sendAmount(BT.PASSPHRASE3, contract.getId(), SignumValue.fromNQT(price));
        BT.forgeBlock();
        BT.forgeBlock();
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress());
        // check the changes
        price += 10 * price / 100;
        assertEquals(bidder2.getSignedLongId(), ownerChain);
        assertEquals(price, priceChain);
        assertTrue(BT.getContractBalance(contract).doubleValue()*Contract.ONE_BURST < KohINoor.ACTIVATION_FEE);

        // send the asked amount, but three different buyers
        BT.sendAmount(BT.PASSPHRASE3, contract.getId(), SignumValue.fromNQT(price));
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromNQT(price));
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(price));
        BT.forgeBlock();
        BT.forgeBlock();
        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress());
        assertTrue(BT.getContractBalance(contract).doubleValue()*Contract.ONE_BURST < KohINoor.ACTIVATION_FEE);

        // check the changes
        price += 10 * price / 100;
        //assertEquals(bidder.getSignedLongId(), ownerChain);
        assertEquals(price, priceChain);


        // test with a lot of simultaneous buyers
        ArrayList<String> buyers = new ArrayList<>();
        SignumAddress lastBuyer = null;
        for (int i = 0; i < 10; i++) {
            String pass = String.valueOf(i);
            SignumAddress buyer = BT.getAddressFromPassphrase(pass);
            BT.sendAmount(PASSPHRASE, buyer, SignumValue.fromNQT(price*2), SignumValue.fromSigna(10));
            buyers.add(pass);
            lastBuyer = buyer;
        }
        forgeBlock();
        forgeBlock();

        int i = 0;
        for (String buyer : buyers) {
            BT.sendAmount(buyer, contract.getId(), SignumValue.fromNQT(price), SignumValue.fromSigna(0.1));
        }
        forgeBlock();
        forgeBlock();

        ownerChain = BT.getContractFieldValue(contract, comp.getField("owner").getAddress());
        priceChain = BT.getContractFieldValue(contract, comp.getField("price").getAddress());
        assertTrue(BT.getContractBalance(contract).doubleValue()*Contract.ONE_BURST < KohINoor.ACTIVATION_FEE);

        for (String buyer : buyers) {
            SignumAddress ad = BT.getAddressFromPassphrase(buyer);
            if(ad.getSignedLongId() == ownerChain){
                System.out.println("Got the token, buyer: " + buyer);
                break;
            }
        }

        // check the changes
        price += 10 * price / 100;
        assertEquals(price, priceChain);
    }
}
