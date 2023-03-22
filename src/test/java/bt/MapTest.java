import java.io.BufferedWriter;
import java.io.FileWriter;

import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.Transaction;
import signumj.entity.response.TransactionAppendix;
import signumj.response.appendix.PlaintextMessageAppendix;
import signumj.service.NodeService;
import signumj.service.impl.HttpBurstNodeService;

/**
 * Check the deadlines of a series of blocks and computes alternative deadlines.
 * 
 * @author jjos
 *
 */
public class BittrexTxs{

	public static void main(String[] args) throws Exception {
		
		NodeService ns = new HttpBurstNodeService("http://localhost:8125", "signumj", 20);
		
		SignumAddress address = SignumAddress.fromEither("S-HK9D-P74Q-XDEJ-D6PGM");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("bittrex.csv"));

		int ntxs = 400;
		int start = 0;
		int end = ntxs;
		
		long total = 0;

		while(true) {
			Transaction[] txs = ns.getAccountTransactions(address, start, end, false).blockingGet();
			if(txs == null || txs.length == 0 || start > 2000)
				break;
			
			System.out.println(end);
			
			for(Transaction tx : txs) {
				TransactionAppendix append = null;
				if(tx.getAppendages() != null && tx.getAppendages().length > 0)
					append = tx.getAppendages()[0];
				
				String memo = "";
				
				if(append instanceof PlaintextMessageAppendix) {
					PlaintextMessageAppendix appendMessage = (PlaintextMessageAppendix) append;

					memo = appendMessage.getMessage();
					
					if(memo.equals("f324f4de3a8f4365ac248d2cbcc62e0a72d537e08f9c476093a48d826a3cec52")) {
					  System.out.println("https://chain.signum.network/tx/" + tx.getId().getID());
					  total += tx.getAmount().longValue();
					}
					else {
					  continue;
					}
				}

				writer.append(tx.getId().getID() + ",");
				writer.append(tx.getTimestamp().getAsDate().toString() + ",");
				if(tx.getSender().getSignedLongId() != address.getSignedLongId()) {
					writer.append(tx.getSender() + ",");
				}
				else {					
					writer.append(tx.getRecipient() + ",");
					writer.append("-");
				}
				writer.append(tx.getAmount().toUnformattedString() + ",");
				writer.append('"' + memo.replace('\n', ' ') + '"' + '\n');
			}
			
			start = end + 1;
			end += ntxs;
		}
		
		System.out.println("total: " + SignumValue.fromNQT(total).toString());
		
		writer.close();
		ns.close();
	}
}
