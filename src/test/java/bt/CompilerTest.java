package bt;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.sample.Forward;
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

import java.io.IOException;

import org.junit.BeforeClass;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available
 * for the tests to work.
 * 
 * @author jjos
 */
public class CompilerTest {

    static final String PASSPHRASE = "block talk, easy to use smart contracts for burst";
    static final String NODE = "http://localhost:6876";

    static BurstNodeService bns;
    static BurstCrypto bc;
    static BurstAddress myAddress;
    static byte[] myPubKey;

    public static void main(String[] args) throws Exception {
        CompilerTest t = new CompilerTest();
        t.setup();
        
        t.testForward();
    }

    @BeforeClass
    public void setup() {
        bns = BurstNodeService.getInstance(NODE);
        bc = BurstCrypto.getInstance();

        myAddress = bc.getBurstAddressFromPassphrase(PASSPHRASE);
        myPubKey = bc.getPublicKey(PASSPHRASE);

        // forge a fitst block to get some balance
        forgeBlock();
    }

    @Test
    public void testForward() throws IOException {
        BurstAddress address = BurstAddress.fromEither(Forward.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(address, BurstValue.fromBurst(1));
        forgeBlock();

        AccountResponse bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue balance = bmfAccount.getBalanceNQT();
        BurstValue actvFee = BurstValue.fromBurst(1);
        BurstValue amount = BurstValue.fromBurst(10);

        ATResponse at = registerAT(Forward.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(at.getAt(), amount);
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue newBalance = bmfAccount.getBalanceNQT();
        double result = newBalance.doubleValue() - balance.doubleValue() - actvFee.doubleValue();

        assertEquals("Value not forwarded", result, 0, 1e-3);
    }
    
    private BroadcastTransactionResponse sendAmount(BurstAddress receiver, BurstValue value){
        return bns.generateTransaction(receiver, myPubKey,
                value, BurstValue.fromBurst(0.1), 1440).flatMap(response -> {
                    byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
                    byte[] signedTransactionBytes = bc.signTransaction(PASSPHRASE, unsignedTransactionBytes);
                    return bns.broadcastTransaction(signedTransactionBytes);
                }).blockingGet();
    }

    private void forgeBlock() {
        Single<SubmitNonceResponse> submit = bns.submitNonce(PASSPHRASE, "0", null);
        SubmitNonceResponse r = submit.blockingGet();
        assertTrue(r.getResult(), r.getResult().equals("success"));
    }

    private ATResponse registerAT(Class<?> c, BurstValue activationFee) throws IOException {
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

        AccountATsResponse ats = bns.getAccountATs(myAddress).blockingGet();
        for(ATResponse ati : ats.getATs()){
            if(ati.getName().equals(name))
                return ati;
        }
        return null;
    }
}
