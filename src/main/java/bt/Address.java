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
	boolean sleeping;
	
	/**
	 * Should be called by the emulator only.
	 * 
	 * @param id
	 * @param balance
	 * @param rs
	 */
	@EmulatorWarning
	Address(long id, long balance, String rs) {
		this.id = id;
		this.balance = balance;
		this.rsAddress = rs;
	}
	
	/**
	 * @return the reed solomon address
	 */
	@EmulatorWarning
	public String getRsAddress() {
		return rsAddress;
	}

	/**
	 * @return the account id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the current balance available
	 */
	@EmulatorWarning
	public long getBalance() {
		return balance;
	}

	/**
	 * @return the underlying contract or null
	 */
	@EmulatorWarning
	public Contract getContract() {
		return contract;
	}
	
	/**
	 * @return true if it is a sleeping contract
	 */
	@EmulatorWarning
	public boolean isSleeping() {
		return sleeping;
	}
	
	@EmulatorWarning
	public void setSleeping(boolean sleeping) {
		this.sleeping = sleeping;
	}

	@Override
	@EmulatorWarning
	public String toString() {
		return rsAddress;
	}
}
