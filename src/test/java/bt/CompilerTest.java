package bt;

import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.response.AT;
import burst.kit.entity.response.Account;

import org.junit.Test;

import bt.compiler.Compiler;
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
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * We assume a localhost testnet with 0 seconds mock mining is available for the
 * tests to work.
 * 
 * @author jjos
 */
public class CompilerTest extends BT {

    public static void main(String[] args) throws Exception {
        CompilerTest t = new CompilerTest();
        // t.testForward();
        // t.testForwardMin();
        // t.testTipThanks();
        // t.testOdds();
        // t.testLocalVar();
        // t.testMethodCall();
        // t.testMethodCallArgs();
        // t.testCounter();
         t.testCounter2();
        // t.testSha256_64();
        // t.testAuction();
        // t.testAuctionNFT();
        // t.testMultiSigLock();
        // t.testHashTimelockRefund();
        // t.testHashTimelockPay();
    }

    @Test
    public void testOdds() throws Exception {

        // Send some burst for the players
        forgeBlock(PASSPHRASE, 500);
        BurstAddress player1 = bc.getBurstAddressFromPassphrase(PASSPHRASE2);
        BurstAddress player2 = bc.getBurstAddressFromPassphrase(PASSPHRASE3);
        sendAmount(PASSPHRASE, player1, BurstValue.fromBurst(1000)).blockingGet();
        sendAmount(PASSPHRASE, player2, BurstValue.fromBurst(1000)).blockingGet();
        forgeBlock(PASSPHRASE, 500);

        BurstValue actvFee = BurstValue.fromBurst(10);
        BurstValue amount = BurstValue.fromBurst(100);

        AT at = registerContract(OddsGame.class, actvFee);
        assertNotNull("AT could not be registered", at);

        // Fill the contract with 3 times the max payment value
        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck(OddsGame.MAX_PAYMENT * 3));
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
        BurstAddress address = BurstAddress.fromRs(Forward.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, BurstValue.fromPlanck(1)).blockingGet();
        forgeBlock();

        Account bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue balance = bmfAccount.getUnconfirmedBalance();
        BurstValue actvFee = BurstValue.fromBurst(1);
        BurstValue amount = BurstValue.fromBurst(10);

        AT at = registerContract(Forward.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getId(), amount);
        forgeBlock();
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue newBalance = bmfAccount.getUnconfirmedBalance();
        double result = newBalance.doubleValue() - balance.doubleValue() - actvFee.doubleValue();

