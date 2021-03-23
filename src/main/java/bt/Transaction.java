package bt;

/**
 * Class representing a transaction.
 * 
 * This class should only be used by the emulated block-chain.
 * 
 * @author jjos
 *
 */
public class Transaction {

	static final byte TYPE_PAYMENT = 0;
	static final byte TYPE_MESSAGING = 1;
	static final byte TYPE_AT_CREATE = 2;
	static final byte TYPE_METHOD_CALL = 3;

	Block block;
	Address sender;
	Address receiver;
	long amount;
	byte type;
	Timestamp ts;
	String msgString;
	Register msg;

	/**
	 * Users are not allowed to create new instances of this class, this function
	 * should be called by the emulator only.
	 * 
	 * @param sender
	 * @param receiver
	 * @param ammount
	 * @param type
	 * @param ts
	 * @param msg
	 * @return
	 */
	Transaction(Address sender, Address receiver, long ammount, byte type, Timestamp ts, String msg) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = ammount;
		this.type = type;
		this.ts = ts;
		msgString = msg;
		if (msg == null) {
			this.msgString = "";
			return;
		}
		this.msg = Register.newMessage(this.msgString);
	}

	/**
	 * Users are not allowed to create new instances of this class, this function
	 * should be called by the emulator only.
	 * 
	 * @param ad
	 * @param ammount
	 * @param type
	 * @param ts
	 * @param msg
	 * @return
	 */
	Transaction(Address sender, Address receiver, long ammount, byte type, Timestamp ts, Register msg) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = ammount;
		this.type = type;
		this.ts = ts;
		this.msg = msg;
	}

	/**
	 * @return the sender address for this transaction
	 */
	public Address getSenderAddress() {
		return sender;
	}

	/**
	 * @return the reciever address for this transaction
	 */
	@EmulatorWarning
	public Address getReceiverAddress() {
		return receiver;
	}

	/**
	 * @return the amount in this transaction minus the activation fee
	 */
	public long getAmount() {
		if (receiver != null && receiver.contract != null)
			return amount - receiver.contract.activationFee;
		return amount;
	}

	/**
	 * Return the message attached to a transaction.
	 * 
	 * Only unencrypted messages are received.
	 * 
	 * @return the message in this transaction
	 */
	public Register getMessage() {
		return msg;
	}
	
	/**
	 * Return the message attached to a transaction.
	 * 
	 * Only unencrypted messages are received.
	 * 
	 * @return the message in this transaction
	 */
	public boolean checkMessageSHA256(Register hash) {
		return Contract.performSHA256_(msg).equals(hash);
	}
	
	/**
	 * Check if the last 192 bits of the message attached to this transaction
	 * matches the given hash. 
	 * 
	 * @return true if they match
	 */
	public boolean checkMessageSHA256_192(Register hash) {
		Register msgHash = Contract.performSHA256_(msg);
		return msgHash.getValue2()== hash.getValue2() && msgHash.getValue3()== hash.getValue3()
				&& msgHash.getValue4()== hash.getValue4();
	}

	/**
	 * Return the first 8 bytes of the message attached to a transaction.
	 * 
	 * Only unencrypted messages are received.
	 * 
	 * @return the first 8 bytes in the message
	 */
	public long getMessage1() {
		if(msg == null)
			return 0L;
		return msg.value[0];
	}
	
	public long getMessage2() {
		if(msg == null)
			return 0L;
		return msg.value[1];
	}

	/**
	 * @return the message in this transaction
	 */
	@EmulatorWarning
	public String getMessageString() {
		return msgString;
	}

	/**
	 * @return the pseudo-timestamp of this transaction (block height and txid)
	 */
	public Timestamp getTimestamp() {
		return ts;
	}

	public byte getType() {
		return type;
	}

	public Block getBlock() {
		return block;
	}
}
