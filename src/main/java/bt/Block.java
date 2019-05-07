package bt;

import java.util.ArrayList;

/**
 * Class representing a block in the block-chain.
 * 
 * This class should not be directly used in contracts, is to be part of the
 * emulated block-chain only.
 * 
 * @author jjos
 */
public class Block {

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
		for (int i = 0; i < hash.value.length; i++) {
			hash.value[i] = (long) ((Math.random()-0.5) * 1e8);
		}
	}
	
	public long getHeight() {
		return height;
	}
}

