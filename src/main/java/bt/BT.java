package bt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;

import bt.compiler.Compiler;
import bt.compiler.Field;
import bt.compiler.Method;
import burst.kit.burst.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.HexStringByteArray;
import burst.kit.entity.response.ATResponse;
import burst.kit.entity.response.AccountATsResponse;
import burst.kit.entity.response.BroadcastTransactionResponse;
import burst.kit.entity.response.GenerateTransactionResponse;
import burst.kit.entity.response.SubmitNonceResponse;
import burst.kit.service.BurstNodeService;
import io.reactivex.Single;

/**
 * BlockTalk Utility functions for handling on-chain contract interactions.
 * 
 * Passphrases never leave the local application, messages are signed locally.
 * 
 * @author jjos
 */
public class BT {

    public static final String PASSPHRASE = "block talk, easy to use smart contracts for burst";
    public static final String PASSPHRASE2 = "block talk: easy to use smart contracts for burst";
    public static final String PASSPHRASE3 = "block talk! easy to use smart contracts for burst";
    public static final String NODE_LOCAL_TESTNET = "http://localhost:6876";
    public static final String NODE_AT_TESTNET = "http://at-testnet.burst-alliance.org:6876";
    public static final String NODE_TESTNET = "http://testnet.getburst.net:6876";

    static BurstNodeService bns = BurstNodeService.getInstance(NODE_LOCAL_TESTNET);
    static BurstCrypto bc = BurstCrypto.getInstance();

    /**
     * Sets the node address, by default localhost with testnet port 6876 is used.
     * 
     * @param nodeAddress the node address including port number
     */
    public static void setNodeAddress(String nodeAddress) {
        bns = BurstNodeService.getInstance(nodeAddress);
    }

    /**
     * @return the BURST node service object
     */
    public static BurstNodeService getNode() {
        return bns;
    }

    /**
     * Return the compiled version of a Contract.
     * 
     * Using this compiled version the user can check for errors
     * {@link Compiler#getErrors()}, can get the methods with
     * {@link Compiler#getMethods()}.
     * 
     * @param contractClass
     * @return
     * @throws IOException
     */
    public static Compiler compileContract(Class<? extends Contract> contractClass) throws IOException {
        Compiler comp = new Compiler(contractClass);
        comp.compile();
        comp.link();
        return comp;
    }

    /**
     * Call a method on the given contract address.
     */
    public static Single<BroadcastTransactionResponse> callMethod(String passFrom, BurstAddress contractAddress,
            Method method, BurstValue value, BurstValue fee, int deadline, Object... args) {

        ByteBuffer b = ByteBuffer.allocate(32);
        b.order(ByteOrder.LITTLE_ENDIAN);

        // Start with the method hash
        b.putLong(method.getHash());

        // Followed by up to 3 arguments
        int nargs = 0;
        for (Object arg : args) {
            nargs++;
            if (nargs > 3)
                throw new InvalidParameterException("Maximum number of parameters is currently 3");

            if (arg instanceof Boolean)
                b.putLong(((Boolean) arg == true) ? 1 : 0);
            else if (arg instanceof Integer)
                b.putLong((Integer) arg);
            else if (arg instanceof Long)
                b.putLong((Long) arg);
            else if (arg instanceof Address)
                b.putLong(((Address) arg).id);
            else if (arg instanceof Timestamp)
                b.putLong(((Timestamp) arg).value);
            else
                throw new InvalidParameterException("Unsupported argument type: " + arg.getClass().getName());
        }
        if (nargs != method.getNArgs()) {
            throw new InvalidParameterException(
                    "Expecting " + method.getNArgs() + " but received " + nargs + " parameters");
        }

        return sendMessage(passFrom, contractAddress, value, fee, deadline, b.array());
    }

