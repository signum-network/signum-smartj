package bt;

import signumj.entity.SignumValue;
import signumj.entity.response.AT;

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
        SignumValue ACT_FEE = SignumValue.fromSigna(10);

        BT.setNodeAddress(NODE);

        AT at = BT.registerContract(CONTRACT, ACT_FEE);

        System.out.println(at.getId().getFullAddress());
    }
}
