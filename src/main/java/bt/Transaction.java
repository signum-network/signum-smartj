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
	
	Block block;
	Address sender;
	Address receiver;
	long amount;
	byte type;
	Timestamp ts;
	String msg;

	/**
	 * Users are not allowed to create new instances of this class,
	 * this function should be called by the emulator only.
	 * 
	 * @param ad
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
		this.msg = msg;
	}

	/**
	 * @return the sender address for this transaction
	 */
	public Address getSenderAddress() {
		return sender;
	}

	/**
	 * @return the amount in this transaction minus the activation fee
	 */
	public long getAmount() {
		if(receiver!=null && receiver.contract!=null)
			return amount-receiver.contract.activationFee;
		return amount;
	}
	
	byte getType() {
		return type;
	}

	/**
	 * Return the message attached to a transaction.
	 * 
	 * Only unencrypted messages are received.
	 * 
	 * @return the message in this transaction
	 */
	public String getMessage() {
		return msg;
	}
	
	/**
	 * @return the pseudo-timestamp of this transaction (block height and txid)
	 */
	public Timestamp getTimestamp() {
		return ts;
	}
	
	public boolean isNull() {
		return sender==null && receiver == null;
	}
}
