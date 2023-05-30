package bt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.compiler.Compiler.Error;
import bt.sample.MapSaveRead;
import signumj.entity.SignumID;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.entity.response.TransactionBroadcast;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 *
 * @author jjos
 */
public class MapTest extends BT {

	static {
		activateSIP37(true);
	}

	@Test
	public void testAll() throws Exception {
		Compiler comp = BT.compileContract(MapSaveRead.class);
		for(Error e : comp.getErrors()) {
			System.err.println(e.getMessage());
		}
		assertTrue(comp.getErrors().size() == 0);

		AT contract1 = registerContract(MapSaveRead.class, SignumValue.fromSigna(0.3));
		System.out.println("contract 1 :" + contract1.getId().getID());

		AT contract2 = registerContract(MapSaveRead.class, SignumValue.fromSigna(0.3));
		System.out.println("contract 2 :" + contract2.getId().getID());
		
		Random r = new Random();
		long key1 = r.nextLong();
		long key2 = r.nextLong();
		long value = r.nextLong();
		
		System.out.println("key1: " + SignumID.fromLong(key1).getID());
		System.out.println("key2: " + SignumID.fromLong(key2).getID());
		System.out.println("value: " + SignumID.fromLong(value).getID());

		TransactionBroadcast tb = BT.callMethod(BT.PASSPHRASE, contract1.getId(), comp.getMethod("saveValue"), contract1.getMinimumActivation(), SignumValue.fromSigna(0.1), 1000,
				key1, key2, value);
		forgeBlock(tb);
		forgeBlock();
		forgeBlock();

		tb = BT.callMethod(BT.PASSPHRASE, contract1.getId(), comp.getMethod("readValue"), contract1.getMinimumActivation(), SignumValue.fromSigna(0.1), 1000,
				key1, key2);
		forgeBlock(tb);
		forgeBlock();

		long valueRead = BT.getContractFieldValue(contract1, comp.getFieldAddress("valueRead"));
		assertEquals(value, valueRead);
		
		tb = BT.callMethod(BT.PASSPHRASE, contract2.getId(), comp.getMethod("readValueExternal"), contract1.getMinimumActivation(), SignumValue.fromSigna(0.1), 1000,
				contract1.getId().getSignedLongId(), key1, key2);
		forgeBlock(tb);
		forgeBlock();
		valueRead = BT.getContractFieldValue(contract2, comp.getFieldAddress("valueRead"));
		assertEquals(value, valueRead);
	}
}