        assertTrue("Value not forwarded", result > actvFee.doubleValue());
    }

    @Test
    public void testForwardMin() throws Exception {
        BurstAddress address = BurstAddress.fromEither(ForwardMin.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, BurstValue.fromPlanck(1));
        forgeBlock();

        Account bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue balance = bmfAccount.getUnconfirmedBalance();
        BurstValue actvFee = BurstValue.fromBurst(1);
        double amount = ForwardMin.MIN_AMOUNT * 0.8;

        AT at = registerContract(ForwardMin.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck((long) amount));
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        BurstValue newBalance = bmfAccount.getUnconfirmedBalance();
        double result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value forwarded while it should not", result < amount);

        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck((long) amount));
        forgeBlock();
        forgeBlock();

        bmfAccount = bns.getAccount(address).blockingGet();
        newBalance = bmfAccount.getUnconfirmedBalance();
        result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value not forwarded as it should", result * Contract.ONE_BURST > amount);
    }

    @Test
    public void testTipThanks() throws Exception {
        BurstAddress address = BurstAddress.fromEither(TipThanks.ADDRESS);

        // send some burst to make sure the account exist
        sendAmount(PASSPHRASE, address, BurstValue.fromBurst(100));
        forgeBlock();

        Account benefAccout = bns.getAccount(address).blockingGet();
        BurstValue balance = benefAccout.getUnconfirmedBalance();
        BurstValue actvFee = BurstValue.fromBurst(1);
        double amount = TipThanks.MIN_AMOUNT * 0.8;

        AT at = registerContract(TipThanks.class, actvFee);
        assertNotNull("AT could not be registered", at);

        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck((long) amount));
        forgeBlock();

        benefAccout = bns.getAccount(address).blockingGet();
        BurstValue newBalance = benefAccout.getUnconfirmedBalance();
        double result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value forwarded while it should not", result < amount);

        sendAmount(PASSPHRASE, at.getId(), BurstValue.fromPlanck((long) amount));
        forgeBlock();
        forgeBlock();

        benefAccout = bns.getAccount(address).blockingGet();
        newBalance = benefAccout.getUnconfirmedBalance();
        result = newBalance.doubleValue() - balance.doubleValue();
        assertTrue("Value not forwarded as it should", result * Contract.ONE_BURST > amount);
    }

    @Test
    public void testLocalVar() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(LocalVar.class);

        String name = LocalVar.class.getSimpleName() + System.currentTimeMillis();
        BurstAddress creator = BurstCrypto.getInstance().getBurstAddressFromPassphrase(BT.PASSPHRASE);

        BT.registerContract(BT.PASSPHRASE, comp, name, name, BurstValue.fromPlanck(LocalVar.FEE),
                BurstValue.fromBurst(0.1), 1000).blockingGet();
        BT.forgeBlock();

        AT contract = BT.findContract(creator, name);

        BurstValue valueSent = BurstValue.fromBurst(10);
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), valueSent).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        assertEquals(valueSent.longValue() * Contract.ONE_BURST,
                BT.getContractFieldValue(contract, comp.getField("amountNoFee").getAddress()));

        long value = 512;
        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("setValue"), BurstValue.fromBurst(1),
                BurstValue.fromBurst(0.1), 1000, value).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        long valueChain = BT.getContractFieldValue(contract, comp.getField("valueTimes2").getAddress());

        assertEquals(value * 2, valueChain);
    }

    @Test
    public void testMethodCall() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(MethodCall.class);

        String name = MethodCall.class.getSimpleName() + System.currentTimeMillis();

        AT contract = BT.registerContract(comp, name, BurstValue.fromBurst(10));

        // variable not initialized yet
        assertEquals(0, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method1"), BurstValue.fromBurst(10),
                BurstValue.fromBurst(0.1), 1000).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        assertEquals(1, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method2"), BurstValue.fromBurst(10),
                BurstValue.fromBurst(0.1), 1000).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        assertEquals(2, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));
    }

    @Test
    public void testMethodCallArgs() throws Exception {
        BT.forgeBlock();
        Compiler comp = BT.compileContract(MethodCallArgs.class);

        String name = MethodCallArgs.class.getSimpleName() + System.currentTimeMillis();

        AT contract = BT.registerContract(comp, name, BurstValue.fromBurst(30));
        System.out.println(contract.getId().getID());

        // variable not initialized yet
        assertEquals(0, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method1"), BurstValue.fromBurst(30),
                BurstValue.fromBurst(0.1), 1000, 100).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        assertEquals(1, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));
        assertEquals(100, BT.getContractFieldValue(contract, comp.getFieldAddress("arg1")));
        assertEquals(-1, BT.getContractFieldValue(contract, comp.getFieldAddress("arg2")));
        assertEquals(-1, BT.getContractFieldValue(contract, comp.getFieldAddress("arg3")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method2"), BurstValue.fromBurst(30),
                BurstValue.fromBurst(0.1), 1000, 100, 200).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        assertEquals(2, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));
        assertEquals(100, BT.getContractFieldValue(contract, comp.getFieldAddress("arg1")));
        assertEquals(200, BT.getContractFieldValue(contract, comp.getFieldAddress("arg2")));
        assertEquals(-1, BT.getContractFieldValue(contract, comp.getFieldAddress("arg3")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method3"), BurstValue.fromBurst(30),
                BurstValue.fromBurst(0.1), 1000, 100, 200, 300).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        assertEquals(3, BT.getContractFieldValue(contract, comp.getFieldAddress("methodCalled")));
        assertEquals(100, BT.getContractFieldValue(contract, comp.getFieldAddress("arg1")));
        assertEquals(200, BT.getContractFieldValue(contract, comp.getFieldAddress("arg2")));
        assertEquals(300, BT.getContractFieldValue(contract, comp.getFieldAddress("arg3")));

        BT.callMethod(BT.PASSPHRASE, contract.getId(), comp.getMethod("method4"), BurstValue.fromBurst(30),
                BurstValue.fromBurst(0.1), 1000, 1000, 2000, 3000).blockingGet();
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
                BurstValue.fromBurst(10));
        System.out.println(contract.getId().getSignedLongId());

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(20)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());
        assertEquals(1, ntx);
        assertEquals(1, nblocks);
        assertEquals(BT.getBurstAddressFromPassphrase(PASSPHRASE).getSignedLongId(), address);

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(20), BurstValue.fromBurst(0.1))
                .blockingGet();
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), BurstValue.fromBurst(20), BurstValue.fromBurst(1))
                .blockingGet();
        BT.sendAmount(BT.PASSPHRASE3, contract.getId(), BurstValue.fromBurst(20), BurstValue.fromBurst(1))
                .blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());

        assertEquals(4, ntx);
        assertEquals(2, nblocks);
    }

    public void testCounter2() throws Exception {
        BT.forgeBlock();

        long ntx, nblocks, address;

        Compiler compiled = BT.compileContract(TXCounter2.class);
        AT contract = BT.registerContract(compiled, TXCounter2.class.getSimpleName() + System.currentTimeMillis(),
                BurstValue.fromBurst(10));
        System.out.println(contract.getId().getSignedLongId());

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(20)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());
        assertEquals(1, ntx);
        assertEquals(1, nblocks);
        assertEquals(BT.getBurstAddressFromPassphrase(PASSPHRASE).getSignedLongId(), address);

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(20), BurstValue.fromBurst(0.1))
                .blockingGet();
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), BurstValue.fromBurst(20), BurstValue.fromBurst(1))
                .blockingGet();
        BT.sendAmount(BT.PASSPHRASE3, contract.getId(), BurstValue.fromBurst(20), BurstValue.fromBurst(1))
                .blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        ntx = BT.getContractFieldValue(contract, compiled.getField("ntx").getAddress());
        nblocks = BT.getContractFieldValue(contract, compiled.getField("nblocks").getAddress());
        address = BT.getContractFieldValue(contract, compiled.getField("address").getAddress());

        assertEquals(4, ntx);
        assertEquals(2, nblocks);
    }

    public void testSha256_64() throws Exception {
        BT.forgeBlock();

        long sha_chain, sha;

        Compiler compiled = BT.compileContract(Sha256_64.class);
        AT contract = BT.registerContract(compiled, "sha" + System.currentTimeMillis(), BurstValue.fromBurst(10));
        System.out.println(contract.getId().getSignedLongId());

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(20)).blockingGet();
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
                BurstValue.fromBurst(Auction.ACTIVATION_FEE));
        System.out.println(contract.getId().getID());

        // lets just initialized the contract
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(30)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        long benef_chain = BT.getContractFieldValue(contract, compiled.getField("beneficiary").getAddress());
        assertEquals(benef_chain, BurstAddress.fromEither(Auction.BENEFICIARY).getSignedLongId());

        long isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());
        assertEquals(1, isOpen);

        long bidder = BT.getContractFieldValue(contract, compiled.getField("highestBidder").getAddress());
        assertEquals(0, bidder);

        // send an acution smaller than the min bid
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromPlanck(Auction.INITIAL_PRICE / 2)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        BurstValue balance = BT.getContractBalance(contract);
        // bid should be refused
        assertTrue(balance.longValue() < 30 * Contract.ONE_BURST);

        // send bid with enough funds
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), BurstValue.fromPlanck(Auction.INITIAL_PRICE * 2)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());
        assertEquals(1, isOpen);

        // bid should accepted
        bidder = BT.getContractFieldValue(contract, compiled.getField("highestBidder").getAddress());
        assertEquals(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2).getSignedLongId(), bidder);

        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() > Auction.INITIAL_PRICE - 30 * Contract.ONE_BURST);

        // send another bit with higher value
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromPlanck(Auction.INITIAL_PRICE * 3)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        // bid should be accepted
        bidder = BT.getContractFieldValue(contract, compiled.getField("highestBidder").getAddress());
        assertEquals(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE).getSignedLongId(), bidder);

        // wait the contract to time out
        for (int i = 0; i < Auction.TIMEOUT_MIN / 4; i++) {
            BT.forgeBlock();
        }
        // make the contract to run to see the payment
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(30)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() < 30 * Contract.ONE_BURST);
    }

    public void testAuctionNFT() throws Exception {
        BT.forgeBlock();

        Compiler compiled = BT.compileContract(AuctionNFT.class);
        AT contract = BT.registerContract(compiled, AuctionNFT.class.getSimpleName() + System.currentTimeMillis(),
                BurstValue.fromPlanck(AuctionNFT.ACTIVATION_FEE));
        System.out.println(contract.getId().getID());

        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(30)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        long ben_chain = BT.getContractFieldValue(contract, compiled.getField("beneficiary").getAddress());
        assertEquals(ben_chain, contract.getCreator().getBurstID().getSignedLongId());

        long isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());

        assertTrue("should not be open", isOpen == 0);

        long minbid = 1000 * Contract.ONE_BURST;
        long timeout_mins = 40;

        // open the contract for auction
        BT.callMethod(BT.PASSPHRASE, contract.getId(), compiled.getMethod("open"),
                BurstValue.fromPlanck(AuctionNFT.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000, timeout_mins, minbid,
                contract.getCreator().getSignedLongId() // beneficiary
        ).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());
        long minbid_chain = BT.getContractFieldValue(contract, compiled.getField("highestBid").getAddress());
        long benef_chain = BT.getContractFieldValue(contract, compiled.getField("beneficiary").getAddress());

        assertTrue("should now be open", isOpen == 1);
        assertEquals(minbid, minbid_chain);
        assertEquals(contract.getCreator().getSignedLongId(), benef_chain);

        // send an acution smaller than the min bid
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromPlanck(minbid / 2)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        BurstValue balance = BT.getContractBalance(contract);
        // bid should be refused
        assertTrue(balance.longValue() < 60 * Contract.ONE_BURST);

        // send bid with enough funds
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), BurstValue.fromPlanck(minbid * 2)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        isOpen = BT.getContractFieldValue(contract, compiled.getField("isOpen").getAddress());
        assertEquals(1, isOpen);

        // bid should accepted
        long bidder = BT.getContractFieldValue(contract, compiled.getFieldAddress("highestBidder"));
        assertEquals(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2).getSignedLongId(), bidder);

        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() > minbid);

        // send another bit with higher value
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromPlanck(minbid * 3)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        // bid should be accepted
        bidder = BT.getContractFieldValue(contract, compiled.getField("highestBidder").getAddress());
        assertEquals(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE).getSignedLongId(), bidder);

        // wait the contract to time out
        for (int i = 0; i < timeout_mins/ 4; i++) {
            BT.forgeBlock();
        }
        // make the contract to run to see the payment
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(30)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        balance = BT.getContractBalance(contract);
        assertTrue(balance.longValue() < 30 * Contract.ONE_BURST);
    }


    public void testMultiSigLock() throws Exception {
        BT.forgeBlock();

        Compiler compiled = BT.compileContract(MultiSigLock.class);
        AT contract = BT.registerContract(compiled, MultiSigLock.class.getSimpleName() + System.currentTimeMillis(),
                BurstValue.fromPlanck(MultiSigLock.ACTIVATION_FEE));
        System.out.println(contract.getId().getID());

        long initialBalance = 1000*Contract.ONE_BURST;

        // initialize the contract and put some balance
        BT.sendAmount(BT.PASSPHRASE, contract.getId(),
                BurstValue.fromPlanck(initialBalance + MultiSigLock.ACTIVATION_FEE)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        BurstAddress owner1 = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE);
        BurstAddress owner2 = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2);
        BurstAddress owner3 = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE3);

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
                BurstValue.fromPlanck(MultiSigLock.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000,
                beneficiary, amount
        ).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());
        long receiver1 = BT.getContractFieldValue(contract, compiled.getField("receiver1").getAddress());
        assertEquals(1, nSignatures);
        assertEquals(beneficiary, receiver1);

        // sign again with the same wallet, should keep a single signature valid
        BT.callMethod(BT.PASSPHRASE, contract.getId(), compiled.getMethod("sign"),
                BurstValue.fromPlanck(MultiSigLock.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000,
                beneficiary, amount
        ).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());
        assertEquals(1, nSignatures);

        // add a second sign
        BT.callMethod(BT.PASSPHRASE2, contract.getId(), compiled.getMethod("sign"),
                BurstValue.fromPlanck(MultiSigLock.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000,
                beneficiary, amount
        ).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());
        long receiver2 = BT.getContractFieldValue(contract, compiled.getField("receiver2").getAddress());
        assertEquals(2, nSignatures);
        assertEquals(beneficiary, receiver2);

        // add a third sign, but incompatible
        BT.callMethod(BT.PASSPHRASE3, contract.getId(), compiled.getMethod("sign"),
                BurstValue.fromPlanck(MultiSigLock.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000,
                beneficiary, amount*2
        ).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();
        nSignatures = BT.getContractFieldValue(contract, compiled.getField("nSignatures").getAddress());
        assertEquals(1, nSignatures);

        // add a third sign to complete the transfer
        BT.callMethod(BT.PASSPHRASE3, contract.getId(), compiled.getMethod("sign"),
                BurstValue.fromPlanck(MultiSigLock.ACTIVATION_FEE), BurstValue.fromBurst(0.1), 1000,
                beneficiary, amount
        ).blockingGet();
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

        BurstAddress beneficiary = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2);
        Register key = Register.newInstance(1, 2, 3, 4);
        Register hahsedkey = Contract.performSHA256_(key);

        long []data = {
                hahsedkey.getValue1(), hahsedkey.getValue2(), hahsedkey.getValue3(), hahsedkey.getValue4(),
                beneficiary.getSignedLongId()
        };

        String name = HashedTimeLock.class.getSimpleName() + System.currentTimeMillis();
        BT.registerContract(BT.PASSPHRASE, compiled.getCode(), compiled.getDataPages(),
                name, "test", data,
                BurstValue.fromPlanck(HashedTimeLock.ACTIVATION_FEE), BT.getMinRegisteringFee(compiled),
                1000).blockingGet();
        BT.forgeBlock(BT.PASSPHRASE2);
        BT.forgeBlock(BT.PASSPHRASE2);

        AT contract = BT.findContract(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE), name);
        System.out.println(contract.getId().getID());

        // Initialize the contract with a given amount, this also initializes the timeout
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(1000)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // Get the funds back after the timeout
        BT.forgeBlock();
        BT.forgeBlock();
        BT.forgeBlock();
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromPlanck(HashedTimeLock.ACTIVATION_FEE)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        long balance = BT.getContractBalance(contract).longValue();
        assertEquals(0, balance);
    }

    public void testHashTimelockPay() throws Exception {
        BT.forgeBlock();

        Compiler compiled = BT.compileContract(HashedTimeLock.class);

        BurstAddress beneficiary = BT.getBurstAddressFromPassphrase(BT.PASSPHRASE2);
        Register key = Register.newInstance(1, 2, 3, 4);
        Register hahsedkey = Contract.performSHA256_(key);

        long []data = {
                hahsedkey.getValue1(), hahsedkey.getValue2(), hahsedkey.getValue3(), hahsedkey.getValue4(),
                beneficiary.getSignedLongId()
        };

        String name = HashedTimeLock.class.getSimpleName() + System.currentTimeMillis();
        BT.registerContract(BT.PASSPHRASE, compiled.getCode(), compiled.getDataPages(),
                name, "test", data,
                BurstValue.fromPlanck(HashedTimeLock.ACTIVATION_FEE), BT.getMinRegisteringFee(compiled),
                1000).blockingGet();
        BT.forgeBlock(BT.PASSPHRASE2);
        BT.forgeBlock(BT.PASSPHRASE2);

        AT contract = BT.findContract(BT.getBurstAddressFromPassphrase(BT.PASSPHRASE), name);
        System.out.println(contract.getId().getID());

        // Initialize the contract with a given amount, this also initializes the timeout
        BT.sendAmount(BT.PASSPHRASE, contract.getId(), BurstValue.fromBurst(1000)).blockingGet();
        BT.forgeBlock();
        BT.forgeBlock();

        // Request the payment without the key, should not pay
        BT.sendAmount(BT.PASSPHRASE2, contract.getId(), BurstValue.fromPlanck(HashedTimeLock.ACTIVATION_FEE)).blockingGet();
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

        BT.sendMessage(BT.PASSPHRASE2, contract.getId(), BurstValue.fromPlanck(HashedTimeLock.ACTIVATION_FEE),
                BurstValue.fromBurst(0.1), 1000, b.array()).blockingGet();
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
}
