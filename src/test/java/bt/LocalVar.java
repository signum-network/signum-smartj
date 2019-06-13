package bt;

import java.io.IOException;

import bt.Contract;
import bt.compiler.Compiler;
import bt.ui.EmulatorWindow;
import burst.kit.burst.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.ATResponse;

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
		BurstAddress creator = BurstCrypto.getInstance().getBurstAddressFromPassphrase(BT.PASSPHRASE);

		BT.registerContract(BT.PASSPHRASE, comp, name, name, BurstValue.fromPlanck(FEE), BurstValue.fromBurst(0.1),
				1000).blockingGet();
		BT.forgeBlock();

		ATResponse contract = BT.findContract(creator, name);
		System.out.println(contract.getAt().getFullAddress());
		System.out.println(contract.getAt().getID());

		BT.sendAmount(BT.PASSPHRASE, contract.getAt(), BurstValue.fromBurst(10)).blockingGet();
		BT.forgeBlock();
		BT.forgeBlock();

		System.out.println(BT.getContractFieldValue(contract, comp.getField("amountNoFee").getAddress()));

		long value = 512;
		BT.callMethod(BT.PASSPHRASE, contract.getAt(), comp.getMethod("setValue"), BurstValue.fromBurst(1),
				BurstValue.fromBurst(0.1), 1000, value).blockingGet();
		BT.forgeBlock();
		BT.forgeBlock();

		long valueChain = BT.getContractFieldValue(contract, comp.getField("valueTimes2").getAddress());

		System.out.println(value);
		System.out.println(valueChain);
	}
}
