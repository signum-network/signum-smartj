package bt.sample;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import bt.BT;
import bt.Contract;
import bt.compiler.Compiler;
import signumj.entity.SignumValue;
import signumj.entity.response.TransactionBroadcast;

/**
 * Sends back you 1 token for every SIGNA received and vice-versa.
 * 
 * First this contract issues a new token, this is the token that
 * is sent back and forth.
 * 
 * @author jjos
 *
 */
public class TokenFactory extends Contract {
	
	long namePart1;
	long namePart2;
	long decimalPlaces;
	long factor;
	
	long tokenId;
	long amount;
	long quantity;
	
	@Override
	public void txReceived() {
		if(tokenId == 0) {
			tokenId = issueAsset(namePart1, namePart2, decimalPlaces);
			return;
		}
		amount = (getCurrentTxAmount() + getActivationFee());		
		if(amount > 0) {
			quantity = amount / factor;
			mintAsset(tokenId, quantity);
			// sends back tokens 1-1
			sendAmount(tokenId, quantity, getCurrentTxSender());
			return;
		}
		
		quantity = getCurrentTxAmount(tokenId);
		if(quantity > 0) {
			amount = quantity * factor;
			sendAmount(amount, getCurrentTxSender());
		}
	}
	
	public static void main(String[] args) throws Exception {
		// Code to deploy this contract
		String passphrase = "ENTER YOU TESTNET PASSPHRASE HERE";
		BT.setNodeAddress(BT.NODE_LOCAL_TESTNET);
		
		Compiler comp = BT.compileContract(TokenFactory.class);
		
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

		TransactionBroadcast tb = BT.registerContract(passphrase, comp.getCode(), comp.getDataPages(),
				"TokenFactory", "Smart contract sending tokens per SIGNA and vice-versa.", data, SignumValue.fromSigna(0.3),
				SignumValue.fromSigna(.5), 1000, null).blockingGet();
		
		System.out.println("Contract id: " + tb.getTransactionId().getID());
	}
	
}
