package bt.sample;

import bt.Address;
import bt.ContractFunction;
import bt.FunctionBasedContract;
import bt.ui.EmulatorWindow;

/**
 * Fairly useless contract to demonstrate FunctionBasedContracts.
 * Also, this AT is over 2KB.
 */
public class Counter extends FunctionBasedContract {

    Address creator;
    long count;
    boolean isPublic;

    public Counter() {
        creator = parseAddress("BURST-WQFQ-W64L-HT3P-C9YNM");
        count = 0;
        isPublic = false;
    }

    @ContractFunction(80) // P for public
    public void makePublic() {
        if (creator.equals(getCurrentTx().getSenderAddress())) {
            isPublic = true;
            sendMessage("Counter is now public.", getCurrentTx().getSenderAddress());
        }
    }

    @ContractFunction(78) // N for not public
    public void makeNotPublic() {
        if (creator.equals(getCurrentTx().getSenderAddress())) {
            isPublic = false;
            sendMessage("Counter is now private.", getCurrentTx().getSenderAddress());
        }
    }

    @ContractFunction(73) // I for increase
    public void increaseCount() {
        if (isPublic || creator.equals(getCurrentTx().getSenderAddress())) {
            if (count < Long.MAX_VALUE) { // Prevent overflow
                sendMessage("Increased!", getCurrentTx().getSenderAddress());
            } else {
                sendMessage("Limit reached!", getCurrentTx().getSenderAddress());
            }
        }
    }

    @ContractFunction(83) // S for send
    public void sendCount() {
        if (isPublic || creator.equals(getCurrentTx().getSenderAddress())) {
            sendAmount(count, creator);
            count = 0;
            sendMessage("Sending count planck", getCurrentTx().getSenderAddress());
        }
    }

    @ContractFunction(82) // R for reset
    public void resetCount() {
        if (isPublic || creator.equals(getCurrentTx().getSenderAddress())) {
            count = 0;
            sendMessage("Counter reset.", getCurrentTx().getSenderAddress());
        }
    }

    @Override
    public void onNoFunctionCalled() {
        sendMessage("No function called!", getCurrentTx().getSenderAddress());
    }

    public static void main(String[] args) {
        compile();
        new EmulatorWindow(Counter.class);
    }
}
