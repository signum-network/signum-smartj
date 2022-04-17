package bt;

import org.junit.Test;

import bt.compiler.Compiler;
import bt.compiler.Method;
import bt.contracts.CalcMultDiv96;
import bt.contracts.CalcSqrt;
import bt.contracts.Cast;
import bt.contracts.CheckSignature;
import bt.contracts.CodeHashId;
import bt.contracts.EqualsCreator;
import bt.contracts.GenSig;
import bt.contracts.LocalVar;
import bt.contracts.MessagePage;
import bt.contracts.MethodCall;
import bt.contracts.MethodCallArgs;
import bt.contracts.Send2Messages;
import bt.contracts.TestHigherThanOne;
import bt.sample.Auction;
import bt.sample.AuctionNFT;
import bt.sample.Forward;
import bt.sample.ForwardMin;
import bt.sample.HashedTimeLock;
import bt.sample.MultiSigLock;
import bt.sample.OddsGame;
import bt.sample.Sha256_64;
import bt.sample.TXCounter;
import bt.sample.TXCounter2;
import bt.sample.TipThanks;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.entity.response.Account;
import signumj.entity.response.Transaction;
import signumj.entity.response.TransactionBroadcast;
import signumj.response.appendix.PlaintextMessageAppendix;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 *
 * @author jjos
 */
public class GeneralTests extends BT {
	static {
		BT.activateSIP37(true);
	}

    @Test
    public void testOdds() throws Exception {

        // Send some burst for the players
        forgeBlock(PASSPHRASE, 500);
        SignumAddress player1 = bc.getAddressFromPassphrase(PASSPHRASE2);
        SignumAddress player2 = bc.getAddressFromPassphrase(PASSPHRASE3);
        sendAmount(PASSPHRASE, player1, SignumValue.fromSigna(1000));
        sendAmount(PASSPHRASE, player2, SignumValue.fromSigna(1000));
        forgeBlock(PASSPHRASE, 500);

        SignumValue actvFee = SignumValue.fromSigna(10);
        SignumValue amount = SignumValue.fromSigna(100);

        AT at = registerContract(OddsGame.class, actvFee);
        assertNotNull("AT could not be registered", at);

        // Fill the contract with 3 times the max payment value
        sendAmount(PASSPHRASE, at.getId(), SignumValue.fromNQT(OddsGame.MAX_PAYMENT * 3));
        forgeBlock();

        // 1 bet each
        sendAmount(PASSPHRASE, at.getId(), amount);
        sendAmount(PASSPHRASE2, at.getId(), amount);
        sendAmount(PASSPHRASE3, at.getId(), amount);
        forgeBlock();
        forgeBlock();

        // send some just to run the code
        sendAmount(PASSPHRASE2, at.getId(), amount);
        forgeBlock();
        forgeBlock();
    }

    @Test
    public void testForward() throws Exception {
        SignumAddress address = SignumAddress.fromRs(Forward.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, SignumValue.fromNQT(1));
        forgeBlock();

        Account bmfAccount = bns.getAccount(address).blockingGet();
        SignumValue balance = bmfAccount.getUnconfirmedBalance();
        SignumValue actvFee = SignumValue.fromSigna(1);
        SignumValue amount = SignumValue.fromSigna(10);

        AT at = registerContract(Forward.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getId(), amount);
        forgeBlock();
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        SignumValue newBalance = bmfAccount.getUnconfirmedBalance();
        double result = newBalance.doubleValue() - balance.doubleValue() - actvFee.doubleValue();

        assertTrue("Value not forwarded", result > actvFee.doubleValue());
    }

    @Test
    public void testForwardMin() throws Exception {
        SignumAddress address = SignumAddress.fromEither(ForwardMin.ADDRESS);

        // send some burst to make sure the account exist
        TransactionBroadcast tb = sendAmount(PASSPHRASE, address, SignumValue.fromNQT(1));
        forgeBlock(tb);

        Account bmfAccount = bns.getAccount(address).blockingGet();
        SignumValue balance = bmfAccount.getUnconfirmedBalance();
        SignumValue actvFee = SignumValue.fromSigna(1);
        double amount = ForwardMin.MIN_AMOUNT * 0.8;

        AT at = registerContract(ForwardMin.class, actvFee);
        assertNotNull("AT could not be registered", at);

        tb = sendAmount(PASSPHRASE, at.getId(), SignumValue.fromNQT((long) amount));
        forgeBlock(tb);

        bmfAccount = bns.getAccount(address).blockingGet();
        SignumValue newBalance = bmfAccount.getUnconfirmedBalance();
        double result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value forwarded while it should not", result < amount);

        tb = sendAmount(PASSPHRASE, at.getId(), SignumValue.fromNQT((long) amount));
        forgeBlock(tb);
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        newBalance = bmfAccount.getUnconfirmedBalance();
        result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value not forwarded as it should", result * Contract.ONE_BURST > amount);
    }

