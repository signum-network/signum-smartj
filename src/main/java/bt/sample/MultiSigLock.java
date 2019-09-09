package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A funds locking smart contract allowing to move funds after a number of
 * a given number of signatures.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class MultiSigLock extends Contract {

    public static final int ACTIVATION_FEE = 30; // expected activation fee in BURST

    // will be much easier when we have support for arrays
    Address owner1, owner2, owner3, owner4, owner5, owner6;
    Address receiver1, receiver2, receiver3, receiver4, receiver5, receiver6;
    long amount1, amount2, amount3, amount4, amount5, amount6;

    long minSignatures;

    /**
     * Constructor, when in blockchain the constructor is called when the first TX
     * reaches the contract.
     */
    public MultiSigLock() {
        owner1 = parseAddress("BURST-438E-UEV4-DCLK-AADEE");
        owner2 = parseAddress("BURST-4HKK-2RAC-EC53-5JAJD");
        owner3 = parseAddress("BURST-RRLA-B3Y7-L4EU-E8PRZ");
        owner4 = null;
        owner5 = null;
        owner6 = null;

        minSignatures = 2;
    }

    /**
     * Sign for sending the given amout to the given receiver address.
     * 
     * A transfer will actually be accomplished only when the minimum number
     * of signatures sending the same amount to the same address are received.
     * 
     */
    public void sign(long amount, Address receiver) {
        // Identify the sender, must be one of the owners
        if (getCurrentTxSender() == owner1) {
            receiver1 = receiver;
            amount1 = amount;
        } else if (getCurrentTxSender() == owner2) {
            receiver2 = receiver;
            amount2 = amount;
        } else if (getCurrentTxSender() == owner3) {
            receiver3 = receiver;
            amount3 = amount;
        } else if (getCurrentTxSender() == owner4) {
            receiver4 = receiver;
            amount4 = amount;
        } else if (getCurrentTxSender() == owner5) {
            receiver5 = receiver;
            amount5 = amount;
        } else if (getCurrentTxSender() == owner6) {
            receiver6 = receiver;
            amount6 = amount;
        } else
            return; // coming from someone else

        long nsigs = 0;
        if (receiver == receiver1 && amount == amount1){
            nsigs++;
        }
        else if (receiver == receiver2 && amount == amount2)
            nsigs++;
        else if (receiver == receiver3 && amount == amount3)
            nsigs++;
        else if (receiver == receiver4 && amount == amount4)
            nsigs++;
        else if (receiver == receiver5 && amount == amount5)
            nsigs++;
        else if (receiver == receiver6 && amount == amount6)
            nsigs++;
        if (nsigs < minSignatures)
            return; // not enough signatures

        if (nsigs >= minSignatures) {
            // make the transfer
            sendAmount(amount, receiver);
            // clear the signatures for the next transfer
            receiver1 = null;
            receiver2 = null;
            receiver3 = null;
            receiver4 = null;
            receiver5 = null;
            receiver6 = null;
        }
    }

    /**
     * Any new transaction received will be handled by this function.
     */
    public void txReceived() {
        // we do nothing, probably just recharging the contract with new funds
    }

    public static void main(String[] args) throws Exception {
        new EmulatorWindow(MultiSigLock.class);
    }
}
