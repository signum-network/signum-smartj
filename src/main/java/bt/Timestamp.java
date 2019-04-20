package bt;

/**
 * A class representing a timestamp.
 * 
 * Note that "timestamp" does not mean a real timestamp but instead
 * is an artificial timestamp that includes two parts.
 * The first part is a block height (32 bits) with the second part
 * being the number of the transaction if applicable
 * (also 32 bits and zero if not applicable).
 * 
 * @author jjos
 *
 */
public class Timestamp {
	
	long value;
	
	/**
	 * Users are not allowed to instantiate this class, called by the emulator only.
	 */
	Timestamp(long block, long txid) {
		value = block << 8;
		value += txid;
	}
	
	/**
	 * Return a new timestamp by adding the given minutes
	 * @param minutes
	 * @return
	 */
	public Timestamp addMinutes(long minutes) {
		value += (minutes/4)<<8;
		return this;
	}
	
	/**
	 * @return true if this timestamp is greater or equal than the given one
	 */
	public boolean ge(Timestamp ts) {
		return this.value >= ts.value;
	}
	
	/**
	 * @return true if this timestamp is lower or equal than the given one
	 */
	public boolean le(Timestamp ts) {
		return this.value < ts.value;
	}

}
