package bt.contracts;

import java.io.IOException;

import bt.BT;
import bt.compiler.Compiler;
import bt.sample.HashLoop;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;

/**
 * Stress test for smart contracts.
 * 
 * @author jjos
 */
public class StressTest extends BT {

    static final String PREFIX = "alottahash";
    static final int N = 1000;

    public static void main(String[] args) throws Exception {

        BT.setNodeAddress(BT.NODE_TESTNET);

        // run this one only once to get all instances registered
        // registerContracts(N);

        // only send amount later, since the contracts need to be already on chain
        sendAmount(SignumValue.fromSigna(2000000 / N));
    }

    static void registerContracts(int N) throws IOException {
        Compiler compiled = BT.compileContract(HashLoop.class);

        for (int i = 1; i <= N; i++) {
            BT.registerContract(BT.PASSPHRASE, compiled, PREFIX + i, PREFIX, SignumValue.fromSigna(10),
                    BT.getMinRegisteringFee(compiled), 1000).blockingGet();
            System.out.println(i);
        }
    }

    static void sendAmount(SignumValue amount) {
        AT[] ats = BT.getContracts(BT.getAddressFromPassphrase(BT.PASSPHRASE));

        int i = 1;
        for (AT at : ats) {
            if (at.getName().startsWith(PREFIX)) {
                if(i>940){
                    System.out.println(i);
                    BT.sendAmount(BT.PASSPHRASE, at.getId(), amount,
                        SignumValue.fromSigna(0.01 * i));
                }
                i++;
            }
        }
    }
}