    @Test
    public void testTipThanks() throws Exception {
        SignumAddress address = SignumAddress.fromEither(TipThanks.ADDRESS);

        // send some signa to make sure the account exists
        sendAmount(PASSPHRASE, address, SignumValue.fromSigna(100));
        forgeBlock();

        Account benefAccout = bns.getAccount(address).blockingGet();
        SignumValue balance = benefAccout.getUnconfirmedBalance();
        SignumValue actvFee = SignumValue.fromSigna(1);
        long amount = (long)(TipThanks.MIN_AMOUNT * 0.8);

        AT at = registerContract(TipThanks.class, actvFee);
        assertNotNull("AT could not be registered", at);
        System.out.println(at.getId().getID());

        TransactionBroadcast tb = sendAmount(PASSPHRASE, at.getId(), SignumValue.fromNQT((long) amount));
        forgeBlock(tb);
        forgeBlock();
        forgeBlock();

        benefAccout = bns.getAccount(address).blockingGet();
        SignumValue newBalance = benefAccout.getUnconfirmedBalance();
        long result = newBalance.longValue() - balance.longValue();
        assertTrue("Value forwarded while it should not", result == 0);

        tb = sendAmount(PASSPHRASE, at.getId(), SignumValue.fromNQT(amount));
        forgeBlock(tb);
        forgeBlock();
        forgeBlock();

        benefAccout = bns.getAccount(address).blockingGet();
        newBalance = benefAccout.getUnconfirmedBalance();
        result = newBalance.longValue() - balance.longValue();
        assertTrue("Value not forwarded as it should", result > amount);
    }

    @Test
    public void testLocalVar() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(LocalVar.class);

        String name = LocalVar.class.getSimpleName() + System.currentTimeMillis();
        SignumAddress creator = SignumCrypto.getInstance().getAddressFromPassphrase(BT.PASSPHRASE);

        TransactionBroadcast tb = BT.registerContract(BT.PASSPHRASE, comp, name, name,
        		SignumValue.fromNQT(LocalVar.FEE),
                SignumValue.fromSigna(1), 1000).blockingGet();
        BT.forgeBlock(tb);

        AT contract = BT.findContract(creator, name);

