package bt;

import bt.Contract;
import bt.compiler.Compiler;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;

public class MethodCall extends Contract {

	long methodCalled;

	public void method1() {
		methodCalled = 1;
	}

	public void method2() {
		methodCalled = 2;
	}

	@Override
	public void txReceived() {
		methodCalled = 0;
	}

	public static void main(String[] args) throws Exception {
		BT.forgeBlock();
		Compiler comp = BT.compileContract(MethodCall.class);

		String name = MethodCall.class.getSimpleName() + System.currentTimeMillis();
		SignumAddress creator = SignumCrypto.getInstance().getAddressFromPassphrase(BT.PASSPHRASE);

		BT.registerContract(BT.PASSPHRASE, comp, name, name, SignumValue.fromSigna(1), SignumValue.fromSigna(0.1), 1000)
				.blockingGet();
		BT.forgeBlock();

		AT contract = BT.findContract(creator, name);
		System.out.println(contract.getId().getFullAddress());

		BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method2"), SignumValue.fromSigna(1),
			SignumValue.fromSigna(0.1), 1000).blockingGet();
		BT.forgeBlock();
		BT.forgeBlock();
	}
}
