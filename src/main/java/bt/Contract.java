package bt;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Semaphore;

import signumj.crypto.SignumCrypto;
import signumj.entity.SignumID;

/**
 * The BlockTalk smart contract abstract class.
 * 
 * Users should extend this class and implement at least the
 * {@link #txReceived()} method.
 * 
 * The class should be public but please be extremely careful when setting a
 * method public. Public methods can be called from external sources in the
 * blockchain (by incoming transactions of other accounts or contracts).
 * Further, public methods should not return values and should take at most 3
 * arguments.
 * 
 * @author jjos
 *
 */
public abstract class Contract {

	public final static long ONE_BURST = 100000000L;
	public final static long ONE_SIGNA = 100000000L;
	public final static long FEE_QUANT = 735000L;
	public final static long FEE_QUANT_SIP34 = 1_000_000;
	public final static long STEP_FEE = FEE_QUANT / 10L; // After AT2 fork, othewise ONE_BURST/10

	Address address;
	Address creator;
	Timestamp creation;

	Transaction currentTx;
	long activationFee;

	// Thread stuff to emulate sleep functions
	Semaphore semaphore = new Semaphore(1);
	boolean running;
	Timestamp sleepUntil;

	protected Contract() {
		Emulator emu = Emulator.getInstance();
		setInitialVars(emu.curTx, new Timestamp(emu.getCurrentBlock().getHeight(), 0));
	}

	/**
	 * Utility function that return the address of a given BURST-account
	 * 
	 * @param account
	 * @return
	 */
	protected Address parseAddress(String rs) {
		return Emulator.getInstance().getAddress(rs);
	}
	
	/**
	 * Utility function that return the address of a given ID
	 * @param id the signed long id
	 * @return the address
	 */
	protected Address getAddress(long id) {
		return Emulator.getInstance().getAddress(SignumCrypto.getInstance().rsEncode(SignumID.fromLong(id)));
	}

	/**
	 * Utility function that return the transaction object of a given ID
	 * @param id the signed long id
	 * @return the transaction
	 */
	protected Address getTransaction(long id) {
		// TODO: implement on the emulator
		return null;
	}

	/**
	 * Send the entire balance to the given address.
	 * 
	 * Care should be exercised with this function since a contract with no balance
	 * cannot continue to run!
	 * 
	 * @param ad the address
	 */
	protected void sendBalance(Address ad) {
		sendAmount(this.address.balance, ad);
	}

	/**
	 * Send the given amount to the receiver address
	 * 
	 * @param amount
	 * @param receiver
	 */
	protected void sendAmount(long amount, Address receiver) {
		Emulator.getInstance().send(address, receiver, amount, true);
	}
	
	/**
	 * Sends the given asset ID amount to the receiver address
	 * @param assetId
	 * @param quantity
	 * @param receiver
	 */
	protected void sendAmount(long assetId, long quantity, Address receiver) {
		Emulator.getInstance().send(address, receiver, 0L, assetId, quantity, true);
	}
	
	/**
	 * Sends the given asset ID quantity plus SIGNA amount to the receiver address
	 * @param assetId
	 * @param quantity
	 * @param receiver
	 */
	protected void sendAmount(long amount, long assetId, long quantity, Address receiver) {
		Emulator.getInstance().send(address, receiver, amount, assetId, quantity, true);
	}
	
	/**
	 * Returns the pow(x, y/1_0000_0000) calculation using double precision
	 * 
	 * @param x
	 * @param y
	 * @return pow(x, y/1_0000_0000)
	 */
	protected long calcPow(long x, long y) {
		if(x < 0)
			return 0;
		
		double ret = Math.pow(x, y/10000_0000.0);
		
		return (long)ret;
	}
	
	/**
	 * Calculates (x*y)/den using big integer precision (arbitrary high precision)
	 * 
	 * @param x
	 * @param y
	 * @param den
	 * @return (x*y)/den
	 */
	protected long calcMultDiv(long x, long y, long den) {
		if (den == 0L)
			return 0L;
		
		return BigInteger.valueOf(x).multiply(BigInteger.valueOf(y)).divide(BigInteger.valueOf(den)).longValue();
	}


