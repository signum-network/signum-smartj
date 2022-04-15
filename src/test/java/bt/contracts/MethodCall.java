package bt.contracts;

import bt.Contract;

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
}
