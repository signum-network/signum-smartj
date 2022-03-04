package bt;

import org.apache.commons.lang3.RandomStringUtils;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;

public class TestAccount extends BT {
	public SignumAddress getAddress() {
		return address;
	}

	public void setAddress(SignumAddress address) {
		this.address = address;
	}

	private SignumAddress address;

	public String getPassphrase() {
		return passphrase;
	}

	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}

	private String passphrase;

	public TestAccount(double initialAmount) {
		String randomPassForInitialFunding = RandomStringUtils.random(49);
		forgeBlock(randomPassForInitialFunding);

		// we forge block until forgedAmount > initialAmount
		for (int i = 0; TestUtils.getBalance(randomPassForInitialFunding) < initialAmount; i++) {
			forgeBlock(randomPassForInitialFunding);
		}

		// Create a random passphrase, so test can be run multiple times without
		// rebooting the test server
		this.passphrase = RandomStringUtils.random(49);
		this.address = SignumCrypto.getInstance().getAddressFromPassphrase(passphrase);
		sendAmount(randomPassForInitialFunding, this.address, SignumValue.fromSigna(initialAmount));
	}

	public void sendAmount(TestAccount receiver, double amount) {
		this.sendAmount(passphrase, receiver.getAddress(), SignumValue.fromSigna(amount));
	}

	public double getBalance() {
		return bns.getAccount(this.address).blockingGet().getUnconfirmedBalance().doubleValue();
	}

	public void sendAmount(SignumAddress receiver, double amount) {
		sendAmount(passphrase, receiver, SignumValue.fromSigna(amount));
		forgeBlock();
	}

	public void sendAmount(AT receiver, double amount) {
		this.sendAmount(receiver.getId(), amount);
	}


}