	/**
	 * Send the given message to the given address.
	 * 
	 * The message has the isText flag set as false, the hexadecimal values are
	 * converted to characters when shown in BRS wallet. Message is always
	 * unencrypted.
	 * 
	 * @param message  the message, truncated in 4*sizeof(long)
	 * @param tradeAmount   the amount or 0 to send the message only
	 * @param receiver the address
	 */
	protected void sendMessage(String message, Address receiver) {
		Emulator.getInstance().send(address, receiver, 0L, 0L, 0L, message, true);
	}

	/**
	 * Send the given message to the given address.
	 * 
	 * The message has the isText flag set as false, the hexadecimal values are
	 * converted to characters when shown in BRS wallet. Message is always
	 * unencrypted.
	 * 
	 * @param message  the message in form of a 4 longs register
	 * @param receiver the address
	 */
	protected void sendMessage(Register message, Address receiver) {
		Emulator.getInstance().send(address, receiver, 0, message);
	}
	
	/**
	 * Send the given message to the given address.
	 * 
	 * The message has the isText flag set as false, the given hexadecimal value is
	 * converted to characters when shown in BRS wallet. Message is always
	 * unencrypted.
	 * 
	 * @param message  the message in form of a long number
	 * @param receiver the address
	 */
	protected void sendMessage(long message, Address receiver) {
		Emulator.getInstance().send(address, receiver, 0, Register.newInstance(message, 0, 0, 0));
	}
	
	/**
	 * Send the given message to the given address.
	 * 
	 * The message has the isText flag set as false, the given hexadecimal values are
	 * converted to characters when shown in BRS wallet. Message is always
	 * unencrypted.
	 * 
	 * @param message  the message in form of a long number
	 * @param message2  the message in form of a long number
	 * @param receiver the address
	 */
	protected void sendMessage(long message, long message2, Address receiver) {
		Emulator.getInstance().send(address, receiver, 0, Register.newInstance(message, message2, 0, 0));
	}
	
	/**
	 * Send the given message to the given address.
	 * 
	 * The message has the isText flag set as false, the given hexadecimal values are
	 * converted to characters when shown in BRS wallet. Message is always
	 * unencrypted.
	 * 
	 * @param message  the message in form of a long number
	 * @param message2  the message in form of a long number
	 * @param message3  the message in form of a long number
	 * @param message4  the message in form of a long number
	 * @param receiver the address
	 */
	protected void sendMessage(long message, long message2, long message3, long message4, Address receiver) {
		Emulator.getInstance().send(address, receiver, 0, Register.newInstance(message, message2, message3, message4));
	}

	/**
	 * Function to be called before processing the transactions of the current
	 * block.
	 * 
	 * Users should overload this function if there is some action to be taken
	 * before processing the first {@link #txReceived()} for the current block.
	 * 
	 * Use this function carefully, since #{@link #getCurrentTx()} and similar
	 * functions are still unavalable inside this function.
	 */
	protected void blockStarted() {
	}

	/**
	 * Function to be called when all transactions on the current block were
	 * processed.
	 * 
	 * Users should overload this function if there is some action to be taken after
	 * the last {@link #txReceived()} was called for the current block.
	 */
	protected void blockFinished() {
	}

	/**
	 * Get the first transaction received after the given timestamp
	 * 
	 * @param ts
	 * @return
	 */
	protected Transaction getTxAfterTimestamp(Timestamp ts) {
		return Emulator.getInstance().getTxAfter(address, ts);
	}

	/**
	 * @return the transaction being processed at this moment
	 */
	protected Transaction getCurrentTx() {
		return currentTx;
	}

	/**
	 * @return the current transaction timestamp
	 */
	protected Timestamp getCurrentTxTimestamp() {
		return getCurrentTx().getTimestamp();
	}

