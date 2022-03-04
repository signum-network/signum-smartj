package bt;

import bt.sample.Hello;
import org.junit.Test;
import signumj.entity.response.AT;

import static org.junit.Assert.assertEquals;

/**
 * Simple testings for contracts
 *
 * @author tyvain
 */
public class SimpleContractTest extends BT {

	/*
	 * Create contract, create wallet, send amount to contract
	 */
	@Test
	public void testNominal() throws Exception {

		TestWalett player1 = new TestWalett(1000);

		AT at = TestUtils.registerContract(Hello.class);

		player1.sendAmount(at.getId(), 150);

		assertEquals(850, player1.getBalance(), 0.2);
	}

}
