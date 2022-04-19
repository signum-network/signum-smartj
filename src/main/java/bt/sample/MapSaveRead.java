package bt.sample;

import bt.Address;
import bt.BT;
import bt.Contract;
import bt.ui.EmulatorWindow;

public class MapSaveRead extends Contract {
	long valueRead;
	
	public void saveValue(long key1, long key2, long value) {
		setMapValue(key1, key2, value);
	}
	
	public void readValue(long key1, long key2) {
		valueRead = getMapValue(key1, key2);
	}
	
	public void readValueExternal(Address contract, long key1, long key2) {
		valueRead = getMapValue(contract, key1, key2);
	}
	
	@Override
	public void txReceived() {
		// not used on this samples
	}
	
	public static void main(String[] args) {
		BT.activateSIP37(true);
		new EmulatorWindow(MapSaveRead.class);
	}
}