	/**
	 * @return the current transaction sender address
	 */
	protected Address getCurrentTxSender() {
		return getCurrentTx().getSenderAddress();
	}

	/**
	 * @return the current transaction amount
	 */
	protected long getCurrentTxAmount() {
		return getCurrentTx().getAmount();
	}
	
	/**
	 * @param assetId
	 * @return the current transaction amount for the given asset ID
	 */
	protected long getCurrentTxAmount(long assetId) {
		return getCurrentTx().getAmount(assetId);
	}

	/**
	 * @return the block hash of the previous block (part 1 of 4)
	 */
	protected Register getPrevBlockHash() {
		return Emulator.getInstance().getPrevBlock().hash;
	}

	/**
	 * @return the first part of the previous block hash
	 */
	protected long getPrevBlockHash1() {
		return Emulator.getInstance().getPrevBlock().hash.getValue1();
	}
	
	/**
	 * @return the block generation signature of the previous block
	 */
	protected Register getPrevBlockGenSig() {
		return Emulator.getInstance().getPrevBlock().hash;
	}
	
	/**
	 * @return the first part of the previous generation signature
	 */
	protected long getPrevBlockGenSig1() {
		return Emulator.getInstance().getPrevBlock().hash.getValue1();
	}

	/**
	 * @return the timestamp of the previous block
	 */
	protected Timestamp getPrevBlockTimestamp() {
		return new Timestamp(Emulator.getInstance().getPrevBlock().getHeight(), 0);
	}

	/**
	 * @return the timestamp of the block being processed
	 */
	protected Timestamp getBlockTimestamp() {
		return new Timestamp(Emulator.getInstance().getCurrentBlock().getHeight(), 0);
	}
	
	/**
	 * @return the timestamp of the block being processed
	 */
	protected long getBlockHeight() {
		return Emulator.getInstance().getCurrentBlock().getHeight();
	}

	/**
	 * @return the address of the creator of this contract
	 */
	protected Address getCreator() {
		return creator;
	}
	
	/**
	 * @param contractId
	 * @return the code hash ID for the given contract ID
	 */
	protected long getCodeHashId(long contractId) {
		// TODO Emulator implementation
		return 0;
	}
	
	/**
	 * @return the code hash ID of this contract
	 */
	protected long getCodeHashId() {
		// TODO Emulator implementation
		return 0;
	}
	
	/**
	 * @return the address of the creator of another contract
	 */
	protected Address getCreator(Address contract) {
		if(contract.getContract() != null)
			return contract.getContract().creator;
		return getAddress(0L);
	}

	/**
	 * @return the creation timestamp of this contract
	 */
	protected Timestamp getCreationTimestamp() {
		return creation;
	}

	/**
	 * @return the current balance of this contract
	 */
	protected long getCurrentBalance() {
		return address.balance;
	}
	
	/**
	 * @param assetId the asset id we want the balance
	 * @return the current balance of this contract
	 */
	protected long getCurrentBalance(long assetId) {
		return address.getBalance(assetId);
	}
	
	/**
	 * @param key1
	 * @param key2
	 * @return the map value associated with this contract and given keys
	 */
	protected long getMapValue(long key1, long key2) {
		return Emulator.getInstance().getMapValue(this.address, key1, key2);
	}

	/**
	 * @param contract
	 * @param key1
	 * @param key2
	 * @return the map value associated with the given contract and keys
	 */
	protected long getMapValue(Address contract, long key1, long key2) {
		return Emulator.getInstance().getMapValue(contract, key1, key2);
	}

	/**
	 * Set the given value to be associated with this contract and given keys.
	 * 
	 * @param key1
	 * @param key2
	 * @param value
	 */
	protected void setMapValue(long key1, long key2, long value) {
		Emulator.getInstance().setMapValue(this.address, key1, key2, value);
	}
	
	protected long issueAsset(long namePart1, long namePart2, long decimalPlaces) {
		return Emulator.getInstance().issueAsset(this.address, namePart1, namePart2, decimalPlaces);
	}
	
	protected long getActivationFee() {
		return 0;
	}
	
