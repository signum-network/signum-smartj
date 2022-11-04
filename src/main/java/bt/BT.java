package bt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;

import bt.compiler.Compiler;
import bt.compiler.Field;
import bt.compiler.Method;
import io.reactivex.Single;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumID;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.entity.response.Transaction;
import signumj.entity.response.TransactionBroadcast;
import signumj.service.NodeService;

/**
 * BlockTalk Utility functions for handling on-chain contract interactions.
 *
 * Passphrases never leave the local application, messages are signed locally.
 *
 * @author jjos
 */
public class BT {

    public static final String PASSPHRASE =  "block talk, easy to use smart contracts for burst";
    public static final String PASSPHRASE2 = "block talk: easy to use smart contracts for burst";
    public static final String PASSPHRASE3 = "block talk! easy to use smart contracts for burst";
    public static final String PASSPHRASE4 = "block";
    public static final String PASSPHRASE5 = "talk";

    public static final String NODE_LOCAL_TESTNET = "http://localhost:6876";
    public static final String NODE_TESTNET_SIGNUM = "https://europe3.testnet.signum.network";
    public static final String NODE_TESTNET = NODE_TESTNET_SIGNUM;

    public static final String NODE_LOCAL = "http://localhost:8125";
    public static final String NODE_SIGNUM_BR = "https://brazil.signum.network";
    public static final String NODE_SIGNUM_EU = "https://europe.signum.network";
    public static final String NODE_BURSTCOIN_RO = "https://wallet.burstcoin.ro:443";

    static boolean SIP20_ACTIVATED = true;
    static boolean SIP37_ACTIVATED = false;

    static NodeService bns = NodeService.getInstance(NODE_LOCAL_TESTNET);
    static SignumCrypto bc = SignumCrypto.getInstance();

    /**
     * Sets the node address, by default localhost with testnet port 6876 is used.
     *
     * @param nodeAddress the node address including port number
     */
    public static void setNodeAddress(String nodeAddress) {
        setNodeInstance(NodeService.getInstance(nodeAddress));
    }

    /**
     * Sets the node service instance
     *
     * @param node the node service instance
     */
    public static void setNodeInstance(NodeService node) {
        bns = node;
    }

    /**
     * Activates SIP20 (default is activated)
     *
     * @param on
     */
    public static void activateCIP20(boolean on) {
    	SIP20_ACTIVATED = on;
    }

    /**
     * Activates CIP37 (default is deactivated for now)
     *
     * @param on
     */
    public static void activateSIP37(boolean on) {
    	SIP37_ACTIVATED = on;
    	if(on) {
    		SIP20_ACTIVATED = on;
    	}
    }
    
    public static boolean isSIP37Activated() {
    	return SIP37_ACTIVATED;
    }

    /**
     * @return the node service object
     */
    public static NodeService getNode() {
        return bns;
    }

    /**
     * @return the BURST address fot the given passphrase
     */
    public static SignumAddress getAddressFromPassphrase(String passphrase) {
        return bc.getAddressFromPassphrase(passphrase);
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
     * Build the message for a method call
     */
    public static byte[] callMethodMessage(Method method, byte []extraPages, Object... args) {
    	return callMethodMessage(method.getHash(), method.getNArgs(), extraPages, args);
    }

    /**
     * Build the message for a method call
     */
    public static byte[] callMethodMessage(long methodHash, int methodNArgs, byte []extraPages, Object... args) {

        ByteBuffer b = ByteBuffer.allocate(32 + (extraPages == null ? 0 : extraPages.length));
        b.order(ByteOrder.LITTLE_ENDIAN);

        // Start with the method hash
        b.putLong(methodHash);

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
        if (nargs != methodNArgs) {
            throw new InvalidParameterException(
                    "Expecting " + methodNArgs + " but received " + nargs + " parameters");
        }
        
        if(extraPages != null) {
        	b.put(extraPages);
        }

        return b.array();
    }

    /**
     * Call a method on the given contract address.
     */
    public static TransactionBroadcast callMethod(String passFrom, SignumAddress contractAddress, Method method,
            SignumValue value, SignumValue fee, int deadline, Object... args) {

        byte[] bytes = callMethodMessage(method, null, args);

        return sendMessage(passFrom, contractAddress, value, fee, deadline, bytes);
    }
    
    /**
     * Call a method on the given contract address.
     */
    public static TransactionBroadcast callMethod(String passFrom, SignumAddress contractAddress, Method method,
    		byte []extraPages, SignumValue value, SignumValue fee, int deadline, Object... args) {

        byte[] bytes = callMethodMessage(method, extraPages, args);

        return sendMessage(passFrom, contractAddress, value, fee, deadline, bytes);
    }
    
    /**
     * Call a method on the given contract address.
     */
    public static TransactionBroadcast callMethod(String passFrom, SignumAddress contractAddress, Method method,
            SignumID assetId, SignumValue quantity, SignumValue value, SignumValue fee, int deadline, Object... args) {

        byte[] bytes = callMethodMessage(method, null, args);

        return sendAsset(passFrom, contractAddress, assetId, quantity, value, fee, deadline, bytes);
    }

    public static TransactionBroadcast sendAmount(String passFrom, SignumAddress receiver, SignumValue value) {
        return sendAmount(passFrom, receiver, value, SignumValue.fromSigna(0.1));
    }

    public static TransactionBroadcast sendAmount(String passFrom, SignumAddress receiver, SignumValue value,
            SignumValue fee) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        TransactionBroadcast tb = bns.generateTransaction(receiver, pubKeyFrom, value, fee, 1440, null)
                .flatMap(unsignedTransactionBytes -> {
                    byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
                    return bns.broadcastTransaction(signedTransactionBytes);
                }).blockingGet();

        return tb;
    }
    
