package bt;

import bt.Contract;
import bt.ui.EmulatorWindow;

public class MethodCalls extends Contract {

	public void method1(){
		sendMessage("method1", getCurrentTx().getSenderAddress());
	}

	public void method2(){
		sendMessage("method2", getCurrentTx().getSenderAddress());
	}

	@Override
	public void txReceived() {
		sendMessage("txReceived", getCurrentTx().getSenderAddress());
	}

	public static void main(String[] args) {
		new EmulatorWindow(MethodCalls.class);
	}
}
