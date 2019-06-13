package bt;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.sample.Forward;
import bt.sample.ForwardMin;
import bt.sample.OddsGame;
import bt.sample.TXCounter;
import bt.sample.TipThanks;
import burst.kit.burst.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.ATResponse;
import burst.kit.entity.response.AccountResponse;

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

        // t.testForward();
        // t.testForwardMin();
        // t.testTipThanks();
        // t.testOdds();
        // t.testLocalVar();
        t.testCounter();
    }

    @BeforeClass
    public void setup() {
        // forge a fitst block to get some balance
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

        ATResponse at = registerContract(OddsGame.class, actvFee);
        assertNotNull("AT could not be registered", at);

        // Fill the contract with 3 times the max payment value
        sendAmount(PASSPHRASE, at.getAt(), BurstValue.fromPlanck(OddsGame.MAX_PAYMENT * 3));
        forgeBlock();

        // 1 bet each
        sendAmount(PASSPHRASE, at.getAt(), amount);
        sendAmount(PASSPHRASE2, at.getAt(), amount);
        sendAmount(PASSPHRASE3, at.getAt(), amount);
        forgeBlock();
        forgeBlock();

        // send some just to run the code
        sendAmount(PASSPHRASE2, at.getAt(), amount);
        forgeBlock();
        forgeBlock();
    }

    @Test
    public void testForward() throws Exception {
        BurstAddress address = BurstAddress.fromEither(Forward.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, BurstValue.fromPlanck(1));
        forgeBlock();

        AccountResponse bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue balance = bmfAccount.getUnconfirmedBalanceNQT();
        BurstValue actvFee = BurstValue.fromBurst(1);
        BurstValue amount = BurstValue.fromBurst(10);

        ATResponse at = registerContract(Forward.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getAt(), amount);
        forgeBlock();
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue newBalance = bmfAccount.getUnconfirmedBalanceNQT();
        double result = newBalance.doubleValue() - balance.doubleValue() - actvFee.doubleValue();

        assertTrue("Value not forwarded", result > actvFee.doubleValue());
    }

    @Test
    public void testForwardMin() throws Exception {
        BurstAddress address = BurstAddress.fromEither(ForwardMin.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, BurstValue.fromPlanck(1));
        forgeBlock();

        AccountResponse bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue balance = bmfAccount.getUnconfirmedBalanceNQT();
        BurstValue actvFee = BurstValue.fromBurst(1);
        double amount = ForwardMin.MIN_AMOUNT * 0.8;

        ATResponse at = registerContract(ForwardMin.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getAt(), BurstValue.fromPlanck((long) amount));
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue newBalance = bmfAccount.getUnconfirmedBalanceNQT();
        double result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value forwarded while it should not", result < amount);

        sendAmount(PASSPHRASE, at.getAt(), BurstValue.fromPlanck((long) amount));
        forgeBlock();
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        newBalance = bmfAccount.getUnconfirmedBalanceNQT();
        result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value not forwarded as it should", result * Contract.ONE_BURST > amount);
    }

    @Test
    public void testTipThanks() throws Exception {
        BurstAddress address = BurstAddress.fromEither(TipThanks.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, BurstValue.fromBurst(100));
        forgeBlock();

        AccountResponse benefAccout = bns.getAccount(address).blockingGet();
        BurstValue balance = benefAccout.getUnconfirmedBalanceNQT();
        BurstValue actvFee = BurstValue.fromBurst(1);
        double amount = TipThanks.MIN_AMOUNT * 0.8;

        ATResponse at = registerContract(TipThanks.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getAt(), BurstValue.fromPlanck((long) amount));
        forgeBlock();

        benefAccout = bns.getAccount(address).blockingGet();
        BurstValue newBalance = benefAccout.getUnconfirmedBalanceNQT();
        double result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value forwarded while it should not", result < amount);

        sendAmount(PASSPHRASE, at.getAt(), BurstValue.fromPlanck((long) amount));
        forgeBlock();
        forgeBlock();

        benefAccout = bns.getAccount(address).blockingGet();
        newBalance = benefAccout.getUnconfirmedBalanceNQT();
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

		ATResponse contract = BT.findContract(creator, name);

        BurstValue valueSent = BurstValue.fromBurst(10);
		BT.sendAmount(BT.PASSPHRASE, contract.getAt(), valueSent).blockingGet();
		BT.forgeBlock();
		BT.forgeBlock();

        assertEquals(valueSent.longValue()*Contract.ONE_BURST,
            BT.getContractFieldValue(contract, comp.getField("amountNoFee").getAddress()));

		long value = 512;
		BT.callMethod(BT.PASSPHRASE, contract.getAt(), comp.getMethod("setValue"), BurstValue.fromBurst(1),
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
        ATResponse contract = BT.registerContract(compiled, TXCounter.class.getSimpleName() + System.currentTimeMillis(),
            BurstValue.fromBurst(10));
        System.out.println(contract.getAt().getBurstID().getSignedLongId());
        
        BT.sendAmount(BT.PASSPHRASE, contract.getAt(), BurstValue.fromBurst(20)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        assertEquals(1, ntx);
        assertEquals(1, nblocks);

        BT.sendAmount(BT.PASSPHRASE, contract.getAt(), BurstValue.fromBurst(20), BurstValue.fromBurst(0.1)).blockingGet();
        BT.sendAmount(BT.PASSPHRASE2, contract.getAt(), BurstValue.fromBurst(20), BurstValue.fromBurst(1)).blockingGet();
        BT.sendAmount(BT.PASSPHRASE3, contract.getAt(), BurstValue.fromBurst(20), BurstValue.fromBurst(1)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());
        
        assertEquals(4, ntx);
        assertEquals(2, nblocks);
        assertEquals(BT.getBurstAddressFromPassphrase(PASSPHRASE).getBurstID().getSignedLongId(), address);
    }
}
