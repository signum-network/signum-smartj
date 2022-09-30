package bt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.compiler.Compiler.Error;
import bt.contracts.DistributeToHolders;
import bt.sample.AssetQuantityReceived;
import bt.sample.TokenFactory;
import bt.sample.TokenPlusSignaEcho;
import bt.sample.TokenPlusSignaEchoHalf;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumID;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.entity.response.Account;
import signumj.entity.response.AssetBalance;
import signumj.entity.response.IndirectIncoming;
import signumj.entity.response.Transaction;
import signumj.entity.response.TransactionBroadcast;
import signumj.response.attachment.AssetTransferAttachment;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 *
 * @author jjos
 */
public class AssetTest extends BT {

	static {
		activateSIP37(true);
	}

	@Test
	public void testGeneral() throws Exception {
		Compiler comp = BT.compileContract(TokenFactory.class);
		for(Error e : comp.getErrors()) {
			System.err.println(e.getMessage());
		}
		assertTrue(comp.getErrors().size() == 0);

		String name = "AssetFactory" + System.currentTimeMillis();

		String tokenName = "FACTORY";
		long decimalPlaces = 4;
		long factor = 10000;
		byte[] nameBytes = tokenName.getBytes(StandardCharsets.UTF_8);
		ByteBuffer nameBytesBuffer = ByteBuffer.allocate(16);
		nameBytesBuffer.order(ByteOrder.LITTLE_ENDIAN);
		nameBytesBuffer.put(nameBytes);
		nameBytesBuffer.clear();

		long []data = {
				nameBytesBuffer.getLong(),
				nameBytesBuffer.getLong(),
				decimalPlaces,
				factor
		};

		BT.forgeBlock();
		SignumValue activationFee = SignumValue.fromSigna(0.3);
		TransactionBroadcast tb = BT.registerContract(BT.PASSPHRASE, comp.getCode(), comp.getDataPages(),
				name, name, data, activationFee,
				SignumValue.fromSigna(.5), 1000, null).blockingGet();
		BT.forgeBlock(tb);

		AT contract = BT.getContract(tb.getTransactionId());

		System.out.println("factory at :" + contract.getId().getID());

		// initialize the contract to create the token
		tb = BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(160));
		BT.forgeBlock(tb);
		BT.forgeBlock();
		BT.forgeBlock();

		SignumID assetId = SignumID.fromLong(BT.getContractFieldValue(contract, comp.getFieldAddress("tokenId")));
		System.out.println("assetId: " + assetId.getID());
		assertTrue(assetId.getSignedLongId() != 0L);

