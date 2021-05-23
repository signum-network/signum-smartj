package bt.sample;

import bt.Address;
import bt.BT;
import bt.Contract;
import bt.Timestamp;
import bt.compiler.TargetCompilerVersion;
import bt.compiler.CompilerVersion;
import bt.ui.EmulatorWindow;

@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class DappositoryToken extends Contract {

    public static final long MINIMUM_DONATION_COSTS = 100*ONE_BURST;
    public static final long FEATURE_PER_MINUTE_COSTS = ONE_BURST/4;

    private long version = 1;
    private boolean isActive = true;
    private Address inventor = parseAddress("BURST-9K9L-4CB5-88Y5-F5G4Z"); // BURST-9K9L-4CB5-88Y5-F5G4Z
    private long donated = 0L;
    private long donationCount = 0L;
    private Timestamp featureActiveUntilTimestamp = getCreationTimestamp();
    private Address owner = getCreator();

    private boolean senderIsOwner(){
        return getCurrentTx().getSenderAddress().equals(owner);
    }

    public void feature(){
        Timestamp currentBlock = getCurrentTxTimestamp();
        if(senderIsOwner() &&
            isActive &&
            getCurrentTxAmount() >= MINIMUM_DONATION_COSTS &&
            currentBlock.ge(featureActiveUntilTimestamp)){
            featureActiveUntilTimestamp = currentBlock.addMinutes(getCurrentTxAmount()/FEATURE_PER_MINUTE_COSTS);
            sendMessage("Featuring started", owner);
            sendAmount(getCurrentTxAmount(), inventor);
        }
        else{
            refund();
        }
    }

    public void transfer(Address newOwner){
        if(senderIsOwner() && isActive){
            sendMessage("Ownership transferred", owner);
            sendMessage("Ownership granted", newOwner);
            owner = newOwner;
        }else{
            refund();
        }
    }

    public void deactivate(){
        if(senderIsOwner() && isActive){
            isActive = false;
            sendMessage("Application deactivated", owner);
            sendBalance(owner);
        }
        else{
            refund();
        }
    }

    public void txReceived(){

        if(getCurrentTxSender() == owner){
            return;
        }

        if(!isActive || getCurrentTxAmount() < MINIMUM_DONATION_COSTS){
            sendMessage("Donation refused!", getCurrentTxSender());
            refund();
            return;
        }

        long fee = getCurrentTxAmount() / 100;
        long donation = getCurrentTxAmount() - MINIMUM_DONATION_COSTS - fee;
        donationCount += 1;
        donated += donation;

        sendAmount(donation, owner);
        sendAmount(fee, inventor);
        if(getCurrentTx().getMessage() != null){
            sendMessage(getCurrentTx().getMessage(), owner);
        }
        sendAmount(MINIMUM_DONATION_COSTS, getCurrentTxSender());
        sendMessage("Thank you!", getCurrentTxSender());
    }

    private void refund(){
        sendAmount(getCurrentTxAmount(), getCurrentTxSender());
    }

    public static void main(String[] args) {
        BT.activateCIP20(true);
        new EmulatorWindow(DappositoryToken.class);
    }
}
