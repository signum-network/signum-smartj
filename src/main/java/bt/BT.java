package bt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;

import bt.compiler.Compiler;
import bt.compiler.Field;
import bt.compiler.Method;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;
import burst.kit.entity.response.TransactionBroadcast;
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
    public static final String NODE_LOCAL_TESTNET_GRPC = "grpc://localhost:6878";
    public static final String NODE_AT_TESTNET = "http://at-testnet.burst-alliance.org:6876";
    public static final String NODE_AT_TESTNET_GRPC = "grpc://at-testnet.burst-alliance.org:6878";
    public static final String NODE_TESTNET = "http://testnet.getburst.net:6876";
    public static final String NODE_TESTNET_GRPC = "grpc://testnet.getburst.net:6878";
    public static final String NODE_TESTNET_MEGASH = "https://test-burst.megash.it";
    public static final String NODE_TESTNET_MEGASH_GRPC = "grpc://test-burst.megash.it:6878";

    public static final String NODE_BURST_TEAM = "https://wallet1.burst-team.us:2083";
    public static final String NODE_BURST_ALLIANCE = "https://wallet.burst-alliance.org:8125";
    public static final String NODE_BURSTCOIN_RO = "https://wallet.burstcoin.ro:443";
    public static final String NODE_BURSTCOIN_RO2 = "https://wallet2.burstcoin.ro:443";

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
     * @return the BURST address fot the given passphrase
     */
    public static BurstAddress getBurstAddressFromPassphrase(String passphrase) {
        return bc.getBurstAddressFromPassphrase(passphrase);
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
    public static Single<TransactionBroadcast> callMethod(String passFrom, BurstAddress contractAddress, Method method,
            BurstValue value, BurstValue fee, int deadline, Object... args) {

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
                b.putLong(((Boolean) arg) ? 1 : 0);
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

    public static Single<TransactionBroadcast> sendAmount(String passFrom, BurstAddress receiver, BurstValue value) {
        return sendAmount(passFrom, receiver, value, BurstValue.fromBurst(0.1));
    }

    public static Single<TransactionBroadcast> sendAmount(String passFrom, BurstAddress receiver, BurstValue value,
            BurstValue fee) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        return bns.generateTransaction(receiver, pubKeyFrom, value, fee, 1440)
                .flatMap(unsignedTransactionBytes -> {
                    byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
                    return bns.broadcastTransaction(signedTransactionBytes);
                });
    }

    public static Single<TransactionBroadcast> sendMessage(String passFrom, BurstAddress receiver, BurstValue value,
            BurstValue fee, int deadline, String msg) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        return bns.generateTransactionWithMessage(receiver, pubKeyFrom, value, fee, deadline, msg)
                .flatMap(unsignedTransactionBytes -> {
                    byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
                    return bns.broadcastTransaction(signedTransactionBytes);
                });
    }

    public static Single<TransactionBroadcast> sendMessage(String passFrom, BurstAddress receiver, BurstValue value,
            BurstValue fee, int deadline, byte[] msg) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        return bns.generateTransactionWithMessage(receiver, pubKeyFrom, value, fee, deadline, msg)
                .flatMap(unsignedTransactionBytes -> {
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
     * Forge block by mock mining for the given passphrase, just for testing purposes.
     */
    public static void forgeBlock(String pass) {
        forgeBlock(pass, 1000);
    }

    /**
     * Forge block by mock mining for the given passphrase followed by a sleep with
     * given milliseconds.
     * 
     * Just for testing purposes.
     */
    public static void forgeBlock(String pass, int millis) {
        bns.submitNonce(pass, "0", null).blockingGet();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the minimum fee to register the given contract
     */
    public static BurstValue getMinRegisteringFee(Compiler compiledContract) {
        return BurstValue.fromBurst(2 + compiledContract.getDataPages() + compiledContract.getCodeNPages());
    }

    /**
     * Register the given contract with the given activation fee.
     */
    public static AT registerContract(Class<? extends Contract> contract, BurstValue activationFee) throws Exception {
        Compiler compiledContract = compileContract(contract);

        String name = contract.getSimpleName() + System.currentTimeMillis();

        registerContract(PASSPHRASE, compiledContract, name, contract.getSimpleName(), activationFee,
                getMinRegisteringFee(compiledContract), 1000).blockingGet();
        forgeBlock();

        return findContract(bc.getBurstAddressFromPassphrase(PASSPHRASE), name);
    }

    /**
     * Register the given contract with the given activation fee.
     */
    public static AT registerContract(Compiler compiledContract, String name, BurstValue activationFee)
            throws Exception {

        registerContract(PASSPHRASE, compiledContract, name, name, activationFee,
                getMinRegisteringFee(compiledContract), 1000).blockingGet();
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
    public static Single<TransactionBroadcast> registerContract(String passphrase, Compiler compiledContract,
            String name, String description, BurstValue activationFee, BurstValue fee, int deadline) {
        return registerContract(passphrase, compiledContract.getCode(), compiledContract.getDataPages(), name, description, null, activationFee, fee, deadline);
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
    public static Single<TransactionBroadcast> registerContract(String passphrase, byte[] code, int dPages,
            String name, String description, long[] data, BurstValue activationFee, BurstValue fee, int deadline) {
        byte[] pubkey = bc.getPublicKey(passphrase);

        ByteBuffer dataBuffer = ByteBuffer.allocate(data==null ? 0 : data.length*8);
        dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; data!=null && i < data.length; i++) {
            dataBuffer.putLong(data[i]);
        }

        byte[] creationBytes = BurstCrypto.getInstance().getATCreationBytes((short) 1, code, dataBuffer.array(), dPages, 1, 1, activationFee);
        return bns.generateCreateATTransaction(pubkey, fee, deadline, name, description, creationBytes)
                .flatMap(unsignedTransactionBytes -> {
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
    public static AT findContract(BurstAddress address, String name) {
        AT[] ats = bns.getAccountATs(address).blockingGet();
        for (AT ati : ats) {
            if (ati.getName().equals(name))
                return ati;
        }
        return null;
    }

    /**
     * Return all contracts registered by the given addres
     * 
     * @param address
     * @return
     */
    public static AT[] getContracts(BurstAddress address) {
        return bns.getAccountATs(address).blockingGet();
    }

    /**
     * Returns the current long value of a given field address.
     * 
     * If the update param is true, the node is consulted (in blocking way) to get
     * the current value of the field.
     * 
     * @param contract a smart contract response
     * @param address  the field address, check {@link Field#getAddress()}
     * @return the current long value of a given field
     */
    public static long getContractFieldValue(AT contract, int address) {
        contract = bns.getAt(contract.getId()).blockingGet();

        byte[] data = contract.getMachineData();
        ByteBuffer b = ByteBuffer.wrap(data);
        b.order(ByteOrder.LITTLE_ENDIAN);

        return b.getLong(address * 8);
    }

    /**
     * @param contract
     * @return the balance for the given contract
     */
    public static BurstValue getContractBalance(AT contract) {
        contract = bns.getAt(contract.getId()).blockingGet();
        return contract.getBalance();
    }
}