	protected long getActivationFee(Contract other) {
		return 0;
	}

	protected void mintAsset(long assetId, long quantity) {
		Emulator.getInstance().mintAsset(this.address, assetId, quantity);
	}
	
	protected void distributeToHolders(long minHolderAmount, long assetId, long amount, long assetToDistribute, long quantity) {
		// TODO implementation missing
	}

	protected int getAssetHoldersCount(long minHolderAmount, long assetId) {
		// TODO implementation missing
		return 0;
	}

	@EmulatorWarning
	public static Register performSHA256_(Register input) {
		Register ret = new Register();

		ByteBuffer b = ByteBuffer.allocate(32);
		b.order(ByteOrder.LITTLE_ENDIAN);

		for (int i = 0; i < 4; i++) {
			b.putLong(input.value[i]);
		}

		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			ByteBuffer shab = ByteBuffer.wrap(sha256.digest(b.array()));
			shab.order(ByteOrder.LITTLE_ENDIAN);

			for (int i = 0; i < 4; i++) {
				ret.value[i] = shab.getLong(i * 8);
			}
		} catch (NoSuchAlgorithmException e) {
			// not expected to reach that point
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * @return a SHA256 hash of the given input
	 */
	protected Register performSHA256(Register input) {
		return performSHA256_(input);
	}

	/**
	 * A utility function returning only the first 64 bits of a SHA256 for two long
	 * inputs.
	 * 
	 * @return the first 64 bits SHA256 hash of the given input
	 */
	protected long performSHA256_64(long input1, long input2) {
		Register input = new Register();
		input.value[0] = input1;
		input.value[1] = input2;

		Register ret = performSHA256_(input);
		return ret.getValue1();
	}
	
	/**
	 * Checks if the signature of the given account id in tx (page, page+1) matches for the given message.
	 * 
	 * The message actually consists in [contractId, msg2, msg3, msg4].
	 * 
	 * @param msg2
	 * @param msg3
	 * @param msg4
	 * @param tx
	 * @param page
	 * @param accountId
	 * @return
	 */
	protected long checkSignature(long msg2, long msg3, long msg4, Transaction tx, long page, long accountId) {
		// TODO: no account id and public key stored in the emulator to verify the signature
		return 0;
	}

	/**
	 * Sleeps until the contract receives a new transaction.
	 */
	protected void sleepUntilNextTx() {
		// FIXME: on emulator we will sleep for one block
		sleep(0);
	}

	/**
	 * Sleeps for the given number of blocks.
	 * 
	 * @param nblocks number of blocks to sleep
	 */
	protected void sleep(long nblocks) {
		if(nblocks <= 0)
			sleepUntil = null;
		else {
			sleepUntil = new Timestamp(Emulator.getInstance().getCurrentBlock().height + nblocks, 0);
			address.setSleeping(true);
			try {
				semaphore.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		address.setSleeping(false);
		sleepUntil = null;
	}

	/**
	 * A new transaction was received.
	 * 
	 * Overload this function to implement your contract.
	 */
	public abstract void txReceived();

	// ========================================================
	// Functions not visible, will be used by the virtual
	// machine when in debug mode but not exported to the
	// AT blockchain machine code

	void setInitialVars(Transaction tx, Timestamp creation) {
		this.creator = tx.sender;
		this.address = tx.receiver;
		this.creation = creation;
		this.activationFee = tx.getAmount();
		this.address.contract = this;
		
		// we acquire the semaphore in the creation process, it is released
		// when finished by the emulator
		try {
			running = true;
			semaphore.acquire();
		} catch (InterruptedException e) {
			running =false;
		}
	}

	void setCurrentTx(Transaction current) {
		this.currentTx = current;
	}

	@EmulatorWarning
	public String getFieldValues() {
		String ret = "<html>";
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field f : fields) {
			try {
				f.setAccessible(true);
				Object v = f.get(this);
				ret += "<b>" + f.getName() + "</b> = " + (v != null ? v.toString() : "null") + "<br>";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
}
