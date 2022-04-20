package bt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.compiler.Compiler.Error;
import bt.sample.AssetQuantityReceived;
import bt.sample.TokenFactory;
import bt.sample.TokenPlusSignaEcho;
import signumj.entity.SignumID;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.entity.response.AssetBalance;
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
	public void testAll() throws Exception {
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
		tb = BT.callMethod(BT.PASSPHRASE, contractEcho.getId(), compEcho.getMethod("echoToken"), assetId, quantity, SignumValue.fromSigna(1.0), SignumValue.fromSigna(0.1), 1000, assetId.getSignedLongId());
		
		forgeBlock(tb);
		forgeBlock();
		forgeBlock();

		txs = BT.getNode().getAccountTransactions(contractEcho.getId(), -1, -1, false).blockingGet();
		assertTrue(txs.length > 0);
		found = false;
		for(Transaction tx : txs) {
			if(tx.getSender().getSignedLongId() == contractEcho.getId().getSignedLongId()) {
				if(tx.getAmount().longValue() == 80000000 && tx.getAttachment() instanceof AssetTransferAttachment) {
					AssetTransferAttachment attachment = (AssetTransferAttachment) tx.getAttachment();
					if(attachment.getQuantityQNT().equals(Long.toString(quantity.longValue()))){
						found = true;
					}
				}
			}
		}
		assertTrue(found);
	}
}
