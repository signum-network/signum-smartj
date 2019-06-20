package bt;

import bt.Contract;
import bt.compiler.Compiler;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;

public class MethodCalls extends Contract {

	public void method1() {
		sendMessage("method1", getCurrentTx().getSenderAddress());
	}

	public void method2() {
		sendMessage("method2", getCurrentTx().getSenderAddress());
	}

	@Override
	public void txReceived() {
		sendMessage("txReceived", getCurrentTx().getSenderAddress());
		sendMessage(getCurrentTx().getMessage(), getCurrentTx().getSenderAddress());
	}

	public static void main(String[] args) throws Exception {
		BT.forgeBlock();
		Compiler comp = BT.compileContract(MethodCalls.class);

		String name = MethodCalls.class.getSimpleName() + System.currentTimeMillis();
		BurstAddress creator = BurstCrypto.getInstance().getBurstAddressFromPassphrase(BT.PASSPHRASE);

		BT.registerContract(BT.PASSPHRASE, comp, name, name, BurstValue.fromBurst(1), BurstValue.fromBurst(0.1), 1000)
				.blockingGet();
		BT.forgeBlock();

		AT contract = BT.findContract(creator, name);
		System.out.println(contract.getId().getFullAddress());

		BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method2"), BurstValue.fromBurst(1),
			BurstValue.fromBurst(0.1), 1000).blockingGet();
		BT.forgeBlock();
		BT.forgeBlock();
	}
}
