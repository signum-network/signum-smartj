package bt.sample;

import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A funds locking smart contract allowing to move funds after a pre-defined
 * number of signatures.
 * 
 * Each of the hardcoded owners has the ability to *sign* (or *vote*) for a
 * funds transfer for a given address. When the minium number of signatures is
 * reached (and all signees agree in terms of the amount) the transfer takes
 * place and the signatures (or votes) are erased.
 * 
 * This contract can be reused many times and can also be recharged with more
 * balance by just sending more transactions to it.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class MultiSigLock extends Contract {

    public static final int ACTIVATION_FEE = 30; // expected activation fee in BURST

    // will be much easier when we have BlockTalk support for arrays
    Address owner1, owner2, owner3, owner4, owner5;
    Address receiver1, receiver2, receiver3, receiver4, receiver5;
    long amount1, amount2, amount3, amount4, amount5;

    long minSignatures, nSignatures;

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

        minSignatures = 2;
    }

    /**
     * Sign for sending the given amout to the given receiver address.
     * 
     * A transfer will actually be accomplished only when the minimum number of
     * signatures sending the same amount to the same address are received.
     * 
     */
    public void sign(long amount, Address receiver) {
        // Identify the sender, must be one of the owners
        if (getCurrentTxSender() == owner1) {
            receiver1 = receiver;
            amount1 = amount;
        }
        if (getCurrentTxSender() == owner2) {
            receiver2 = receiver;
            amount2 = amount;
        }
        if (getCurrentTxSender() == owner3) {
            receiver3 = receiver;
            amount3 = amount;
        }
        if (getCurrentTxSender() == owner4) {
            receiver4 = receiver;
            amount4 = amount;
        }
        if (getCurrentTxSender() == owner5) {
            receiver5 = receiver;
            amount5 = amount;
        }

        nSignatures = 0;
        if (receiver == receiver1 && amount == amount1)
            nSignatures++;
        if (receiver == receiver2 && amount == amount2)
            nSignatures++;
        if (receiver == receiver3 && amount == amount3)
            nSignatures++;
        if (receiver == receiver4 && amount == amount4)
            nSignatures++;
        if (receiver == receiver5 && amount == amount5)
            nSignatures++;

        if (nSignatures < minSignatures)
            return; // not enough signatures

        if (nSignatures >= minSignatures && receiver != null) {
            // make the transfer
            sendAmount(amount, receiver);
            // clear the signatures for the next transfer
            receiver1 = null;
            receiver2 = null;
            receiver3 = null;
            receiver4 = null;
            receiver5 = null;
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
