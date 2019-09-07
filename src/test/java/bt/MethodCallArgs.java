package bt;

import bt.Contract;
import bt.compiler.Compiler;
import bt.ui.EmulatorWindow;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AT;

public class MethodCallArgs extends Contract {

    long methodCalled;
    long arg1, arg2, arg3;

	public void method1(long arg1) {
        methodCalled = 1;
        this.arg1 = arg1;
        this.arg2 = -1;
        this.arg3 = -1;
	}

	public void method2(long arg1, long arg2) {
        methodCalled = 2;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = -1;
	}

	public void method3(long arg1, long arg2, long arg3) {
        methodCalled = 3;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
    }

    public void method4(long arg1, long arg2, long arg3) {
        methodCalled = 4;
        this.arg3 = arg3;
        this.arg2 = arg2;
        this.arg1 = arg1;
    }

	@Override
	public void txReceived() {
		methodCalled = 0;
	}

	public static void main(String[] args) throws Exception {
        new EmulatorWindow(MethodCallArgs.class);
	}
}