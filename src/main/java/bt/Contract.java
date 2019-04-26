package bt;

/**
 * The BlockTalk smart contract abstract class.
 * 
 * Users should extend this class and implement at least the {@link #txReceived()} method.
 * 
 * The class should be public but please be extremely careful when setting a method public.
 * Public methods can be called from external sources in the blockchain (by incoming transactions
 * of other accounts or contracts).
 * Further, public methods should not return values and should take at most 3 arguments.
 * 
 * @author jjos
 *
 */
public abstract class Contract {
	
	public final static long ONE_BURST = 100000000L;
	public final static long FEE_QUANT = 735000L;
	public final static long STEP_FEE = FEE_QUANT/10L; // After AT2 fork, othewise ONE_BURST/10
	
	Address address;
	Address creator;
	Timestamp creation;
	
	Transaction currentTx;
	long activationFee;
	
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
	 * Send the entire balance to the given address
	 * @param ad the address
	 */
	protected void sendBalance(Address ad) {
		sendAmount(ad.balance, ad);
	}
	
	/**
	 * Send the given amount to the receiver address
	 * @param amount
	 * @param receiver
	 */
	protected void sendAmount(long amount, Address receiver) {
		Emulator.getInstance().send(address, receiver, amount, null);
	}
	
	/**
	 * Send the given message to the given address.
	 * 
	 * The message has the isText flag set as false, the hexadecimal
	 * values are converted to characters when shown in BRS wallet.
	 * Message is always unencrypted.
	 * 
	 * @param message the message, truncated in 4*sizeof(long)
	 * @param receiver the address
	 */
	protected void sendMessage(String message, Address receiver) {
		Emulator.getInstance().send(address, receiver, 0, message);
	}

	/**
	 * Send the given message to the given address.
	 * 
	 * The message has the isText flag set as false, the hexadecimal
	 * values are converted to characters when shown in BRS wallet.
	 * Message is always unencrypted.
	 * 
	 * @param message the message, truncated in 4*sizeof(long)
	 * @param receiver the address
	 */
	protected void sendMessage(Register message, Address receiver) {
		// FIXME: convert register to a string here
		Emulator.getInstance().send(address, receiver, 0, null);
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
	 * @return the block hash of the previous block
	 */
	protected Register getPrevBlockHash() {
		return Emulator.getInstance().getPrevBlock().hash;
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
	 * @return the address of the creator of this contract
	 */
	protected Address getCreator() {
		return creator;
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
	 * A new transaction was received.
	 * 
	 * Overload this function to implement your contract.
	 */
	public abstract void txReceived();	
	
	
	// ========================================================
	// Functions not visible, will be used by the virtual
	// machine when in debug mode but not exported to the
	// AT blockchain machine code
	
	
	void setInitialVars(Address creator, Timestamp creation,
			long activationFee) {
		this.creator = creator;
		this.creation = creation;
		this.activationFee = activationFee;
	}
	
	void setCurrentTx(Transaction current) {
		this.currentTx = current;
	}
}
