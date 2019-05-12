package bt;

import bt.compiler.Compiler;
import burst.kit.burst.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.ATResponse;
import burst.kit.entity.response.AccountATsResponse;
import burst.kit.entity.response.BroadcastTransactionResponse;
import burst.kit.entity.response.GenerateTransactionResponse;
import burst.kit.entity.response.SubmitNonceResponse;
import burst.kit.service.BurstNodeService;
import io.reactivex.Single;

/**
 * Utility functions for testing on-chain.
 * 
 * @author jjos
 */
public class TestUtil {

    public static final String PASSPHRASE  = "block talk, easy to use smart contracts for burst";
    public static final String PASSPHRASE2 = "block talk: easy to use smart contracts for burst";
    public static final String PASSPHRASE3 = "block talk! easy to use smart contracts for burst";
    public static final String NODE_LOCAL_TESTNET = "http://localhost:6876";
    public static final String NODE_AT_TESTNET = "http://at-testnet.burst-alliance.org:6876";
    public static final String NODE_TESTNET = "http://testnet.getburst.net:6876";

    static BurstNodeService bns = BurstNodeService.getInstance(NODE_LOCAL_TESTNET);
    static BurstCrypto bc = BurstCrypto.getInstance();

    public static void setNode(String node){
        bns = BurstNodeService.getInstance(node);
    }

    public static BroadcastTransactionResponse sendAmount(String passFrom, BurstAddress receiver, BurstValue value) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        return bns.generateTransaction(receiver, pubKeyFrom, value, BurstValue.fromBurst(0.1), 1440).flatMap(response -> {
            byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
            byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
            return bns.broadcastTransaction(signedTransactionBytes);
        }).blockingGet();
    }

    public static void forgeBlock(){
        forgeBlock(PASSPHRASE);
    }

    public static void forgeBlock(String pass) {
        Single<SubmitNonceResponse> submit = bns.submitNonce(pass, "0", null);
        SubmitNonceResponse r = submit.blockingGet();
        if(!r.getResult().equals("success"))
            System.err.println(r.getResult());
        try {
             Thread.sleep(200);
        } catch (InterruptedException e) {
             e.printStackTrace();
        }
    }

    public static ATResponse registerAT(Class<?> c, BurstValue activationFee) throws Exception {
        return registerAT(PASSPHRASE, c, activationFee, BurstValue.fromBurst(0.1));
    }

    public static ATResponse registerAT(String passphrase, Class<?> c, BurstValue activationFee, BurstValue fee) throws Exception {
        Compiler comp = new Compiler(c.getName());
        String name = c.getSimpleName() + System.currentTimeMillis();
        comp.compile();
        comp.link();

        int deadline = 1440; // 4 days (in blocks of 4 minutes)
        byte[] pubkey = bc.getPublicKey(passphrase);
        Single<GenerateTransactionResponse> createAT = bns.generateCreateATTransaction(pubkey, fee,
                deadline, name, name, new byte[0], comp.getCode().array(), new byte[0], 1, 1, 1,
                activationFee);

        createAT.flatMap(response -> {
            byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
            byte[] signedTransactionBytes = bc.signTransaction(passphrase, unsignedTransactionBytes);
            return bns.broadcastTransaction(signedTransactionBytes);
        }).blockingGet();

        forgeBlock();
        Thread.sleep(400);

        BurstAddress myAddress = bc.getBurstAddressFromPassphrase(passphrase);
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
