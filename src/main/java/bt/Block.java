package bt;

import java.util.ArrayList;

/**
 * Class representing a block in the block-chain.
 * 
 * This class should not be directly used, is to be part of the
 * emulated block-chain only.
 * 
 * @author jjos
 */
class Block {

	long height;
	
	Block prev;
	Block next;
	ArrayList<Transaction> txs = new ArrayList<Transaction>();
	Register hash = new Register();
	
	public Block(Block prev) {
		this.prev = prev;
		if(prev!=null) {
			prev.next = this;

			this.height = prev.height +1;
		}

		// Just some random numbers for the block hash
		hash.value1 = (long) (Math.random() * Long.MAX_VALUE);
		hash.value2 = (long) (Math.random() * Long.MAX_VALUE);
		hash.value3 = (long) (Math.random() * Long.MAX_VALUE);
		hash.value4 = (long) (Math.random() * Long.MAX_VALUE);		
	}
	
	public long getHeight() {
		return height;
	}
}

