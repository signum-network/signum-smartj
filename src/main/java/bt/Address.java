package bt;

/**
 * A burstcoin address.
 * 
 * This class should not be directly instantiated by users.
 * Use {@link Contract#parseAddress(String)} to get an address from
 * a string. Another alternative to get an address is by
 * {@link Contract#getCurrentTx()} and then
 * {@link Transaction#getSenderAddress()}.
 * 
 * @author jjos
 */
public class Address {

	long id;
	String rsAddress;
	long balance;
	Contract contract;
	
	/**
	 * Should be called by the emulator only.
	 * 
	 * @param id
	 * @param balance
	 * @param rs
	 */
	Address(long id, long balance, String rs) {
		this.id = id;
		this.balance = balance;
		this.rsAddress = rs;
	}
	
	/**
	 * @return the reed solomon address
	 */
	public String getRsAddress() {
		return rsAddress;
	}

	/**
	 * @return the current balance available
	 */
	public long getBalance() {
		return balance;
	}

	@Override
	public String toString() {
		return rsAddress;
	}
}
