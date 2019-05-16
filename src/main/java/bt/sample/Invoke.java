package bt.sample;

import bt.ContractFunction;
import bt.FunctionBasedContract;
import bt.ui.EmulatorWindow;

public class Invoke extends FunctionBasedContract {

    @ContractFunction(1)
    public void f1() {
        sendMessage("f1!", getCurrentTx().getSenderAddress());
    }

    @ContractFunction(2)
    public void f2() {
        sendMessage("f2!", getCurrentTx().getSenderAddress());
    }

    public static void main(String[] args) {
        compile();
    }

    @Override
    public void onNoFunctionCalled() {
        if (receivedMessage.getValue1() == 0)
        sendMessage("What was that?", getCurrentTx().getSenderAddress());
    }
}
