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

		TestAccount player1 = new TestAccount(1000);

		AT at = TestUtils.registerContract(Hello.class);

		player1.sendAmount(at.getId(), 150);

		assertEquals(850, player1.getBalance(), 0.2);
	}

	/*
	 * Require very high initial amount
	 */
	@Test
	public void testHugeInitialAmount() throws Exception {

		TestAccount player1 = new TestAccount(100000);

		AT at = TestUtils.registerContract(Hello.class);

		player1.sendAmount(at, 150);

		assertEquals(99850, player1.getBalance(), 0.2);
	}

}
