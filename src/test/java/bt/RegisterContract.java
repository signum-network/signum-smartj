package bt;

import bt.sample.OddsGame;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.ATResponse;

/**
 * Simple code to register a contract.
 * 
 * @author jjos
 */
public class RegisterContract {

    public static void main(String[] args) throws Exception {

//        String NODE = TestUtil.NODE_LOCAL_TESTNET;
        String NODE = TestUtil.NODE_AT_TESTNET;

        Class<?> CONTRACT = OddsGame.class;
        BurstValue ACT_FEE = BurstValue.fromBurst(10);
        BurstValue FEE = BurstValue.fromBurst(0.1);

        TestUtil.setNode(NODE);

        ATResponse at = TestUtil.registerAT(TestUtil.PASSPHRASE, CONTRACT, ACT_FEE, FEE);

        System.out.println(at.getAt().getFullAddress());
    }
}