    public static Single<BroadcastTransactionResponse> sendAmount(String passFrom, BurstAddress receiver,
            BurstValue value) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        return bns.generateTransaction(receiver, pubKeyFrom, value, BurstValue.fromBurst(0.1), 1440)
                .flatMap(response -> {
                    byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
                    byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
                    return bns.broadcastTransaction(signedTransactionBytes);
                });
    }

    public static Single<BroadcastTransactionResponse> sendMessage(String passFrom, BurstAddress receiver,
            BurstValue value, BurstValue fee, int deadline, String msg) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        return bns.generateTransactionWithMessage(receiver, pubKeyFrom, value, fee, deadline, msg).flatMap(response -> {
            byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
            byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
            return bns.broadcastTransaction(signedTransactionBytes);
        });
    }

    public static Single<BroadcastTransactionResponse> sendMessage(String passFrom, BurstAddress receiver,
            BurstValue value, BurstValue fee, int deadline, byte[] msg) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        return bns.generateTransactionWithMessage(receiver, pubKeyFrom, value, fee, deadline, msg).flatMap(response -> {
            byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
            byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
            return bns.broadcastTransaction(signedTransactionBytes);
        });
    }

    /**
     * Forge block by mock mining, just for testing purposes.
     */
    public static void forgeBlock() {
        forgeBlock(PASSPHRASE, 1000);
    }

    /**
     * Forge block by mock mining for the given passphrase followed by a sleep with
     * given milliseconds.
     * 
     * Just for testing purposes.
     */
    public static void forgeBlock(String pass, int millis) {
        Single<SubmitNonceResponse> submit = bns.submitNonce(pass, "0", null);
        SubmitNonceResponse r = submit.blockingGet();
        if (!r.getResult().equals("success"))
            System.err.println(r.getResult());
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Register the given contract with the given activation fee.
     */
    public static ATResponse registerContract(Class<? extends Contract> contract, BurstValue activationFee)
            throws Exception {
        Compiler compiledContract = compileContract(contract);

        String name = contract.getSimpleName() + System.currentTimeMillis();

        registerContract(PASSPHRASE, compiledContract, name, contract.getSimpleName(), activationFee,
                BurstValue.fromBurst(0.1), 1440).blockingGet();
        forgeBlock();

        return findContract(bc.getBurstAddressFromPassphrase(PASSPHRASE), name);
    }

    /**
     * Register the given contract with the given activation fee and paying the
     * given fee.
     * 
     * @param passphrase
     * @param compiledContract
     * @param name
     * @param description
     * @param activationFee
     * @param fee
     * @param deadline         in blocks
     * 
     * @return the response
     * @throws Exception
     */
    public static Single<BroadcastTransactionResponse> registerContract(String passphrase, Compiler compiledContract,
            String name, String descrition, BurstValue activationFee, BurstValue fee, int deadline) throws Exception {

        byte[] pubkey = bc.getPublicKey(passphrase);
        Single<GenerateTransactionResponse> createAT = bns.generateCreateATTransaction(pubkey, fee, deadline, name,
                descrition, new byte[0], compiledContract.getCode().array(), new byte[0], 1, 1, 1, activationFee);

        return createAT.flatMap(response -> {
            byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
            byte[] signedTransactionBytes = bc.signTransaction(passphrase, unsignedTransactionBytes);
            return bns.broadcastTransaction(signedTransactionBytes);
        });
    }

    /**
     * Try to find the AT for the given name, registered by the given address.
     * 
     * @param address
     * @param name
     * @return the ATResponse or null if not found
     */
    public static ATResponse findContract(BurstAddress address, String name) {
        AccountATsResponse ats = bns.getAccountATs(address).blockingGet();
        for (ATResponse ati : ats.getATs()) {
            if (ati.getName().equals(name))
                return ati;
        }
        return null;
    }

    /**
     * Returns the current long value of a given field address.
     * 
     * If the update param is true, the node is consulted (in blocking way)
     * to get the current value of the field.
     * 
     * @param contract a smart contract response
     * @param address the field address, check {@link Field#getAddress()}
     * @param update if the node should be contacted (in blocking way) for an updated value
     * @return the current long value of a given field
     */
    public static long getContractFieldValue(ATResponse contract, int address, boolean update) {
        if(update)
            contract = bns.getAt(contract.getAt().getBurstID()).blockingGet();

        HexStringByteArray data = contract.getMachineData();

        ByteBuffer b = ByteBuffer.wrap(data.getBytes());
        b.order(ByteOrder.LITTLE_ENDIAN);

        return b.getLong(address * 8);
    }
}
