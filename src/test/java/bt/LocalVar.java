package bt;

import bt.compiler.Compiler;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;


public class LocalVar extends Contract {

	static final long FEE = Contract.ONE_BURST;

	long amountNoFee;
	long valueTimes2;

	@Override
	public void txReceived() {
		long local = getCurrentTx().getAmount();
		amountNoFee = local + FEE;
	}

	public void setValue(long value) {
		valueTimes2 = value + value;
	}

	public static void main(String[] args) throws Exception {
		// new EmulatorWindow(LocalVar.class);

		BT.forgeBlock();
		Compiler comp = BT.compileContract(LocalVar.class);

		String name = LocalVar.class.getSimpleName() + System.currentTimeMillis();
		SignumAddress creator = SignumCrypto.getInstance().getAddressFromPassphrase(BT.PASSPHRASE);

		BT.registerContract(BT.PASSPHRASE, comp, name, name, SignumValue.fromNQT(FEE), SignumValue.fromSigna(0.1),
				1000).blockingGet();
		BT.forgeBlock();

		AT contract = BT.findContract(creator, name);
		System.out.println(contract.getId().getFullAddress());
		System.out.println(contract.getId().getID());

		BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(10)).blockingGet();
		BT.forgeBlock();
		BT.forgeBlock();

		System.out.println(BT.getContractFieldValue(contract, comp.getField("amountNoFee").getAddress()));

		long value = 512;
		BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("setValue"), SignumValue.fromSigna(1),
				SignumValue.fromSigna(0.1), 1000, value).blockingGet();
		BT.forgeBlock();
		BT.forgeBlock();

		long valueChain = BT.getContractFieldValue(contract, comp.getField("valueTimes2").getAddress());

		System.out.println(value);
		System.out.println(valueChain);
	}
}