    public static TransactionBroadcast sendAsset(String passFrom, SignumAddress receiver,
    		SignumID assetId, SignumValue quantity, SignumValue amount, SignumValue fee, int deadline, byte []msg) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);
        
        Single<byte[]> single;
		if(msg == null)
        	single = bns.generateTransferAssetTransaction(pubKeyFrom, receiver, assetId, quantity, amount, fee, deadline);
		else
			single = bns.generateTransferAssetTransactionWithMessage(pubKeyFrom, receiver, assetId, quantity, amount, fee, deadline, msg);
        
        TransactionBroadcast tb = single.flatMap(unsignedTransactionBytes -> {
                    byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
                    return bns.broadcastTransaction(signedTransactionBytes);
                }).blockingGet();

        return tb;
    }

    public static Single<TransactionBroadcast> sendMessage(String passFrom, SignumAddress receiver, SignumValue value,
            SignumValue fee, int deadline, String msg) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        return bns.generateTransactionWithMessage(receiver, pubKeyFrom, value, fee, deadline, msg, null)
                .flatMap(unsignedTransactionBytes -> {
                    byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
                    return bns.broadcastTransaction(signedTransactionBytes);
                });
    }

    public static TransactionBroadcast sendMessage(String passFrom, SignumAddress receiver, SignumValue value,
            SignumValue fee, int deadline, byte[] msg) {
        byte[] pubKeyFrom = bc.getPublicKey(passFrom);

        TransactionBroadcast tb = bns.generateTransactionWithMessage(receiver, pubKeyFrom, value, fee, deadline, msg, null)
                .flatMap(unsignedTransactionBytes -> {
                    byte[] signedTransactionBytes = bc.signTransaction(passFrom, unsignedTransactionBytes);
                    return bns.broadcastTransaction(signedTransactionBytes);
                }).blockingGet();

        return tb;
    }


	/**
     * Assure a transaction is confirmed
     */
    public static void forgeBlock(TransactionBroadcast ... txs) {
		for (int i = 0; i < 4; i++) {
			// retries
			boolean allFound = true;
			for(TransactionBroadcast tx : txs){
				try {
					Transaction txConfirmed = bns.getTransaction(tx.getTransactionId()).blockingGet();
					if (txConfirmed.getBlockHeight() == Integer.MAX_VALUE) {
						allFound = false;
						break;
					}
				}
				catch (Exception e){
					allFound = false;
				}
			}
			if (allFound)
				break;

			forgeBlock(PASSPHRASE, 2000);
		}
    }

    /**
     * Forge block by mock mining, just for testing purposes.
     */
    public static void forgeBlock() {
        forgeBlock(PASSPHRASE, 2000);
    }

    /**
     * Forge block by mock mining for the given passphrase, just for testing purposes.
     */
    public static void forgeBlock(String pass) {
        forgeBlock(pass, 2000);
    }

    /**
     * Forge block by mock mining for the given passphrase with the given millis timeout.
     *
     * Just for testing purposes.
     */
    public static void forgeBlock(String pass, int millis) {
    	try {
    		Thread.sleep(200);
    		long height = bns.getMiningInfoSingle().blockingGet().getHeight();
    		long startTimer = System.currentTimeMillis();
    		bns.submitNonce(pass, "0", null).blockingGet();
    		while(true) {
    			Thread.sleep(50);
    			long newHeight = bns.getMiningInfoSingle().blockingGet().getHeight();
    			long timeElapsed = System.currentTimeMillis() - startTimer;
    			if(newHeight > height || timeElapsed > millis) {
    				break;
    			}
    		}
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }

    /**
     * @return the minimum fee to register the given contract
     *
     * @see BT#activateCIP20(boolean)
     */
    public static SignumValue getMinRegisteringFee(Compiler compiledContract) {
    	return getMinRegisteringFee(compiledContract, true);
    }

    /**
     * @return the minimum fee to register the given contract
     *
     * @see BT#activateCIP20(boolean)
     */
    public static SignumValue getMinRegisteringFee(Compiler compiledContract, boolean includeCode) {
    	SignumValue baseFee = SignumValue.fromNQT(SIP20_ACTIVATED ? Contract.FEE_QUANT*10L : Contract.ONE_BURST);
    	if(SIP37_ACTIVATED) {
    		baseFee = SignumValue.fromNQT(Contract.FEE_QUANT_SIP34*10L);
    	}
        return baseFee.multiply(2 + compiledContract.getDataPages() +
        		(includeCode ? compiledContract.getCodeNPages() : 0));
    }

    /**
     * @return the maximum number of code pages the blockchain would accept.
     */
    public static int getMaxMachineCodePages() {
    	if(SIP37_ACTIVATED)
    		return 40;
    	return SIP20_ACTIVATED ? 20 : 10;
    }

    /**
     * Register the given contract with the given activation fee.
     */
    public static AT registerContract(Class<? extends Contract> contract, SignumValue activationFee) throws Exception {
        Compiler compiledContract = compileContract(contract);

        String name = contract.getSimpleName() + System.currentTimeMillis();
        if(name.length() > 30) {
        	name = name.substring(0, 30);
        }

        return registerContract(compiledContract, name, activationFee);
    }

    /**
     * Register the given contract with the given activation fee.
     */
    public static AT registerContract(Compiler compiledContract, String name, SignumValue activationFee)
            throws Exception {

        TransactionBroadcast tb = registerContract(PASSPHRASE, compiledContract, name, name, activationFee,
                getMinRegisteringFee(compiledContract), 1000).blockingGet();
        for (int i = 0; i < 4; i++) {
	        forgeBlock();
        	// retries
        	try {
        		return getNode().getAt(SignumAddress.fromId(tb.getTransactionId())).blockingGet();
        	}
        	catch (RuntimeException ignored) {
			}
		}

        return null;
    }
    
    /**
     * @param transactionId
     * @return the contract for the given transaction id (that created the contract)
     * @throws Exception
     */
    public static AT getContract(SignumID transactionId) throws Exception {
    	return getNode().getAt(SignumAddress.fromId(transactionId)).blockingGet();
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
            String name, String description, SignumValue activationFee, SignumValue fee, int deadline) {
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
            String name, String description, long[] data, SignumValue activationFee, SignumValue fee, int deadline) {
    	return registerContract(passphrase, code, dPages, name, description, data, activationFee, fee, deadline, null);
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
            String name, String description, long[] data, SignumValue activationFee, SignumValue fee, int deadline, String referenceTxFullHash) {
        byte[] pubkey = bc.getPublicKey(passphrase);

        ByteBuffer dataBuffer = ByteBuffer.allocate(data==null ? 0 : data.length*8);
        dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; data!=null && i < data.length; i++) {
            dataBuffer.putLong(data[i]);
        }

        if(code == null) {
            return bns.generateCreateATTransaction(pubkey, fee, activationFee, deadline, name, description, code, dataBuffer.array(), dPages, 1, 1, referenceTxFullHash)
                    .flatMap(unsignedTransactionBytes -> {
                        byte[] signedTransactionBytes = bc.signTransaction(passphrase, unsignedTransactionBytes);
                        return bns.broadcastTransaction(signedTransactionBytes);
                    });
        }

        byte[] creationBytes = SignumCrypto.getInstance().getATCreationBytes(getATVersion(), code, dataBuffer.array(), (short) dPages, (short) 1, (short) 1, activationFee);
        return bns.generateCreateATTransaction(pubkey, fee, deadline, name, description, creationBytes, referenceTxFullHash)
                .flatMap(unsignedTransactionBytes -> {
                    byte[] signedTransactionBytes = bc.signTransaction(passphrase, unsignedTransactionBytes);
                    return bns.broadcastTransaction(signedTransactionBytes);
                });
    }
    
    public static short getATVersion() {
    	if (SIP37_ACTIVATED)
    		return (short) 3;
    	return (short) (SIP20_ACTIVATED ? 2 : 1);
    }

    /**
     * Try to find the AT for the given name, registered by the given address.
     *
     * @param address
     * @param name
     * @return the ATResponse or null if not found
     */
    public static AT findContract(SignumAddress address, String name) {
        AT[] ats = bns.getAccountATs(address, null).blockingGet();
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
    public static AT[] getContracts(SignumAddress address) {
        return bns.getAccountATs(address, null).blockingGet();
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
    public static SignumValue getContractBalance(AT contract) {
        contract = bns.getAt(contract.getId()).blockingGet();
        return contract.getBalance();
    }

    /**
     * Converts a smart contract timestamp to block height.
     *
     * @param timestamp
     * @return the block height
     */
    public static int longToHeight(long timestamp) {
        return (int) (timestamp >> 32);
    }
}