		long amount = 10 * Contract.ONE_SIGNA;
		tb = sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromNQT(amount).add(activationFee));
		forgeBlock(tb);
		forgeBlock();
		forgeBlock();
		
		boolean assetReceived = false;
		AssetBalance[] balances = BT.getNode().getAssetBalances(assetId, -1, -1).blockingGet();
		for(AssetBalance b : balances) {
			if(b.getAccountAddress().getSignedLongId() == BT.getAddressFromPassphrase(BT.PASSPHRASE).getSignedLongId()) {
				if(b.getBalance().longValue() == amount/factor) {
					assetReceived = true;
					break;
				}
			}
		}
		assertTrue("asset not received back correctly", assetReceived);
		
		// send back some asset to get back SIGNA
		tb = sendAsset(BT.PASSPHRASE, contract.getId(), assetId, SignumValue.fromNQT(2000), activationFee, SignumValue.fromSigna(0.1), 1000, null);
		forgeBlock(tb);
		forgeBlock();
		forgeBlock();
		Transaction[] txs = BT.getNode().getAccountTransactions(BT.getAddressFromPassphrase(BT.PASSPHRASE), 0, 1, false).blockingGet();
		assertTrue(txs.length > 0);
		assertEquals(2000_0000L, txs[0].getAmount().longValue());
		
		// the contract should have burned the tokens
		boolean found = false;
		balances = BT.getNode().getAssetBalances(assetId, -1, -1).blockingGet();
		for(AssetBalance b : balances) {
			if(b.getAccountAddress().getSignedLongId() == contract.getId().getSignedLongId()) {
				found = true;
			}
		}
		assertFalse(found);
		
		// send out our asset
		AT contractReceived = registerContract(AssetQuantityReceived.class, SignumValue.fromSigna(0.2));
		System.out.println("received at :" + contractReceived.getId().getID());

		Compiler compReceived = BT.compileContract(AssetQuantityReceived.class);
		assertTrue(compReceived.getErrors().size() == 0);

		SignumValue quantity = SignumValue.fromNQT(1000);
		tb = BT.callMethod(BT.PASSPHRASE, contractReceived.getId(), compReceived.getMethod("checkAssetQuantity"), assetId, quantity, contractReceived.getMinimumActivation(), SignumValue.fromSigna(0.1), 1000, assetId.getSignedLongId());
		
		forgeBlock(tb);
		forgeBlock();
		forgeBlock();
		
		long assetIdReceived = BT.getContractFieldValue(contractReceived, compReceived.getFieldAddress("assetId"));
		long quantityReceived = BT.getContractFieldValue(contractReceived, compReceived.getFieldAddress("quantity"));
		
		assertEquals(assetId.getSignedLongId(), assetIdReceived);
		assertEquals(quantity.longValue(), quantityReceived);
		
		// send twice in the same block, 1*amount and 2*amount, should get back 3*amount tokens
		BT.forgeBlock(BT.PASSPHRASE2);
		tb = sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(amount*2).add(activationFee));
		TransactionBroadcast tb2 = sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(amount).add(activationFee));
		forgeBlock(tb, tb2);
		forgeBlock();
		forgeBlock();
		
		assetReceived = false;
		balances = BT.getNode().getAssetBalances(assetId, -1, -1).blockingGet();
		for(AssetBalance b : balances) {
			if(b.getAccountAddress().getSignedLongId() == BT.getAddressFromPassphrase(BT.PASSPHRASE2).getSignedLongId()) {
				if(b.getBalance().longValue() == 3*amount/factor) {
					assetReceived = true;
					break;
				}
			}
		}
		assertTrue("asset not received back correctly", assetReceived);
		
		
		// send out our asset
		AT contractEcho = registerContract(TokenPlusSignaEcho.class, SignumValue.fromSigna(0.2));
		System.out.println("echo at :" + contractEcho.getId().getID());

		Compiler compEcho = BT.compileContract(TokenPlusSignaEcho.class);
		assertTrue(compEcho.getErrors().size() == 0);

		quantity = SignumValue.fromNQT(1000);
		tb = BT.callMethod(BT.PASSPHRASE, contractEcho.getId(), compEcho.getMethod("echoToken"), assetId, quantity,
				SignumValue.fromSigna(10.2), SignumValue.fromSigna(0.1), 1000, assetId.getSignedLongId());
		
		forgeBlock(tb);
		forgeBlock();
		forgeBlock();

		txs = BT.getNode().getAccountTransactions(contractEcho.getId(), -1, -1, false).blockingGet();
		assertTrue(txs.length > 0);
		found = false;
		for(Transaction tx : txs) {
			if(tx.getSender().getSignedLongId() == contractEcho.getId().getSignedLongId()) {
				if(tx.getAmount().equals(SignumValue.fromSigna(10)) && tx.getAttachment() instanceof AssetTransferAttachment) {
					AssetTransferAttachment attachment = (AssetTransferAttachment) tx.getAttachment();
					if(attachment.getQuantityQNT().equals(Long.toString(quantity.longValue()))){
						found = true;
					}
				}
			}
		}
		assertTrue(found);
		
		// all should have left the contract
		Account account = BT.getNode().getAccount(contractEcho.getId()).blockingGet();
		assertEquals(0, account.getAssetBalances().length);
		assertTrue(account.getBalance().longValue() < Contract.ONE_SIGNA);

		// register contract
		AT contractEchoHalf = registerContract(TokenPlusSignaEchoHalf.class, SignumValue.fromSigna(0.2));
		System.out.println("echo half at :" + contractEchoHalf.getId().getID());

		quantity = SignumValue.fromNQT(1000);
		tb = BT.sendAsset(BT.PASSPHRASE, contractEchoHalf.getId(), assetId, quantity,
				SignumValue.fromSigna(10.2), SignumValue.fromSigna(0.1), 1000, null);
		
		forgeBlock(tb);
		forgeBlock();
		forgeBlock();

		txs = BT.getNode().getAccountTransactions(contractEchoHalf.getId(), -1, -1, false).blockingGet();
		assertTrue(txs.length > 0);
		found = false;
		for(Transaction tx : txs) {
			if(tx.getSender().getSignedLongId() == contractEchoHalf.getId().getSignedLongId()) {
				if(tx.getAmount().equals(SignumValue.fromSigna(5))){
					found = true;
				}
			}
		}
		assertTrue(found);
		found = false;
		for(Transaction tx : txs) {
			if(tx.getSender().getSignedLongId() == contractEchoHalf.getId().getSignedLongId()) {
				if(tx.getAmount().equals(SignumValue.fromSigna(0)) && tx.getAttachment() instanceof AssetTransferAttachment) {
					AssetTransferAttachment attachment = (AssetTransferAttachment) tx.getAttachment();
					if(attachment.getQuantityQNT().equals(Long.toString(quantity.longValue()/2))){
						found = true;
					}
				}
			}
		}
		assertTrue(found);
		
		// half should have left the contract
		account = BT.getNode().getAccount(contractEchoHalf.getId()).blockingGet();
		assertEquals(1, account.getAssetBalances().length);
		assertEquals(quantity.longValue()/2, account.getAssetBalances()[0].getBalance().longValue());
		assertTrue(account.getBalance().longValue() > SignumValue.fromSigna(4).longValue());

	
		// create more assets and send them to the contract
		contractEchoHalf = registerContract(TokenPlusSignaEchoHalf.class, SignumValue.fromSigna(0.2));
		System.out.println("echo new half at :" + contractEchoHalf.getId().getID());

		BT.forgeBlock();
		byte[] unsigned = BT.getNode().generateIssueAssetTransaction(BT.bc.getPublicKey(BT.PASSPHRASE), "TESTD2", "Test token for the distribution",
				SignumValue.fromNQT(2_000_000), 3, SignumValue.fromSigna(150), 1000).blockingGet();
		byte[] signedTransactionBytes = BT.bc.signTransaction(BT.PASSPHRASE, unsigned);
        tb = BT.getNode().broadcastTransaction(signedTransactionBytes).blockingGet();
        forgeBlock(tb);
        SignumID assetId2 = tb.getTransactionId();
		System.out.println("asset2 :" + assetId2.getID());
		unsigned = BT.getNode().generateIssueAssetTransaction(BT.bc.getPublicKey(BT.PASSPHRASE), "TESTD3", "Test token for the distribution",
				SignumValue.fromNQT(2_000_000), 3, SignumValue.fromSigna(150), 1000).blockingGet();
		signedTransactionBytes = BT.bc.signTransaction(BT.PASSPHRASE, unsigned);
        tb = BT.getNode().broadcastTransaction(signedTransactionBytes).blockingGet();
        forgeBlock(tb);
        SignumID assetId3 = tb.getTransactionId();
		System.out.println("asset3 :" + assetId3.getID());
		unsigned = BT.getNode().generateIssueAssetTransaction(BT.bc.getPublicKey(BT.PASSPHRASE), "TESTD4", "Test token for the distribution",
				SignumValue.fromNQT(2_000_000), 3, SignumValue.fromSigna(150), 1000).blockingGet();
		signedTransactionBytes = BT.bc.signTransaction(BT.PASSPHRASE, unsigned);
        tb = BT.getNode().broadcastTransaction(signedTransactionBytes).blockingGet();
        forgeBlock(tb);
        SignumID assetId4 = tb.getTransactionId();
		System.out.println("asset4 :" + assetId4.getID());

		Map<SignumID, SignumValue> assets = new HashMap<SignumID, SignumValue>();
		assets.put(assetId, quantity);
		assets.put(assetId2, quantity);
		assets.put(assetId3, quantity);
		assets.put(assetId4, quantity);
		quantity = SignumValue.fromNQT(1000);
		unsigned = BT.getNode().generateTransferAssetMultiTransaction(BT.bc.getPublicKey(BT.PASSPHRASE),
				contractEchoHalf.getId(), assets, SignumValue.fromSigna(10.2), SignumValue.fromSigna(0.1), 1000).blockingGet();
		signedTransactionBytes = BT.bc.signTransaction(BT.PASSPHRASE, unsigned);
        tb = BT.getNode().broadcastTransaction(signedTransactionBytes).blockingGet();
		forgeBlock(tb);
		forgeBlock();
		forgeBlock();

		txs = BT.getNode().getAccountTransactions(contractEchoHalf.getId(), -1, -1, false).blockingGet();
		assertEquals(6, txs.length);		
		for (int i = 0; i < txs.length; i++) {
			Transaction tx = txs[i];
			
			if(tx.getAmount().longValue() == 0L) {			
				assertEquals(Long.toString(quantity.longValue()/2), ((AssetTransferAttachment)tx.getAttachment()).getQuantityQNT());
				String assetIdTx = ((AssetTransferAttachment)txs[i].getAttachment()).getAsset();
				assertTrue(assetIdTx.equals(assetId.getID()) || assetIdTx.equals(assetId2.getID()) || assetIdTx.equals(assetId3.getID()) || assetIdTx.equals(assetId4.getID()));
			}
			else if (tx.getRecipient().getSignedLongId() != contractEchoHalf.getId().getSignedLongId()){
				assertEquals(SignumValue.fromSigna(5), tx.getAmount());
			}
		}
		
		// half should have left the contract
		account = BT.getNode().getAccount(contractEchoHalf.getId()).blockingGet();
		assertEquals(4, account.getAssetBalances().length);
		assertEquals(quantity.longValue()/2, account.getAssetBalances()[0].getBalance().longValue());
		assertEquals(quantity.longValue()/2, account.getAssetBalances()[1].getBalance().longValue());
		assertEquals(quantity.longValue()/2, account.getAssetBalances()[2].getBalance().longValue());
		assertEquals(quantity.longValue()/2, account.getAssetBalances()[3].getBalance().longValue());
		assertTrue(account.getBalance().longValue() > SignumValue.fromSigna(4).longValue());
	}
	
	
	@Test
	public void testDistribute() throws Exception {
		
		SignumCrypto crypto = SignumCrypto.getInstance();
		
		Compiler comp = BT.compileContract(DistributeToHolders.class);
		for(Error e : comp.getErrors()) {
			System.err.println(e.getMessage());
		}
		assertTrue(comp.getErrors().size() == 0);
		
		BT.forgeBlock();
		byte[] unsigned = BT.getNode().generateIssueAssetTransaction(BT.bc.getPublicKey(BT.PASSPHRASE), "TESTD", "Test token for the distribution",
				SignumValue.fromNQT(2_000_000), 3, SignumValue.fromSigna(150), 1000).blockingGet();
		
		byte[] signedTransactionBytes = BT.bc.signTransaction(BT.PASSPHRASE, unsigned);
        TransactionBroadcast tb = BT.getNode().broadcastTransaction(signedTransactionBytes).blockingGet();
        forgeBlock(tb);
        
        SignumID assetId = tb.getTransactionId();
		System.out.println("asset :" + assetId.getID());
        
        for (int i = 1; i <= 1000; i++) {
        	// create a 1000 holders
			BT.sendAsset(BT.PASSPHRASE, crypto.getAddressFromPassphrase(Integer.toString(i)), assetId, SignumValue.fromNQT(1000), null, SignumValue.fromSigna(0.1),
					1000, null);
		}
		BT.forgeBlock();
		BT.forgeBlock();
		
		// place an ask order
		tb = BT.sendAmount(BT.PASSPHRASE, crypto.getAddressFromPassphrase("1"), SignumValue.fromSigna(0.1));
		forgeBlock(tb);
		unsigned = BT.getNode().generatePlaceAskOrderTransaction(crypto.getPublicKey("1"), assetId,
				SignumValue.fromNQT(1000), SignumValue.fromNQT(10), SignumValue.fromSigna(0.1), 1000).blockingGet();
		signedTransactionBytes = BT.bc.signTransaction("1", unsigned);
        tb = BT.getNode().broadcastTransaction(signedTransactionBytes).blockingGet();
        forgeBlock(tb);
		
		SignumValue activationFee = SignumValue.fromSigna(0.1);
		String name = "dist" + System.currentTimeMillis();
		tb = BT.registerContract(BT.PASSPHRASE, comp.getCode(), comp.getDataPages(),
				name, name, new long[] {assetId.getSignedLongId()}, activationFee,
				SignumValue.fromSigna(.5), 1000, null).blockingGet();
		BT.forgeBlock(tb);

		AT contract = BT.getContract(tb.getTransactionId());

		System.out.println("dist at :" + contract.getId().getID());
		
		// send the amount in SIGNA to distribute to holders, but not enough for it to run the distribution
		tb = BT.sendAmount(BT.PASSPHRASE, contract.getId(), activationFee);
		BT.forgeBlock(tb);
		BT.forgeBlock();
		BT.forgeBlock();

		// no tx should have gone out
		Transaction[] txs = BT.getNode().getAccountTransactions(contract.getId(), 0, 100, true).blockingGet();
		assertEquals(1, txs.length);

		// send the amount in SIGNA to distribute to holders
		SignumValue amountSent = SignumValue.fromSigna(100);
		tb = BT.sendAmount(BT.PASSPHRASE, contract.getId(), amountSent);
		BT.forgeBlock(tb);
		BT.forgeBlock();
		BT.forgeBlock();
		
		SignumAddress account1 = crypto.getAddressFromPassphrase("1");
		txs = BT.getNode().getAccountTransactions(account1, 0, 1, true).blockingGet();
		assertEquals(2, txs.length);
		IndirectIncoming indirect = BT.getNode().getIndirectIncoming(account1, txs[0].getId()).blockingGet();
		assertNotNull(indirect);
		assertEquals(0, indirect.getQuantity().longValue());
		assertTrue(amountSent.longValue() / indirect.getAmount().longValue() > 1000);
		
		SignumAddress account1000 = crypto.getAddressFromPassphrase("1000");
		txs = BT.getNode().getAccountTransactions(account1000, 0, 1, true).blockingGet();
		assertEquals(2, txs.length);
		indirect = BT.getNode().getIndirectIncoming(account1000, txs[0].getId()).blockingGet();
		assertNotNull(indirect);
		assertEquals(0, indirect.getQuantity().longValue());
		assertTrue(amountSent.longValue() / indirect.getAmount().longValue() > 1000);


		// send the amount in SIGNA+token to distribute to holders
		tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("distributeToken"), assetId, SignumValue.fromNQT(20000), amountSent,
				SignumValue.fromSigna(0.1), 1000, assetId.getSignedLongId());
		BT.forgeBlock(tb);
		BT.forgeBlock();
		BT.forgeBlock();
		
		txs = BT.getNode().getAccountTransactions(account1, 0, 1, true).blockingGet();
		assertEquals(2, txs.length);
		indirect = BT.getNode().getIndirectIncoming(account1, txs[0].getId()).blockingGet();
		assertNotNull(indirect);
		assertEquals(10, indirect.getQuantity().longValue());
		assertTrue(amountSent.longValue() / indirect.getAmount().longValue() > 1000);
		
		txs = BT.getNode().getAccountTransactions(account1000, 0, 1, true).blockingGet();
		assertEquals(2, txs.length);
		indirect = BT.getNode().getIndirectIncoming(account1000, txs[0].getId()).blockingGet();
		assertNotNull(indirect);
		assertEquals(10, indirect.getQuantity().longValue());
		assertTrue(amountSent.longValue() / indirect.getAmount().longValue() > 1000);
		
		
		// one account sending the distribution, should not receive
		BT.forgeBlock("1");
		unsigned = BT.getNode().generateDistributeToAssetHolders(crypto.getPublicKey("1"), assetId, SignumValue.fromNQT(0),
				SignumValue.fromSigna(0.1), SignumID.fromLong(0), SignumValue.fromNQT(0), SignumValue.fromSigna(1.1), 1000).blockingGet();
		signedTransactionBytes = BT.bc.signTransaction("1", unsigned);
        tb = BT.getNode().broadcastTransaction(signedTransactionBytes).blockingGet();
        BT.forgeBlock(tb);
        
        indirect = BT.getNode().getIndirectIncoming(account1000, tb.getTransactionId()).blockingGet();
        Exception unknownTx = null;
        try {
        	// this should throw an exception as the sender should not be a receiver
        	indirect = BT.getNode().getIndirectIncoming(account1, tb.getTransactionId()).blockingGet();
        }
        catch (Exception e) {
        	unknownTx = e;
		}
        assertNotNull(unknownTx);
	}
}
