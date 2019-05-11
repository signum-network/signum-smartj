package bt;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.sample.Forward;
import bt.sample.ForwardMin;
import bt.sample.TipThanks;
import burst.kit.burst.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.ATResponse;
import burst.kit.entity.response.AccountATsResponse;
import burst.kit.entity.response.AccountResponse;
import burst.kit.entity.response.BroadcastTransactionResponse;
import burst.kit.entity.response.GenerateTransactionResponse;
import burst.kit.entity.response.SubmitNonceResponse;
import burst.kit.service.BurstNodeService;
import io.reactivex.Single;

import static org.junit.Assert.*;

import org.junit.BeforeClass;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 * 
 * @author jjos
 */
public class CompilerTest {

    static final String PASSPHRASE  = "block talk, easy to use smart contracts for burst";
    static final String PASSPHRASE2 = "block talk: easy to use smart contracts for burst";
    static final String PASSPHRASE3 = "block talk! easy to use smart contracts for burst";
    static final String NODE = "http://localhost:6876";

    static BurstNodeService bns;
    static BurstCrypto bc;
    static BurstAddress myAddress;

    public static void main(String[] args) throws Exception {
        CompilerTest t = new CompilerTest();
        t.setup();

        // t.testForward();
        //t.testForwardMin();
        t.testTipThanks();
    }

    @BeforeClass
    public static void setup() {
        bns = BurstNodeService.getInstance(NODE);
        bc = BurstCrypto.getInstance();

        myAddress = bc.getBurstAddressFromPassphrase(PASSPHRASE);

        // forge a fitst block to get some balance
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

        ATResponse at = registerAT(Forward.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getAt(), amount);
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue newBalance = bmfAccount.getUnconfirmedBalanceNQT();
        double result = newBalance.doubleValue() - balance.doubleValue() - actvFee.doubleValue();

        assertEquals("Value not forwarded", result, 0, 1e-3);
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

        ATResponse at = registerAT(ForwardMin.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getAt(), BurstValue.fromPlanck((long) amount));
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue newBalance = bmfAccount.getUnconfirmedBalanceNQT();
        double result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value forwarded while it should not", result < amount);

        sendAmount(PASSPHRASE, at.getAt(), BurstValue.fromPlanck((long) amount));
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        newBalance = bmfAccount.getUnconfirmedBalanceNQT();
        result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value not forwarded as it should", result*Contract.ONE_BURST > amount);
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

        ATResponse at = registerAT(TipThanks.class, actvFee);
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
        assertTrue("Value not forwarded as it should", result*Contract.ONE_BURST > amount);
    }

    private BroadcastTransactionResponse sendAmount(String passFrom, BurstAddress receiver, BurstValue value) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        return bns.generateTransaction(receiver, pubKeyFrom, value, BurstValue.fromBurst(0.1), 1440).flatMap(response -> {
            byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
            byte[] signedTransactionBytes = bc.signTransaction(PASSPHRASE, unsignedTransactionBytes);
            return bns.broadcastTransaction(signedTransactionBytes);
        }).blockingGet();
    }

    private static void forgeBlock(){
        forgeBlock(PASSPHRASE);
    }

    private static void forgeBlock(String pass) {
        Single<SubmitNonceResponse> submit = bns.submitNonce(pass, "0", null);
        SubmitNonceResponse r = submit.blockingGet();
        assertTrue(r.getResult(), r.getResult().equals("success"));
        try {
             Thread.sleep(200);
        } catch (InterruptedException e) {
             e.printStackTrace();
        }
    }

    private ATResponse registerAT(Class<?> c, BurstValue activationFee) throws Exception {
        Compiler comp = new Compiler(c.getName());
        String name = c.getSimpleName() + System.currentTimeMillis();
        comp.compile();
        comp.link();

        int deadline = 1440; // 4 days (in blocks of 4 minutes)
        byte[] pubkey = bc.getPublicKey(PASSPHRASE);
        Single<GenerateTransactionResponse> createAT = bns.generateCreateATTransaction(pubkey, BurstValue.fromBurst(0.1),
                deadline, name, name, new byte[0], comp.getCode().array(), new byte[0], 1, 1, 1,
                activationFee);

        createAT.flatMap(response -> {
            byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
            byte[] signedTransactionBytes = bc.signTransaction(PASSPHRASE, unsignedTransactionBytes);
            return bns.broadcastTransaction(signedTransactionBytes);
        }).blockingGet();

        forgeBlock();
        Thread.sleep(400);

        AccountATsResponse ats = bns.getAccountATs(myAddress).blockingGet();
        for(ATResponse ati : ats.getATs()){
            if(ati.getName().equals(name))
                return ati;
            System.out.println(ati.getName());
        }
        System.out.println("AT not found: " + name);
        return null;
    }
}
