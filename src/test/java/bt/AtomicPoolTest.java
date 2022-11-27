package bt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.compiler.Compiler.Error;
import bt.dapps.AtomicPool;
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
public class AtomicPoolTest extends BT {

	static {
		activateSIP37(true);
	}

	@Test
	public void testAll() throws Exception {
		Compiler comp = BT.compileContract(AtomicPool.class);
		for(Error e : comp.getErrors()) {
			System.err.println(e.getMessage());
		}
		assertTrue(comp.getErrors().size() == 0);

		BT.forgeBlock();
		String name = "AtomicPool";
		SignumValue activationFee = SignumValue.fromSigna(0.5);
		TransactionBroadcast tb = BT.registerContract(BT.PASSPHRASE, comp.getCode(), comp.getDataPages(),
				name, name, null, activationFee,
				SignumValue.fromSigna(3), 1000, null).blockingGet();
		BT.forgeBlock(tb);

		AT contract = BT.getContract(tb.getTransactionId());

		System.out.println("Pool at: " + contract.getId().getID());

		Random random = new Random();
		long secret1 = random.nextLong();
		long secret2 = random.nextLong();
		long secret3 = random.nextLong();
		long secret4 = random.nextLong();
		Register secret = Register.newInstance(secret1, secret2, secret3, secret4);
		Register secretHash = Contract.performSHA256_(secret);
		
		ByteBuffer b = ByteBuffer.allocate(32);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putLong(secretHash.getValue1());
        b.putLong(secretHash.getValue2());
        b.putLong(secretHash.getValue3());
        b.putLong(secretHash.getValue4());
        b.clear();
        
        System.out.println(secretHash.getValue1());
        System.out.println(secretHash.getValue2());
        System.out.println(secretHash.getValue3());
        System.out.println(secretHash.getValue4());

		tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("deposit"), b.array(),
			SignumValue.fromSigna(100).add(activationFee), activationFee, 1000,
			
			// arguments
			BT.getAddressFromPassphrase(BT.PASSPHRASE2).getSignedLongId(), // beneficiary
			40L // timeout in minutes
			);
		long offer = tb.getTransactionId().getSignedLongId();
		System.out.println("offer: " + SignumID.fromLong(offer).getID());
		BT.forgeBlock(tb);
        BT.forgeBlock();
        BT.forgeBlock();

		long numberOfLockedDeposits = BT.getContractFieldValue(contract, comp.getFieldAddress("numberOfLockedDeposits"));
        assertEquals(1L, numberOfLockedDeposits);
        
        // try to refund, but will be invalid at this point
        tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("refund"),
            activationFee, activationFee, 1000,
            // arguments
            offer
            );
        BT.forgeBlock(tb);
        BT.forgeBlock();
        BT.forgeBlock();
        
        // if we try to get the dust, we will not get it, since there are locked deposits
        tb = BT.sendAmount(BT.PASSPHRASE, contract.getId(), activationFee);
        BT.forgeBlock(tb);
        BT.forgeBlock();
        BT.forgeBlock();
        
        SignumValue balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() > Contract.ONE_SIGNA * 100);
        
        // let the timeout to expire and then refund
        for (int i = 0; i < 10; i++) {
          BT.forgeBlock();
        }
        tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("refund"),
            activationFee, activationFee, 1000,
            // arguments
            offer
            );
        BT.forgeBlock(tb);
        BT.forgeBlock();
        BT.forgeBlock();
        
        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() < Contract.ONE_SIGNA);
        
        // get the dust, the balance should be zero now
        tb = BT.sendAmount(BT.PASSPHRASE, contract.getId(), activationFee);
        BT.forgeBlock(tb);
        BT.forgeBlock();
        BT.forgeBlock();
        balance = BT.getContractBalance(contract);
        assertEquals(0, balance.longValue());
        
        // we lock another deposit
        tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("deposit"), b.array(),
            SignumValue.fromSigna(100).add(activationFee), activationFee, 1000,
            
            // arguments
            BT.getAddressFromPassphrase(BT.PASSPHRASE2).getSignedLongId(), // beneficiary
            30L // timeout in minutes
            );
        offer = tb.getTransactionId().getSignedLongId();
        System.out.println("offer: " + SignumID.fromLong(offer).getID());
        
        // we send an invalid claim
        tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("claim"), null,
            activationFee, activationFee, 1000,
            
            // arguments
            offer
            );
        BT.forgeBlock(tb);
        BT.forgeBlock();
        BT.forgeBlock();
        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() > Contract.ONE_SIGNA * 100);

        // now a valid claim
        b = ByteBuffer.allocate(32);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putLong(secret.getValue1());
        b.putLong(secret.getValue2());
        b.putLong(secret.getValue3());
        b.putLong(secret.getValue4());
        b.clear();
        tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("claim"), b.array(),
            activationFee, activationFee, 1000,
            
            // arguments
            offer
            );
        BT.forgeBlock(tb);
        BT.forgeBlock();
        BT.forgeBlock();

        long depositTransaction = BT.getContractFieldValue(contract, comp.getFieldAddress("depositTransaction"));
        System.out.println(SignumID.fromLong(depositTransaction).getID());
        long amount = BT.getContractFieldValue(contract, comp.getFieldAddress("amount"));
        long fee = BT.getContractFieldValue(contract, comp.getFieldAddress("fee"));
        System.out.println(fee);
        System.out.println(amount);
        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() < Contract.ONE_SIGNA);
	}
}
