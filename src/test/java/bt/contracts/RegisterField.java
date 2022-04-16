package bt.contracts;

import bt.Contract;
import bt.Register;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A smart contract that simply echoes the message received.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class RegisterField extends Contract {

	Register msg;
	
	/**
	 * Any new transaction received will be handled by this function.
	 */
	public void txReceived(){
		msg = getCurrentTx().getMessage();

		if(msg.getValue1() != 0)
			sendMessage(msg, getCurrentTx().getSenderAddress());
	}

	public static void main(String[] args) {
		new EmulatorWindow(RegisterField.class);
	}
}


