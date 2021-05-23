package bt.sample;
 
import bt.Address;
import bt.BT;
import bt.Contract;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;
 
 
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class YieldFarming extends Contract {

    // --- "constants"
    public static final long GAS_FEE = FEE_QUANT * 40;
    public static final long FARMINGPOWER = 100 * ONE_BURST;
    public static final long LORD_TAX = 6 * ONE_BURST;
    public static final long INITIAL_SEED =   1_000 * ONE_BURST;
    public static final long INITIAL_INVEST = 10_000 * ONE_BURST;
    public static final long LORD_TAX_PERCENTAGE = LORD_TAX * 90/100;
    public static final long ONE  = 1;
    public static final long TWO = 2;
    public static final long THREE = 3;
    public static final long FOUR = 4;
    public static final long MARGE =102;   

    // --- "variant values"
    long seed = INITIAL_SEED;
    Address farmer1,farmer2,farmer3;
    long currentFarmer = ONE;
    long winnerFarmer = ONE;
    long winnerFarmerBalance = INITIAL_SEED;
    Address patron;
    long patronBalance;    
    Address lord1,lord2,lord3,lord4;
    long lord1Invest,lord2Invest,lord3Invest,lord4Invest;
    long numberlords;

    // --- "locals"
    long hash;
    long taxPerLord;
    long looserFarmerBalance;
    boolean canStake;

    public void txReceived(){
        if(getCurrentTxAmount() + GAS_FEE < seed){
            refund();
            return;
        }
    
        hash = getPrevBlockHash().getValue1();
        hash &= 0xffL; // bitwise AND to get the last part of the number (and avoid negative values)
        hash %= THREE; // MOD 3 to get just  0,1 or 2
        winnerFarmer = hash +1;
        if (currentFarmer == ONE){
            farmer1= getCurrentTxSender();
            currentFarmer= TWO;
        }
        else if (currentFarmer == TWO){
            farmer2= getCurrentTxSender();
            currentFarmer= THREE;
        }
        else {
            farmer3= getCurrentTxSender();
            payout();
        }
    }

    public void farmerPayout(){
        //Farmer1 or Farmer 2 wants to be getting paid
        if (getCurrentTxSender() == farmer1 || getCurrentTxSender() == farmer2) {
            sendAmount(winnerFarmerBalance, farmer1);
            farmer1 = null;
            if (farmer2 != null) {
                sendAmount(seed, farmer2);
                farmer2 = null;
            }
            currentFarmer = ONE;
            seed = INITIAL_SEED;
            winnerFarmerBalance = INITIAL_SEED;
            //Payout dust to creator
            sendAmount((getCurrentBalance()-lord1Invest-lord2Invest-lord3Invest-lord4Invest),getCreator());
        }
        else{
            refund();
        }
    }

    public void lordIntialInvest(){
        if (numberlords != FOUR && getCurrentTxAmount() + GAS_FEE >= INITIAL_INVEST){
            // A new lord buys in
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
        else{
            refund();
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
        else { 
            refund();
        }
    }


    public void lordStaking(){
        canStake = getCurrentTxAmount() + GAS_FEE >= INITIAL_INVEST;
        if (canStake && getCurrentTxSender() == lord1){
            lord1Invest += INITIAL_INVEST;
        }
        else if (canStake && getCurrentTxSender() == lord2){
            lord2Invest += INITIAL_INVEST;
        }
        else if (canStake && getCurrentTxSender() == lord3){
            lord3Invest += INITIAL_INVEST;
        }
        else if (canStake && getCurrentTxSender() == lord4){
            lord4Invest += INITIAL_INVEST;
        }
        else{
            refund();
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
           refund();
        }
    }

    private boolean canTakeOver(long invest){
        return getCurrentTxAmount() >= (invest*MARGE)/100;
    }

    private void payout(){
        looserFarmerBalance = seed - FARMINGPOWER;
         if (winnerFarmer == ONE){
            sendAmount(looserFarmerBalance , farmer2);
            sendAmount(looserFarmerBalance , farmer3);
            winnerFarmerBalance += (FARMINGPOWER * TWO)  - LORD_TAX;
        }
        else{
            sendAmount(winnerFarmerBalance -FARMINGPOWER , farmer1);
            winnerFarmerBalance = seed + (FARMINGPOWER * TWO)  - LORD_TAX;
            if ( winnerFarmer == TWO){
                sendAmount(looserFarmerBalance , farmer3);
                farmer1 = farmer2;
            }
            else{
                sendAmount(looserFarmerBalance , farmer2);
                farmer1 = farmer3;
            }
        }
        seed += FARMINGPOWER;
        farmer2= null;
        farmer3= null;
        currentFarmer= TWO;
        if (winnerFarmerBalance > patronBalance) {
            patron = farmer1;
            patronBalance = winnerFarmerBalance;
        } 
        sendAmount(LORD_TAX/10, patron);
        if (numberlords >= ONE) {
            taxPerLord = LORD_TAX_PERCENTAGE/numberlords;
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
            sendAmount(LORD_TAX_PERCENTAGE, getCreator());
            }
    }

    private void refund(){
        sendAmount(getCurrentTxAmount(), getCurrentTxSender());
    }

    public static void main(String[] args) {
        BT.activateCIP20(true);
        new EmulatorWindow(YieldFarming.class);
    }

}