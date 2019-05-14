package bt;

public class FunctionBasedContract extends Contract {
    long methodIdentifier;

    /**
     * This method does not actually get compiled for this class - this is just for the emulator.
     */
    @Override
    public final void txReceived() {
        methodIdentifier = getMessage(getCurrentTx()).value[0]; // Could this be made more efficient?
        if (methodIdentifier == 12345L) {

        }
    }

    public static void main(String[] args) {
        compile();
    }
}
