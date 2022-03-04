package bt;

import bt.sample.Hello;
import org.junit.Test;
import signumj.entity.response.AT;

import static org.junit.Assert.assertEquals;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
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

		// Fill the contract with 3 times the max payment value
		player1.sendAmount(at.getId(), 150);
		forgeBlock();

		assertEquals(850, player1.getBalance(), 0.2);
	}

}
