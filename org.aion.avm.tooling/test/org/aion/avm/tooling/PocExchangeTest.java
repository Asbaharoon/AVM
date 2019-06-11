package org.aion.avm.tooling;

import avm.Address;
import org.aion.avm.core.AvmConfiguration;
import org.aion.avm.core.AvmImpl;
import org.aion.avm.core.CommonAvmFactory;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.tooling.testExchange.*;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.kernel.*;
import org.aion.vm.api.interfaces.KernelInterface;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.*;

import java.math.BigInteger;


public class PocExchangeTest {
    private static KernelInterface kernel;
    private static AvmImpl avm;
    private static byte[] testERC20Jar;
    private static byte[] testExchangeJar;

    @BeforeClass
    public static void setup() {
        TestingBlock block = new TestingBlock(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);
        kernel = new TestingKernel(block);
        avm = CommonAvmFactory.buildAvmInstanceForConfiguration(new StandardCapabilities(), new AvmConfiguration());
        
        testERC20Jar = JarBuilder.buildJarForMainAndClassesAndUserlib(CoinController.class, ERC20Token.class);
        testExchangeJar = JarBuilder.buildJarForMainAndClassesAndUserlib(ExchangeController.class, Exchange.class, ExchangeTransaction.class, ERC20Token.class);
    }

    @AfterClass
    public static void tearDown() {
        avm.shutdown();
    }

    private long energyLimit = 6_000_0000;

    private org.aion.vm.api.types.Address pepeMinter = Helpers.randomAddress();
    private org.aion.vm.api.types.Address memeMinter = Helpers.randomAddress();
    private org.aion.vm.api.types.Address exchangeOwner = Helpers.randomAddress();
    private org.aion.vm.api.types.Address usr1 = Helpers.randomAddress();
    private org.aion.vm.api.types.Address usr2 = Helpers.randomAddress();
    private org.aion.vm.api.types.Address usr3 = Helpers.randomAddress();


    class CoinContract{
        private org.aion.vm.api.types.Address addr;
        private org.aion.vm.api.types.Address minter;

        CoinContract(org.aion.vm.api.types.Address contractAddr, org.aion.vm.api.types.Address minter, byte[] jar, byte[] arguments){
            kernel.adjustBalance(minter, BigInteger.valueOf(1_000_000_000L));
            kernel.adjustBalance(pepeMinter, BigInteger.valueOf(1_000_000_000L));
            kernel.adjustBalance(memeMinter, BigInteger.valueOf(1_000_000_000L));
            kernel.adjustBalance(exchangeOwner, BigInteger.valueOf(1_000_000_000L));
            kernel.adjustBalance(usr1, BigInteger.valueOf(1_000_000_000L));
            kernel.adjustBalance(usr2, BigInteger.valueOf(1_000_000_000L));
            kernel.adjustBalance(usr3, BigInteger.valueOf(1_000_000_000L));

            this.addr = contractAddr;
            this.minter = minter;
            this.addr = initCoin(jar, arguments);
        }

        private org.aion.vm.api.types.Address initCoin(byte[] jar, byte[] arguments){
            TestingTransaction createTransaction = TestingTransaction.create(minter, kernel.getNonce(minter), BigInteger.ZERO, new CodeAndArguments(jar, arguments).encodeToBytes(), energyLimit, 1L);
            TransactionResult createResult = avm.run(kernel, new TestingTransaction[] {createTransaction})[0].get();
            Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, createResult.getResultCode());
            return org.aion.vm.api.types.Address.wrap(createResult.getReturnData());
        }

        public TransactionResult callTotalSupply() {
            byte[] args = ABIUtil.encodeMethodArguments("totalSupply");
            return call(minter, args);
        }

        private TransactionResult callBalanceOf(org.aion.vm.api.types.Address toQuery) {
            byte[] args = ABIUtil.encodeMethodArguments("balanceOf", new Address(toQuery.toBytes()));
            return call(minter, args);
        }

        private TransactionResult callMint(org.aion.vm.api.types.Address receiver, long amount) {
            byte[] args = ABIUtil.encodeMethodArguments("mint", new Address(receiver.toBytes()), amount);
            return call(minter, args);
        }

        private TransactionResult callTransfer(org.aion.vm.api.types.Address sender, org.aion.vm.api.types.Address receiver, long amount) {
            byte[] args = ABIUtil.encodeMethodArguments("transfer", new Address(receiver.toBytes()), amount);
            return call(sender, args);
        }

        private TransactionResult callAllowance(org.aion.vm.api.types.Address owner, org.aion.vm.api.types.Address spender) {
            byte[] args = ABIUtil.encodeMethodArguments("allowance", new Address(owner.toBytes()), new Address(spender.toBytes()));
            return call(minter, args);
        }

        private TransactionResult callApprove(org.aion.vm.api.types.Address owner, org.aion.vm.api.types.Address spender, long amount) {
            byte[] args = ABIUtil.encodeMethodArguments("approve", new Address(spender.toBytes()), amount);
            return call(owner, args);
        }

        private TransactionResult callTransferFrom(org.aion.vm.api.types.Address executor, org.aion.vm.api.types.Address from, org.aion.vm.api.types.Address to, long amount) {
            byte[] args = ABIUtil.encodeMethodArguments("transferFrom", new Address(from.toBytes()), new Address(to.toBytes()), amount);
            return call(executor, args);
        }