        SignumValue valueSent = SignumValue.fromSigna(10);
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), valueSent);
        BT.forgeBlock();
        BT.forgeBlock();

        assertEquals(valueSent.longValue(),
                BT.getContractFieldValue(contract, comp.getField("amountNoFee").getAddress()));

        long value = 512;
        tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("setValue"), SignumValue.fromSigna(1),
                SignumValue.fromSigna(0.1), 1000, value);
        BT.forgeBlock(tb);
        BT.forgeBlock();

        long valueChain = BT.getContractFieldValue(contract, comp.getField("valueTimes2").getAddress());

        assertEquals(value * 2, valueChain);
    }

    @Test
    public void testMethodCall() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(MethodCall.class);

        String name = MethodCall.class.getSimpleName() + System.currentTimeMillis();

        AT contract = BT.registerContract(comp, name, SignumValue.fromSigna(10));

        // variable not initialized yet
        assertEquals(0, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));

        TransactionBroadcast tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method1"), SignumValue.fromSigna(10),
                SignumValue.fromSigna(0.1), 1000);
        BT.forgeBlock(tb);
        BT.forgeBlock();
        assertEquals(1, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));

        tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method2"), SignumValue.fromSigna(10),
                SignumValue.fromSigna(0.1), 1000);
        BT.forgeBlock(tb);
        BT.forgeBlock();
        assertEquals(2, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));
    }

    @Test
    public void testMethodCallArgs() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(MethodCallArgs.class);

        String name = MethodCallArgs.class.getSimpleName() + System.currentTimeMillis();

        AT contract = BT.registerContract(comp, name, SignumValue.fromSigna(30));
        System.out.println(contract.getId().getID());

        // variable not initialized yet
        assertEquals(0, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method1"), SignumValue.fromSigna(30),
                SignumValue.fromSigna(0.1), 1000, 100);
        BT.forgeBlock();
        BT.forgeBlock();
        assertEquals(1, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));
        assertEquals(100, BT.getContractFieldValue(contract, comp.getFieldAddress("arg1")));
        assertEquals(-1, BT.getContractFieldValue(contract, comp.getFieldAddress("arg2")));
        assertEquals(-1, BT.getContractFieldValue(contract, comp.getFieldAddress("arg3")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method2"), SignumValue.fromSigna(30),
                SignumValue.fromSigna(0.1), 1000, 100, 200);
        BT.forgeBlock();
        BT.forgeBlock();
        assertEquals(2, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));
        assertEquals(100, BT.getContractFieldValue(contract, comp.getFieldAddress("arg1")));
        assertEquals(200, BT.getContractFieldValue(contract, comp.getFieldAddress("arg2")));
        assertEquals(-1, BT.getContractFieldValue(contract, comp.getFieldAddress("arg3")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method3"), SignumValue.fromSigna(30),
                SignumValue.fromSigna(0.1), 1000, 100, 200, 300);
        BT.forgeBlock();
        BT.forgeBlock();
        assertEquals(3, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));
        assertEquals(100, BT.getContractFieldValue(contract, comp.getFieldAddress("arg1")));
        assertEquals(200, BT.getContractFieldValue(contract, comp.getFieldAddress("arg2")));
        assertEquals(300, BT.getContractFieldValue(contract, comp.getFieldAddress("arg3")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method4"), SignumValue.fromSigna(30),
                SignumValue.fromSigna(0.1), 1000, 1000, 2000, 3000);
        BT.forgeBlock();
        BT.forgeBlock();
        assertEquals(4, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));
        assertEquals(1000, BT.getContractFieldValue(contract, comp.getFieldAddress("arg1")));
        assertEquals(2000, BT.getContractFieldValue(contract, comp.getFieldAddress("arg2")));
        assertEquals(3000, BT.getContractFieldValue(contract, comp.getFieldAddress("arg3")));
    }

    public void testCounter() throws Exception {
        BT.forgeBlock();

        long ntx, nblocks, address;

        Compiler compiled = BT.compileContract(TXCounter.class);
        AT contract = BT.registerContract(compiled, TXCounter.class.getSimpleName() + System.currentTimeMillis(),
                SignumValue.fromSigna(10));
        System.out.println(contract.getId().getSignedLongId());

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(20));
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());
        assertEquals(1, ntx);
        assertEquals(1, nblocks);
        assertEquals(BT.getAddressFromPassphrase(PASSPHRASE).getSignedLongId(), address);

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(20), SignumValue.fromSigna(0.1))
                ;
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromSigna(20), SignumValue.fromSigna(1))
                ;
        BT.sendAmount(BT.PASSPHRASE3, contract.getId(), SignumValue.fromSigna(20), SignumValue.fromSigna(1))
                ;
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());

        assertEquals(4, ntx);
        assertEquals(2, nblocks);
    }

    public void testCounter2() throws Exception {
    	// make sure all accounts exist
        BT.forgeBlock();
        BT.forgeBlock(BT.PASSPHRASE2);
        BT.forgeBlock(BT.PASSPHRASE3);

        long ntx, ncalls, nblocks, address;

        Compiler compiled = BT.compileContract(TXCounter2.class);
        AT contract = BT.registerContract(compiled, TXCounter2.class.getSimpleName() + System.currentTimeMillis(),
                SignumValue.fromSigna(10));
        System.out.println(contract.getId().getID());

        TransactionBroadcast tb = BT.callMethod(BT.PASSPHRASE, contract.getId(), compiled.getMethod("methodCall"), SignumValue.fromSigna(20),
        		SignumValue.fromSigna(1), 1000);
        BT.forgeBlock(tb);
        BT.forgeBlock();
        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        ncalls = BT.getContractFieldValue(contract, compiled.getField("ncalls").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        assertEquals(1, ntx);
        assertEquals(1, ncalls);
        assertEquals(1, nblocks);

        tb = BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(20));
        BT.forgeBlock(tb);
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());
        assertEquals(2, ntx);
        assertEquals(BT.getAddressFromPassphrase(PASSPHRASE).getSignedLongId(), address);

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(20), SignumValue.fromSigna(0.01));
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromSigna(20), SignumValue.fromSigna(0.1));
        tb = BT.sendAmount(BT.PASSPHRASE3, contract.getId(), SignumValue.fromSigna(20), SignumValue.fromSigna(1));
        BT.forgeBlock(tb);
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());

        assertEquals(5, ntx);
    }

    public void testSha256_64() throws Exception {
        BT.forgeBlock();

        long sha_chain, sha;

        Compiler compiled = BT.compileContract(Sha256_64.class);
        AT contract = BT.registerContract(compiled, "sha" + System.currentTimeMillis(), SignumValue.fromSigna(10));
        System.out.println(contract.getId().getSignedLongId());

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(20));
        BT.forgeBlock();
        BT.forgeBlock();

        Register input = new Register();
        input.value[0] = 1;
        input.value[1] = 2;
        sha = Contract.performSHA256_(input).getValue1();

        sha_chain = BT.getContractFieldValue(contract, compiled.getField("sha256_64").getAddress());
        assertEquals(sha_chain, sha);
    }

    public void testAuction() throws Exception {
        BT.forgeBlock();

        Compiler compiled = BT.compileContract(Auction.class);
        AT contract = BT.registerContract(compiled, Auction.class.getSimpleName() + System.currentTimeMillis(),
                SignumValue.fromSigna(Auction.ACTIVATION_FEE));
        System.out.println(contract.getId().getID());

        // lets just initialized the contract
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(30));
        BT.forgeBlock();
        BT.forgeBlock();

        long benef_chain = BT.getContractFieldValue(contract, compiled.getField("beneficiary").getAddress());
        assertEquals(benef_chain, SignumAddress.fromEither(Auction.BENEFICIARY).getSignedLongId());

        long isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());
        assertEquals(1, isOpen);

        long bidder = BT.getContractFieldValue(contract, compiled.getField("highestBidder").getAddress());
        assertEquals(0, bidder);

        // send an acution smaller than the min bid
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromNQT(Auction.INITIAL_PRICE / 2));
        BT.forgeBlock();
        BT.forgeBlock();

        SignumValue balance = BT.getContractBalance(contract);
        // bid should be refused
        assertTrue(balance.longValue() < 30 * Contract.ONE_BURST);

        // send bid with enough funds
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(Auction.INITIAL_PRICE * 2));
        BT.forgeBlock();
        BT.forgeBlock();

        isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());
        assertEquals(1, isOpen);

        // bid should accepted
        bidder = BT.getContractFieldValue(contract, compiled.getField("highestBidder").getAddress());
        assertEquals(BT.getAddressFromPassphrase(BT.PASSPHRASE2).getSignedLongId(), bidder);

        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() > Auction.INITIAL_PRICE - 30 * Contract.ONE_BURST);

        // send another bit with higher value
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromNQT(Auction.INITIAL_PRICE * 3));
        BT.forgeBlock();
        BT.forgeBlock();
        // bid should be accepted
        bidder = BT.getContractFieldValue(contract, compiled.getField("highestBidder").getAddress());
        assertEquals(BT.getAddressFromPassphrase(BT.PASSPHRASE).getSignedLongId(), bidder);

        // wait the contract to time out
        for (int i = 0; i < Auction.TIMEOUT_MIN / 4; i++) {
            BT.forgeBlock();
        }
        // make the contract to run to see the payment
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(30));
        BT.forgeBlock();
        BT.forgeBlock();

        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() < 30 * Contract.ONE_BURST);
    }

    public void testAuctionNFT() throws Exception {
        BT.forgeBlock();

        Compiler compiled = BT.compileContract(AuctionNFT.class);
        AT contract = BT.registerContract(compiled, AuctionNFT.class.getSimpleName() + System.currentTimeMillis(),
                SignumValue.fromNQT(AuctionNFT.ACTIVATION_FEE));
        System.out.println(contract.getId().getID());

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(30));
        BT.forgeBlock();
        BT.forgeBlock();

        long ben_chain = BT.getContractFieldValue(contract, compiled.getField("beneficiary").getAddress());
        assertEquals(ben_chain, contract.getCreator().getSignumID().getSignedLongId());

        long isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());

        assertTrue("should not be open", isOpen == 0);

        long minbid = 1000 * Contract.ONE_BURST;
        long timeout_mins = 40;

        // open the contract for auction
        BT.callMethod(BT.PASSPHRASE, contract.getId(), compiled.getMethod("open"),
                SignumValue.fromNQT(AuctionNFT.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000, timeout_mins, minbid,
                contract.getCreator().getSignedLongId() // beneficiary
        );
        BT.forgeBlock();
        BT.forgeBlock();

        isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());
        long minbid_chain = BT.getContractFieldValue(contract, compiled.getField("highestBid").getAddress());
        long benef_chain = BT.getContractFieldValue(contract, compiled.getField("beneficiary").getAddress());

        assertTrue("should now be open", isOpen == 1);
        assertEquals(minbid, minbid_chain);
        assertEquals(contract.getCreator().getSignedLongId(), benef_chain);

        // send an acution smaller than the min bid
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromNQT(minbid / 2));
        BT.forgeBlock();
        BT.forgeBlock();

        SignumValue balance = BT.getContractBalance(contract);
        // bid should be refused
        assertTrue(balance.longValue() < 60 * Contract.ONE_BURST);

        // send bid with enough funds
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(minbid * 2));
        BT.forgeBlock();
        BT.forgeBlock();

        isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());
        assertEquals(1, isOpen);

        // bid should accepted
        long bidder = BT.getContractFieldValue(contract, compiled.getFieldAddress("highestBidder"));
        assertEquals(BT.getAddressFromPassphrase(BT.PASSPHRASE2).getSignedLongId(), bidder);

        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() > minbid);

        // send another bit with higher value
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromNQT(minbid * 3));
        BT.forgeBlock();
        BT.forgeBlock();
        // bid should be accepted
        bidder = BT.getContractFieldValue(contract, compiled.getField("highestBidder").getAddress());
        assertEquals(BT.getAddressFromPassphrase(BT.PASSPHRASE).getSignedLongId(), bidder);

        // wait the contract to time out
        for (int i = 0; i < timeout_mins/ 4; i++) {
            BT.forgeBlock();
        }
        // make the contract to run to see the payment
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(30));
        BT.forgeBlock();
        BT.forgeBlock();

        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() < 30 * Contract.ONE_BURST);
    }


    public void testMultiSigLock() throws Exception {
        BT.forgeBlock();

        Compiler compiled = BT.compileContract(MultiSigLock.class);
        AT contract = BT.registerContract(compiled, MultiSigLock.class.getSimpleName() + System.currentTimeMillis(),
                SignumValue.fromNQT(MultiSigLock.ACTIVATION_FEE));
        System.out.println(contract.getId().getID());

        long initialBalance = 1000*Contract.ONE_BURST;

        // initialize the contract and put some balance
        BT.sendAmount(BT.PASSPHRASE, contract.getId(),
                SignumValue.fromNQT(initialBalance + MultiSigLock.ACTIVATION_FEE));
        BT.forgeBlock();
        BT.forgeBlock();

        SignumAddress owner1 = BT.getAddressFromPassphrase(BT.PASSPHRASE);
        SignumAddress owner2 = BT.getAddressFromPassphrase(BT.PASSPHRASE2);
        SignumAddress owner3 = BT.getAddressFromPassphrase(BT.PASSPHRASE3);

        // check for the owners
        long owner1Chain =  BT.getContractFieldValue(contract, compiled.getField("owner1").getAddress());
        long owner2Chain =  BT.getContractFieldValue(contract, compiled.getField("owner2").getAddress());
        long owner3Chain =  BT.getContractFieldValue(contract, compiled.getField("owner3").getAddress());
        long nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());

        assertEquals(owner1.getSignedLongId(), owner1Chain);
        assertEquals(owner2.getSignedLongId(), owner2Chain);
        assertEquals(owner3.getSignedLongId(), owner3Chain);
        assertEquals(0, nSignatures);

        long amount = initialBalance/2;
        long beneficiary = owner1.getSignedLongId();

        // sign a transaction
        BT.callMethod(BT.PASSPHRASE, contract.getId(), compiled.getMethod("sign"),
                SignumValue.fromNQT(MultiSigLock.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000,
                beneficiary, amount
        );
        BT.forgeBlock();
        BT.forgeBlock();

        nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());
        long receiver1 = BT.getContractFieldValue(contract, compiled.getField("receiver1").getAddress());
        assertEquals(1, nSignatures);
        assertEquals(beneficiary, receiver1);

        // sign again with the same wallet, should keep a single signature valid
        BT.callMethod(BT.PASSPHRASE, contract.getId(), compiled.getMethod("sign"),
                SignumValue.fromNQT(MultiSigLock.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000,
                beneficiary, amount
        );
        BT.forgeBlock();
        BT.forgeBlock();
        nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());
        assertEquals(1, nSignatures);

        // add a second sign
        BT.callMethod(BT.PASSPHRASE2, contract.getId(), compiled.getMethod("sign"),
                SignumValue.fromNQT(MultiSigLock.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000,
                beneficiary, amount
        );
        BT.forgeBlock();
        BT.forgeBlock();
        nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());
        long receiver2 = BT.getContractFieldValue(contract, compiled.getField("receiver2").getAddress());
        assertEquals(2, nSignatures);
        assertEquals(beneficiary, receiver2);

        // add a third sign, but incompatible
        BT.callMethod(BT.PASSPHRASE3, contract.getId(), compiled.getMethod("sign"),
                SignumValue.fromNQT(MultiSigLock.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000,
                beneficiary, amount*2
        );
        BT.forgeBlock();
        BT.forgeBlock();
        nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());
        assertEquals(1, nSignatures);

        // add a third sign to complete the transfer
        BT.callMethod(BT.PASSPHRASE3, contract.getId(), compiled.getMethod("sign"),
                SignumValue.fromNQT(MultiSigLock.ACTIVATION_FEE), SignumValue.fromSigna(0.1), 1000,
                beneficiary, amount
        );
        BT.forgeBlock();
        BT.forgeBlock();
        nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());
        assertEquals(0, nSignatures);

        long balance = BT.getContractBalance(contract).longValue();
        assertTrue(balance < initialBalance);
    }

    public void testHashTimelockRefund() throws Exception {
        BT.forgeBlock();

        Compiler compiled = BT.compileContract(HashedTimeLock.class);

        SignumAddress beneficiary = BT.getAddressFromPassphrase(BT.PASSPHRASE2);
        Register key = Register.newInstance(1, 2, 3, 4);
        Register hahsedkey = Contract.performSHA256_(key);

        long []data = {
                hahsedkey.getValue1(), hahsedkey.getValue2(), hahsedkey.getValue3(), hahsedkey.getValue4(),
                beneficiary.getSignedLongId()
        };

        String name = HashedTimeLock.class.getSimpleName() + System.currentTimeMillis();
        BT.registerContract(BT.PASSPHRASE, compiled.getCode(), compiled.getDataPages(),
                name, "test", data,
                SignumValue.fromNQT(HashedTimeLock.ACTIVATION_FEE), BT.getMinRegisteringFee(compiled),
                1000);
        BT.forgeBlock(BT.PASSPHRASE2);
        BT.forgeBlock(BT.PASSPHRASE2);

        AT contract = BT.findContract(BT.getAddressFromPassphrase(BT.PASSPHRASE), name);
        System.out.println(contract.getId().getID());

        // Initialize the contract with a given amount, this also initializes the timeout
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(1000));
        BT.forgeBlock();
        BT.forgeBlock();

        // Get the funds back after the timeout
        BT.forgeBlock();
        BT.forgeBlock();
        BT.forgeBlock();
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromNQT(HashedTimeLock.ACTIVATION_FEE));
        BT.forgeBlock();
        BT.forgeBlock();

        long balance = BT.getContractBalance(contract).longValue();
        assertEquals(0, balance);
    }

    public void testHashTimelockPay() throws Exception {
        BT.forgeBlock();

        Compiler compiled = BT.compileContract(HashedTimeLock.class);

        SignumAddress beneficiary = BT.getAddressFromPassphrase(BT.PASSPHRASE2);
        Register key = Register.newInstance(1, 2, 3, 4);
        Register hahsedkey = Contract.performSHA256_(key);

        long []data = {
                hahsedkey.getValue1(), hahsedkey.getValue2(), hahsedkey.getValue3(), hahsedkey.getValue4(),
                beneficiary.getSignedLongId()
        };

        String name = HashedTimeLock.class.getSimpleName() + System.currentTimeMillis();
        BT.registerContract(BT.PASSPHRASE, compiled.getCode(), compiled.getDataPages(),
                name, "test", data,
                SignumValue.fromNQT(HashedTimeLock.ACTIVATION_FEE), BT.getMinRegisteringFee(compiled),
                1000);
        BT.forgeBlock(BT.PASSPHRASE2);
        BT.forgeBlock(BT.PASSPHRASE2);

        AT contract = BT.findContract(BT.getAddressFromPassphrase(BT.PASSPHRASE), name);
        System.out.println(contract.getId().getID());

        // Initialize the contract with a given amount, this also initializes the timeout
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), SignumValue.fromSigna(1000));
        BT.forgeBlock();
        BT.forgeBlock();

        // Request the payment without the key, should not pay
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(HashedTimeLock.ACTIVATION_FEE));
        BT.forgeBlock();
        BT.forgeBlock();

        long balance = BT.getContractBalance(contract).longValue();
        assertTrue(balance>0);

        // Request the payment with the correct key, should pay
        ByteBuffer b = ByteBuffer.allocate(32);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putLong(key.getValue1());
        b.putLong(key.getValue2());
        b.putLong(key.getValue3());
        b.putLong(key.getValue4());

        BT.sendMessage(BT.PASSPHRASE2, contract.getId(), SignumValue.fromNQT(HashedTimeLock.ACTIVATION_FEE),
                SignumValue.fromSigna(0.1), 1000, b.array());
        BT.forgeBlock();
        BT.forgeBlock();

        long hash1 = BT.getContractFieldValue(contract, compiled.getFieldAddress("hashedKey"));
        long hash2 = BT.getContractFieldValue(contract, compiled.getFieldAddress("hashedKey") + 1);
        long hash3 = BT.getContractFieldValue(contract, compiled.getFieldAddress("hashedKey") + 2);
        long hash4 = BT.getContractFieldValue(contract, compiled.getFieldAddress("hashedKey") + 3);

        assertEquals(hahsedkey.getValue1(), hash1);
        assertEquals(hahsedkey.getValue2(), hash2);
        assertEquals(hahsedkey.getValue3(), hash3);
        assertEquals(hahsedkey.getValue4(), hash4);

        balance = BT.getContractBalance(contract).longValue();
        assertEquals(0, balance);
    }
    
    @Test
    public void testCast() throws Exception{
    	forgeBlock();
    	AT castContract = registerContract(Cast.class, SignumValue.fromSigna(0.3));
    	
    	TransactionBroadcast tb = sendAmount(BT.PASSPHRASE, castContract.getId(), castContract.getMinimumActivation());
    	forgeBlock(tb);
    	forgeBlock();
    	
    	long worked = BT.getContractFieldValue(castContract, 2);
    	assertEquals(1, worked);
    }
    
    @Test
    public void testEquals() throws Exception{
    	forgeBlock();
    	AT equalsContract = registerContract(EqualsCreator.class, SignumValue.fromSigna(0.3));
    	
    	TransactionBroadcast tb = sendAmount(BT.PASSPHRASE, equalsContract.getId(), equalsContract.getMinimumActivation());
    	forgeBlock(tb);
    	forgeBlock();
    	
    	long worked = BT.getContractFieldValue(equalsContract, 0);
    	assertEquals(1, worked);
    }
    
    @Test
    public void testHigherThanOne() throws Exception{
    	forgeBlock();
    	AT testContract = registerContract(TestHigherThanOne.class, SignumValue.fromSigna(0.3));
    	
    	TransactionBroadcast tb = sendAmount(BT.PASSPHRASE, testContract.getId(), testContract.getMinimumActivation());
    	forgeBlock(tb);
    	forgeBlock();
    	
    	long worked = BT.getContractFieldValue(testContract, 0);
    	assertEquals(0, worked);

    	SignumValue amount = SignumValue.fromSigna(2);
    	tb = sendAmount(BT.PASSPHRASE, testContract.getId(), amount);
    	forgeBlock(tb);
    	forgeBlock();
    	worked = BT.getContractFieldValue(testContract, 0);
    	assertEquals(amount.subtract(testContract.getMinimumActivation()).longValue(), worked);
    }
    
    @Test
    public void testSqrt() throws Exception{
    	forgeBlock();
    	AT testContract = registerContract(CalcSqrt.class, SignumValue.fromSigna(0.4));
    	
    	TransactionBroadcast tb = sendAmount(BT.PASSPHRASE, testContract.getId(), testContract.getMinimumActivation());
    	forgeBlock(tb);
    	forgeBlock();
    	
    	long result = BT.getContractFieldValue(testContract, 0);
    	assertEquals(0, result);

    	SignumValue amount = SignumValue.fromSigna(2);
    	tb = sendAmount(BT.PASSPHRASE, testContract.getId(), amount);
    	forgeBlock(tb);
    	forgeBlock();
    	forgeBlock();
    	
    	result = BT.getContractFieldValue(testContract, 0);
    	assertEquals((long)Math.sqrt(amount.subtract(testContract.getMinimumActivation()).longValue()), result);
    }
    
    @Test
    public void testCalcMultDiv() throws Exception{
    	forgeBlock();
    	AT testContract = registerContract(CalcMultDiv96.class, SignumValue.fromSigna(0.4));
    	
    	SignumValue amount = SignumValue.fromSigna(2);
    	TransactionBroadcast tb = sendAmount(BT.PASSPHRASE, testContract.getId(), amount);
    	forgeBlock(tb);
    	forgeBlock();
    	forgeBlock();
    	
    	long result = BT.getContractFieldValue(testContract, 0);
    	assertEquals((long)(amount.subtract(testContract.getMinimumActivation()).longValue() * 0.96), result);
    }
    
    
    @Test
    public void test2Messages() throws Exception{
    	forgeBlock();
    	AT testContract = registerContract(Send2Messages.class, SignumValue.fromSigna(0.4));
    	
    	SignumValue amount = SignumValue.fromSigna(2);
    	TransactionBroadcast tb = sendAmount(BT.PASSPHRASE, testContract.getId(), amount);
    	forgeBlock(tb);
    	forgeBlock();
    	forgeBlock();

    	boolean found = false;
    	Transaction[] txs = BT.getNode().getAccountTransactions(testContract.getId(), 0, 100, false).blockingGet();
    	for(Transaction tx : txs) {
    		if(tx.getSender().equals(testContract.getId())) {
    			if(tx.getAppendages().length == 1 && tx.getAppendages()[0] instanceof PlaintextMessageAppendix) {
    				PlaintextMessageAppendix msg = (PlaintextMessageAppendix) tx.getAppendages()[0];
    				assertTrue(msg.isText() == false);
    				// should have the 2 messages concatenated
    				assertEquals(8*4*2 *2, msg.getMessage().length());
    				found = true;
    				break;
    			}
    		}
    	}
    	assertTrue(found);
    }
    
    @Test
    public void testGenSig() throws Exception{
    	forgeBlock();
    	AT testContract = registerContract(GenSig.class, SignumValue.fromSigna(0.4));
    	System.out.println(testContract.getId().getID());
    	
    	SignumValue amount = SignumValue.fromSigna(2);
    	TransactionBroadcast tb = sendAmount(BT.PASSPHRASE, testContract.getId(), amount);
    	forgeBlock(tb);
    	forgeBlock();
    	forgeBlock();

    	long genSig = BT.getContractFieldValue(testContract, 0);
    	long genSigReg1 = BT.getContractFieldValue(testContract, 1);
    	long genSigReg2 = BT.getContractFieldValue(testContract, 2);
    	long genSigReg3 = BT.getContractFieldValue(testContract, 3);
    	long genSigReg4 = BT.getContractFieldValue(testContract, 4);
    	assertTrue(genSig != 0L);
    	assertTrue(genSigReg1 != 0L);
    	assertTrue(genSigReg2 != 0L);
    	assertTrue(genSigReg3 != 0L);
    	assertTrue(genSigReg4 != 0L);
    	assertEquals(genSig, genSigReg1);
    }
    
    @Test
    public void testMassagePage() throws Exception{
    	forgeBlock();
    	AT testContract = registerContract(MessagePage.class, SignumValue.fromSigna(0.4));
    	System.out.println(testContract.getId().getID());
    	
    	ByteBuffer msg = ByteBuffer.allocate(4*8*2);
    	msg.order(ByteOrder.LITTLE_ENDIAN);
    	
    	Random r = new Random();
    	long value1 = r.nextLong();
    	msg.putLong(value1);
    	msg.putLong(r.nextLong());
    	msg.putLong(r.nextLong());
    	msg.putLong(r.nextLong());
    	long value2 = r.nextLong();
    	msg.putLong(value2);
    	msg.putLong(r.nextLong());
    	msg.putLong(r.nextLong());
    	msg.putLong(r.nextLong());
    	msg.clear();
    	
    	SignumValue amount = SignumValue.fromSigna(2);
    	TransactionBroadcast tb = sendMessage(BT.PASSPHRASE, testContract.getId(), amount, SignumValue.fromSigna(0.1), 100,
    			msg.array());
    	forgeBlock(tb);
    	forgeBlock();
    	forgeBlock();

    	long value1Contract = BT.getContractFieldValue(testContract, 0);
    	long value2Contract = BT.getContractFieldValue(testContract, 4);
    	assertEquals(value1, value1Contract);
    	assertEquals(value2, value2Contract);
    }

    @Test
    public void testCheckSignature() throws Exception{
    	forgeBlock();
    	AT testContract = registerContract(CheckSignature.class, SignumValue.fromSigna(0.4));
    	System.out.println(testContract.getId().getID());
    	
    	Compiler comp = BT.compileContract(CheckSignature.class);

    	Random r = new Random();
    	long msg2 = r.nextLong();
    	long msg3 = r.nextLong();
    	long msg4 = r.nextLong();

    	ByteBuffer msg = ByteBuffer.allocate(8*4);
    	msg.order(ByteOrder.LITTLE_ENDIAN);
    	msg.putLong(testContract.getId().getSignedLongId());
    	msg.putLong(msg2);
    	msg.putLong(msg3);
    	msg.putLong(msg4);
    	msg.clear();
    	
    	byte[] signature = SignumCrypto.getInstance().sign(msg.array(), BT.PASSPHRASE);
    	
    	SignumValue amount = SignumValue.fromSigna(2);
    	Method method = comp.getMethod("checkSignature");
    	
    	// send a null signature first
    	TransactionBroadcast tb = BT.callMethod(BT.PASSPHRASE, testContract.getId(), method, amount, SignumValue.fromSigna(0.1), 1000,
    			msg2, msg3, msg4);
    	forgeBlock(tb);
    	forgeBlock();
    	forgeBlock();

    	long verified = BT.getContractFieldValue(testContract, 0);
    	assertEquals(0, verified);

    	// send the correct signature now
    	tb = BT.callMethod(BT.PASSPHRASE, testContract.getId(), method, signature, amount, SignumValue.fromSigna(0.1), 1000,
    			msg2, msg3, msg4);
    	forgeBlock(tb);
    	forgeBlock();
    	forgeBlock();

    	verified = BT.getContractFieldValue(testContract, 0);
    	assertEquals(1, verified);
    }
    
    @Test
    public void testCodeHash() throws Exception{
    	forgeBlock();
    	AT testContract = registerContract(CodeHashId.class, SignumValue.fromSigna(0.4));
    	System.out.println(testContract.getId().getID());
    	
    	Compiler comp = BT.compileContract(CodeHashId.class);
    	Method method = comp.getMethod("getCodeHash");
    	
    	Random r = new Random();
    	SignumValue amount = SignumValue.fromSigna(2);
    	TransactionBroadcast tb = BT.callMethod(BT.PASSPHRASE, testContract.getId(), method, amount, SignumValue.fromSigna(0.1), 1000,
    			r.nextLong());
    	forgeBlock(tb);
    	forgeBlock();
    	forgeBlock();

    	long thisCodeHash = BT.getContractFieldValue(testContract, 0);
    	long codeHash = BT.getContractFieldValue(testContract, 1);

    	assertEquals(thisCodeHash, testContract.getMachineCodeHashId().getSignedLongId());
    	assertEquals(0L, codeHash);
    	
    	tb = BT.callMethod(BT.PASSPHRASE, testContract.getId(), method, amount, SignumValue.fromSigna(0.1), 1000,
    			testContract.getId().getSignedLongId());
    	forgeBlock(tb);
    	forgeBlock();
    	forgeBlock();
    	
    	thisCodeHash = BT.getContractFieldValue(testContract, 0);
    	codeHash = BT.getContractFieldValue(testContract, 1);
    	
    	assertEquals(thisCodeHash, codeHash);
    	assertTrue(thisCodeHash != 0L);
    }

}
