package bt;

import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.response.AT;
import burst.kit.entity.response.Account;
import org.junit.Test;

import bt.compiler.Compiler;
import bt.sample.Forward;
import bt.sample.ForwardMin;
import bt.sample.OddsGame;
import bt.sample.TXCounter;
import bt.sample.TipThanks;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;

import static org.junit.Assert.*;

import org.junit.BeforeClass;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 * 
 * @author jjos
 */
public class CompilerTest extends BT {

    public static void main(String[] args) throws Exception {
        CompilerTest t = new CompilerTest();
        t.setup();

        t.testForward();
        // t.testForwardMin();
        // t.testTipThanks();
        // t.testOdds();
        // t.testLocalVar();
        t.testCounter();
    }

    @BeforeClass
    public static void setup() {
        // forge a first block to get some balance
        forgeBlock();
    }

    @Test
    public void testOdds() throws Exception {

        // Send some burst for the players
        forgeBlock(PASSPHRASE, 500);
        BurstAddress player1 = bc.getBurstAddressFromPassphrase(PASSPHRASE2);
        BurstAddress player2 = bc.getBurstAddressFromPassphrase(PASSPHRASE3);
        sendAmount(PASSPHRASE, player1, BurstValue.fromBurst(1000)).blockingGet();
        sendAmount(PASSPHRASE, player2, BurstValue.fromBurst(1000)).blockingGet();
        forgeBlock(PASSPHRASE, 500);

        BurstValue actvFee = BurstValue.fromBurst(10);
        BurstValue amount = BurstValue.fromBurst(100);

        AT at = registerContract(OddsGame.class, actvFee);
        assertNotNull("AT could not be registered", at);

        // Fill the contract with 3 times the max payment value
        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck(OddsGame.MAX_PAYMENT * 3));
        forgeBlock();

        // 1 bet each
        sendAmount(PASSPHRASE, at.getId(), amount);
        sendAmount(PASSPHRASE2, at.getId(), amount);
        sendAmount(PASSPHRASE3, at.getId(), amount);
        forgeBlock();
        forgeBlock();

        // send some just to run the code
        sendAmount(PASSPHRASE2, at.getId(), amount);
        forgeBlock();
        forgeBlock();
    }

    @Test
    public void testForward() throws Exception {
        BurstAddress address = BurstAddress.fromRs(Forward.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, BurstValue.fromPlanck(1)).blockingGet();
        forgeBlock();

        Account bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue balance = bmfAccount.getUnconfirmedBalance();
        BurstValue actvFee = BurstValue.fromBurst(1);
        BurstValue amount = BurstValue.fromBurst(10);

        AT at = registerContract(Forward.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getId(), amount);
        forgeBlock();
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue newBalance = bmfAccount.getUnconfirmedBalance();
        double result = newBalance.doubleValue() - balance.doubleValue() - actvFee.doubleValue();

        assertTrue("Value not forwarded", result > actvFee.doubleValue());
    }

    @Test
    public void testForwardMin() throws Exception {
        BurstAddress address = BurstAddress.fromEither(ForwardMin.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, BurstValue.fromPlanck(1));
        forgeBlock();

        Account bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue balance = bmfAccount.getUnconfirmedBalance();
        BurstValue actvFee = BurstValue.fromBurst(1);
        double amount = ForwardMin.MIN_AMOUNT * 0.8;

        AT at = registerContract(ForwardMin.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck((long) amount));
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue newBalance = bmfAccount.getUnconfirmedBalance();
        double result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value forwarded while it should not", result < amount);

        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck((long) amount));
        forgeBlock();
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        newBalance = bmfAccount.getUnconfirmedBalance();
        result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value not forwarded as it should", result * Contract.ONE_BURST > amount);
    }

    @Test
    public void testTipThanks() throws Exception {
        BurstAddress address = BurstAddress.fromEither(TipThanks.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, BurstValue.fromBurst(100));
        forgeBlock();

        Account benefAccout = bns.getAccount(address).blockingGet();
        BurstValue balance = benefAccout.getUnconfirmedBalance();
        BurstValue actvFee = BurstValue.fromBurst(1);
        double amount = TipThanks.MIN_AMOUNT * 0.8;

        AT at = registerContract(TipThanks.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck((long) amount));
        forgeBlock();

        benefAccout = bns.getAccount(address).blockingGet();
        BurstValue newBalance = benefAccout.getUnconfirmedBalance();
        double result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value forwarded while it should not", result < amount);

        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck((long) amount));
        forgeBlock();
        forgeBlock();

        benefAccout = bns.getAccount(address).blockingGet();
        newBalance = benefAccout.getUnconfirmedBalance();
        result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value not forwarded as it should", result * Contract.ONE_BURST > amount);
    }

    @Test
    public void testLocalVar() throws Exception {
        BT.forgeBlock();
		Compiler comp = BT.compileContract(LocalVar.class);

		String name = LocalVar.class.getSimpleName() + System.currentTimeMillis();
		BurstAddress creator = BurstCrypto.getInstance().getBurstAddressFromPassphrase(BT.PASSPHRASE);

		BT.registerContract(BT.PASSPHRASE, comp, name, name, BurstValue.fromPlanck(LocalVar.FEE), BurstValue.fromBurst(0.1),
				1000).blockingGet();
		BT.forgeBlock();

		AT contract = BT.findContract(creator, name);

        BurstValue valueSent = BurstValue.fromBurst(10);
		BT.sendAmount(BT.PASSPHRASE, contract.getId(), valueSent).blockingGet();
		BT.forgeBlock();
		BT.forgeBlock();

        assertEquals(valueSent.longValue()*Contract.ONE_BURST,
            BT.getContractFieldValue(contract, comp.getField("amountNoFee").getAddress()));

		long value = 512;
		BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("setValue"), BurstValue.fromBurst(1),
				BurstValue.fromBurst(0.1), 1000, value).blockingGet();
		BT.forgeBlock();
		BT.forgeBlock();

		long valueChain = BT.getContractFieldValue(contract, comp.getField("valueTimes2").getAddress());

        assertEquals(value*2, valueChain);
    }


    public void testCounter() throws Exception {
        BT.forgeBlock();

        long ntx, nblocks, address;

        Compiler compiled = BT.compileContract(TXCounter.class);
        AT contract = BT.registerContract(compiled, TXCounter.class.getSimpleName() + System.currentTimeMillis(),
            BurstValue.fromBurst(10));
        System.out.println(contract.getId().getSignedLongId());
        
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(20)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());
        assertEquals(1, ntx);
        assertEquals(1, nblocks);
        assertEquals(BT.getBurstAddressFromPassphrase(PASSPHRASE).getSignedLongId(), address);

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(20), BurstValue.fromBurst(0.1)).blockingGet();
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), BurstValue.fromBurst(20), BurstValue.fromBurst(1)).blockingGet();
        BT.sendAmount(BT.PASSPHRASE3, contract.getId(), BurstValue.fromBurst(20), BurstValue.fromBurst(1)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());
        
        assertEquals(4, ntx);
        assertEquals(2, nblocks);
    }
}