        private TransactionResult call(org.aion.vm.api.types.Address sender, byte[] args) {
            TestingTransaction callTransaction = TestingTransaction.call(sender, addr, kernel.getNonce(sender), BigInteger.ZERO, args, energyLimit, 1l);
            TransactionResult callResult = avm.run(kernel, new TestingTransaction[] {callTransaction})[0].get();
            Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, callResult.getResultCode());
            return callResult;
        }
    }

    class ExchangeContract{
        private org.aion.vm.api.types.Address addr;
        private org.aion.vm.api.types.Address owner;

        ExchangeContract(org.aion.vm.api.types.Address contractAddr, org.aion.vm.api.types.Address owner, byte[] jar){
            this.addr = contractAddr;
            this.owner = owner;
            this.addr = initExchange(jar, null);
        }

        private org.aion.vm.api.types.Address initExchange(byte[] jar, byte[] arguments){
            TestingTransaction createTransaction = TestingTransaction.create(owner, kernel.getNonce(owner), BigInteger.ZERO, new CodeAndArguments(jar, arguments).encodeToBytes(), energyLimit, 1L);
            TransactionResult createResult = avm.run(kernel, new TestingTransaction[] {createTransaction})[0].get();
            Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, createResult.getResultCode());
            return org.aion.vm.api.types.Address.wrap(createResult.getReturnData());
        }

        public TransactionResult callListCoin(String name, org.aion.vm.api.types.Address coinAddr) {
            byte[] args = ABIUtil.encodeMethodArguments("listCoin", name.toCharArray(), new Address(coinAddr.toBytes()));
            return call(owner,args);
        }

        public TransactionResult callRequestTransfer(String name, org.aion.vm.api.types.Address from,  org.aion.vm.api.types.Address to, long amount) {
            byte[] args = ABIUtil.encodeMethodArguments("requestTransfer", name.toCharArray(), new Address(to.toBytes()), amount);
            return call(from,args);
        }

        public TransactionResult callProcessExchangeTransaction(org.aion.vm.api.types.Address sender) {
            byte[] args = ABIUtil.encodeMethodArguments("processExchangeTransaction");
            return call(sender,args);
        }

        private TransactionResult call(org.aion.vm.api.types.Address sender, byte[] args) {
            TestingTransaction callTransaction = TestingTransaction.call(sender, addr, kernel.getNonce(sender), BigInteger.ZERO, args, energyLimit, 1l);
            TransactionResult callResult = avm.run(kernel, new TestingTransaction[] {callTransaction})[0].get();
            Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, callResult.getResultCode());
            return callResult;
        }
    }

    @Test
    public void testERC20() {
        TransactionResult res;
        byte[] arguments = ABIUtil.encodeDeploymentArguments("Pepe", "PEPE", 8);
        CoinContract pepe = new CoinContract(null, pepeMinter, testERC20Jar, arguments);

        res = pepe.callTotalSupply();
        Assert.assertEquals(0L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callBalanceOf(usr1);
        Assert.assertEquals(0L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callBalanceOf(usr2);
        Assert.assertEquals(0L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callMint(usr1, 5000L);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = pepe.callBalanceOf(usr1);
        Assert.assertEquals(5000L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callMint(usr2, 10000L);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = pepe.callBalanceOf(usr2);
        Assert.assertEquals(10000L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callTransfer(usr1, usr2, 2000L);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = pepe.callBalanceOf(usr1);
        Assert.assertEquals(3000L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callBalanceOf(usr2);
        Assert.assertEquals(12000L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callAllowance(usr1, usr2);
        Assert.assertEquals(0L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callApprove(usr1, usr3, 1000L);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = pepe.callAllowance(usr1, usr3);
        Assert.assertEquals(1000L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callTransferFrom(usr3, usr1, usr2, 500L);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = pepe.callAllowance(usr1, usr3);
        Assert.assertEquals(500L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callBalanceOf(usr1);
        Assert.assertEquals(2500L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callBalanceOf(usr2);
        Assert.assertEquals(12500L, new ABIDecoder(res.getReturnData()).decodeOneLong());
    }

    @Test
    public void testExchange() {
        byte[] arguments = ABIUtil.encodeDeploymentArguments("Pepe", "PEPE", 8);
        CoinContract pepe = new CoinContract(null, pepeMinter, testERC20Jar, arguments);

        arguments = ABIUtil.encodeDeploymentArguments("Meme", "MEME", 8);
        CoinContract meme = new CoinContract(null, memeMinter, testERC20Jar, arguments);

        ExchangeContract ex = new ExchangeContract(null, exchangeOwner, testExchangeJar);

        TransactionResult res;

        res = ex.callListCoin("PEPE", pepe.addr);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = ex.callListCoin("MEME", meme.addr);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = pepe.callMint(usr1, 5000L);
        res = pepe.callMint(usr2, 5000L);

        res = pepe.callApprove(usr1, ex.addr, 2000L);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = ex.callRequestTransfer("PEPE", usr1, usr2, 1000L);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = ex.callProcessExchangeTransaction(exchangeOwner);
        Assert.assertEquals(true, new ABIDecoder(res.getReturnData()).decodeOneBoolean());

        res = pepe.callBalanceOf(usr1);
        Assert.assertEquals(4000L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callBalanceOf(usr2);
        Assert.assertEquals(6000L, new ABIDecoder(res.getReturnData()).decodeOneLong());

        res = pepe.callAllowance(usr1, ex.addr);
        Assert.assertEquals(1000L, new ABIDecoder(res.getReturnData()).decodeOneLong());
    }
}
