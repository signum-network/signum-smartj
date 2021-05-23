package bt.sample;
 
import bt.Address;
import bt.BT;
import bt.Contract;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;
 
 
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class YieldRaw extends Contract {
    
    public static final long GAS_FEE = FEE_QUANT * 40;
    public static final long FARMINGPOWER = 100 * ONE_BURST;
    public static final long LORD_TAX = 6 * ONE_BURST;
    public static final long INITIAL_SEED =   1_000 * ONE_BURST;
    public static final long INITIAL_INVEST = 10_000 * ONE_BURST;
    public static final long LORD_TAX_PERCENTAGE = LORD_TAX * 90/100;

    long one  = 1;
    Address Patron;
    long seed = INITIAL_SEED;
    long PatronLevel;
    Address farmer1,farmer2,farmer3;
    long CurrentFarmer = one;
    long WinnerFarmer = one;
    long WinnerFarmerBalance = INITIAL_SEED;
    long looserFarmer;
    Address lord1,lord2,lord3,lord4;
    long lord1Invest,lord2Invest,lord3Invest,lord4Invest;
    long numberlords;
    long TaxPerLord;
    long two = 2;
    long three = 3;
    long four = 4;
    long marge =102;   
    long lordTax = LORD_TAX *90/100;
    long hash;
    public void txReceived(){
        if  (getCurrentTxAmount() + GAS_FEE >= seed){
            hash = getPrevBlockHash().getValue1();
            hash &= 0xffL; // bitwise AND to get the last part of the number (and avoid negative values)
            hash %= three; // MOD 3 to get just  0,1 or 2
            WinnerFarmer = hash +1;
            if (CurrentFarmer == one){
               farmer1= getCurrentTxSender();
               CurrentFarmer= two;
            }
            else if (CurrentFarmer == two){
                    farmer2= getCurrentTxSender();
                    CurrentFarmer= three;
                }
                else{
                    farmer3= getCurrentTxSender();
                    payout();
                    }
        }
        else{
            sendAmount(getCurrentTxAmount(), getCurrentTxSender());
        }
    }
    public void farmerPayout(){
        //Farmer1 or Farnmer 2 wants to be getting paid
        if (getCurrentTxSender() == farmer1 || getCurrentTxSender() == farmer2) {
            sendAmount(WinnerFarmerBalance, farmer1);
            farmer1 = null;
            if (farmer2 != null) {
                sendAmount(seed, farmer2);
                farmer2 = null;
            }
            CurrentFarmer = one;
            seed = INITIAL_SEED;
            WinnerFarmerBalance = INITIAL_SEED;
            //Payout dust to creator
            sendAmount((getCurrentBalance()-lord1Invest-lord2Invest-lord3Invest-lord4Invest),getCreator());
        }
        else{
            sendAmount(getCurrentTxAmount(), getCurrentTxSender());
        }
    }
    public void lordIntialInvest(){
        if (numberlords != four && getCurrentTxAmount() + GAS_FEE >= INITIAL_INVEST){
            lordBuyIn();
        }
        else{
            //We send the amount back
            sendAmount(getCurrentTxAmount(), getCurrentTxSender());
        }

    }
    private void lordBuyIn(){
        numberlords+= 1;
        if (lord1 == null){
            lord1 = getCurrentTxSender();
            lord1Invest = INITIAL_INVEST;
        }
        else if (lord2 == null){
            lord2 = getCurrentTxSender();
            lord2Invest = INITIAL_INVEST;
        }
        else if (lord3 == null){
            lord3 = getCurrentTxSender();
            lord3Invest = INITIAL_INVEST;
        }
        else if (lord4 == null){
            lord4 = getCurrentTxSender();
            lord4Invest = INITIAL_INVEST;
        }
    }
    public void lordRemoveInvestment(){
        if (numberlords == 0){
            return;
        }
        if (lord1 == getCurrentTxSender()){
            sendAmount(lord1Invest, lord1);
            lord1 = null;
            lord1Invest = 0;
            numberlords -= 1;
        }
        else if (lord2 == getCurrentTxSender()){
            sendAmount(lord2Invest, lord2);
            lord2 = null;
            lord2Invest = 0;
            numberlords -= 1;
        }
        else if (lord3 == getCurrentTxSender()){
            sendAmount(lord3Invest, lord3);
            lord3 = null;
            lord3Invest = 0;
            numberlords -= 1;
        }
        else if (lord4 == getCurrentTxSender()){
            sendAmount(lord4Invest, lord4);
            lord4 = null;
            lord4Invest = 0;
            numberlords -= 1;
        }
    }
    public void lordStaking(){
        boolean canStake = getCurrentTxAmount() + GAS_FEE >= INITIAL_INVEST;
        if (canStake && getCurrentTxSender() == lord1){
            lord1Invest += INITIAL_INVEST;
        }
        else if (canStake && getCurrentTxSender() == lord2){
            lord2Invest += INITIAL_INVEST;
        }
        else if (canStake && getCurrentTxSender() == lord3){
            lord3Invest += INITIAL_INVEST;
        }
        else if (canStake && getCurrentTxSender() == lord3){
            lord4Invest += INITIAL_INVEST;
        }
        else{
            //We send the amount back
            sendAmount(getCurrentTxAmount(), getCurrentTxSender());
        }
    }
    public void lordTakeOver(Address lordAddress){
        if(lordAddress == lord1 && canTakeOver(lord1Invest)){
            sendAmount(getCurrentTxAmount(), lord1);
            lord1 = getCurrentTxSender();
        }
        else if(lordAddress == lord2 && canTakeOver(lord2Invest)){
            sendAmount(getCurrentTxAmount(), lord2);
            lord2 = getCurrentTxSender();
        }
        else if(lordAddress == lord3 && canTakeOver(lord3Invest)){
            sendAmount(getCurrentTxAmount(), lord3);
            lord3 = getCurrentTxSender();
        }
        else if(lordAddress == lord4 && canTakeOver(lord4Invest)){
            sendAmount(getCurrentTxAmount(), lord4);
            lord4 = getCurrentTxSender();
        }
        else {
            sendAmount(getCurrentTxAmount(), getCurrentTxSender());
        }
    }

    private boolean canTakeOver(long invest){
        return getCurrentTxAmount() >= (invest*marge)/100;
    }
    private void payout(){
        looserFarmer = seed -FARMINGPOWER;
         if (WinnerFarmer == one){
            sendAmount(looserFarmer , farmer2);
            sendAmount(looserFarmer , farmer3);
            WinnerFarmerBalance += (FARMINGPOWER * two)  - LORD_TAX;
        }
        else{
            sendAmount(WinnerFarmerBalance -FARMINGPOWER , farmer1);
            WinnerFarmerBalance = seed + (FARMINGPOWER * two)  - LORD_TAX;
            if ( WinnerFarmer == two){
                sendAmount(looserFarmer , farmer3);
                farmer1 = farmer2;
            }
            else{
                sendAmount(looserFarmer , farmer2);
                farmer1 = farmer3;
            }
        }
        seed += FARMINGPOWER;
        farmer2= null;
        farmer3= null;
        CurrentFarmer= two;
        if (WinnerFarmerBalance > PatronLevel) {
            // WinningFarmer is farmer1 after the payout
            Patron = farmer1;
            PatronLevel = WinnerFarmerBalance;
        } 
        sendAmount(LORD_TAX/10, Patron);
        if (numberlords >= one) {
            long taxPerLord = lordTax/numberlords;
            if (lord1 != null){
                sendAmount(taxPerLord,lord1);
            };
            if (lord2 != null){
                sendAmount(taxPerLord,lord2);
            };
            if (lord3 != null){
                sendAmount(taxPerLord,lord3);
            };
            if (lord4 != null){
                sendAmount(taxPerLord,lord4);
            };
        }
        else{
            sendAmount(lordTax, getCreator());
            }
    }
    public static void main(String[] args) {
        BT.activateCIP20(true);
        new EmulatorWindow(YieldRaw.class);
    }

}