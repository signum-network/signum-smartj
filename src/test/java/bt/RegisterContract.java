package bt;

import burst.kit.entity.BurstValue;
import burst.kit.entity.response.ATResponse;

/**
 * Simple code to register a contract.
 * 
 * @author jjos
 */
public class RegisterContract {

    public static void main(String[] args) throws Exception {

        String NODE = BT.NODE_LOCAL_TESTNET;
//        String NODE = BT.NODE_AT_TESTNET;

        Class<? extends Contract> CONTRACT = Cast.class;
        BurstValue ACT_FEE = BurstValue.fromBurst(10);

        BT.setNodeAddress(NODE);

        ATResponse at = BT.registerContract(CONTRACT, ACT_FEE);

        System.out.println(at.getAt().getFullAddress());
    }
}
