package bt;

import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.Account;

@Data
public class TestWalett extends BT{
	private SignumAddress address;
	private String passphrase;

	public TestWalett(double initialAmount) {
		String randomPassForInitialFunding = RandomStringUtils.random(49);
		forgeBlock(randomPassForInitialFunding);
		// Create a random passphrase, so test can be run multiple times without
		// rebooting the test server
		this.passphrase = RandomStringUtils.random(49);
		this.address = SignumCrypto.getInstance().getAddressFromPassphrase(passphrase);
		sendAmount(randomPassForInitialFunding, this.address, SignumValue.fromSigna(initialAmount));
	}

	public void sendAmount (TestWalett receiver, double amount) {
		sendAmount(passphrase, receiver.getAddress(), SignumValue.fromSigna(amount));
	}

	public double getBalance () {
		return bns.getAccount(this.address).blockingGet().getUnconfirmedBalance().doubleValue();
	}

	public void sendAmount (SignumAddress receiver, double amount) {
		sendAmount(passphrase, receiver, SignumValue.fromSigna(amount));
		forgeBlock();
	}
}
