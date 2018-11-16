package org.aion.avm.core.util;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.Address;
import org.aion.avm.arraywrapper.ObjectArray;
import org.aion.avm.internal.GeneratedClassesFactory;
import org.aion.avm.internal.IHelper;
import org.aion.avm.internal.RuntimeAssertionError;
import org.aion.avm.shadow.java.lang.Class;
import org.aion.kernel.TransactionResult;


/**
 * Implements the IHelper interface for tests which need to create runtime objects or otherwise interact with the parts of the system
 * which assume that there is an IHelper installed.
 * It automatically installs itself as the helper and provides utilities to install and remove itself from IHelper.currentContractHelper.
 * Additionally, it provides some common static helpers for common cases of its use.
 */
public class TestingHelper implements IHelper {
    public static Address buildAddress(byte[] raw) {
        TestingHelper helper = new TestingHelper(false);
        Address data = new Address(raw);
        helper.remove();
        return data;
    }
    public static Object decodeResult(TransactionResult result) {
        return decodeResultRaw(result.getReturnData());
    }
    public static Object decodeResultRaw(byte[] returnData) {
        Object data = null;
        if (null != returnData) {
            TestingHelper helper = new TestingHelper(false);
            data = ABIDecoder.decodeOneObject(returnData);
            helper.remove();
        }
        return data;
    }
    public static ObjectArray construct2DWrappedArray(Object data) {
        TestingHelper helper = new TestingHelper(false);
        ObjectArray ret = null;
        if (data.getClass().getName() == "[[C") {
            ret = (ObjectArray) GeneratedClassesFactory.construct2DCharArray((char[][]) data);
        }
        else if (data.getClass().getName() == "[[I") {
            ret = (ObjectArray) GeneratedClassesFactory.construct2DIntArray((int[][]) data);
        }// add code for other 2D wrapped array when needed.
        helper.remove();
        return ret;
    }

    public static ObjectArray construct1DWrappedStringArray(Object data) {
        TestingHelper helper = new TestingHelper(false);
        ObjectArray ret = null;
        if (data.getClass().getName() == "[Ljava.lang.String;") {
            org.aion.avm.shadow.java.lang.String[] shadowArray = new org.aion.avm.shadow.java.lang.String[((String[])data).length];
            for (int i = 0; i < ((String[])data).length; i++) {
                shadowArray[i] = new org.aion.avm.shadow.java.lang.String(((String[]) data)[i]);
            }
            ret = (ObjectArray) GeneratedClassesFactory.construct1DStringArray(shadowArray);
        }
        helper.remove();
        return ret;
    }

    /**
     * A special entry-point used only the test wallet when running the constract, inline.  This allows the helper to be setup for constant initialization.
     * 
     * @param invocation The invocation to run under the helper.
     */
    public static void runUnderBoostrapHelper(Runnable invocation) {
        TestingHelper helper = new TestingHelper(true);
        try {
            invocation.run();
        } finally {
            helper.remove();
        }
    }


    private final boolean isBootstrapOnly;
    private final int constantHashCode;

    private TestingHelper(boolean isBootstrapOnly) {
        this.isBootstrapOnly = isBootstrapOnly;
        // If this is a helper created for bootstrap purposes, use the "placeholder hash code" we rely on for constants.
        // Otherwise, use something else so we know we aren't accidentally being used for constant init.
        this.constantHashCode = isBootstrapOnly ? Integer.MIN_VALUE : -1;
        install();
    }

    private void install() {
        RuntimeAssertionError.assertTrue(null == IHelper.currentContractHelper.get());
        IHelper.currentContractHelper.set(this);
    }
    private void remove() {
        RuntimeAssertionError.assertTrue(this == IHelper.currentContractHelper.get());
        IHelper.currentContractHelper.remove();
    }

    @Override
    public void externalChargeEnergy(long cost) {
        // Free!
    }

    @Override
    public long externalGetEnergyRemaining() {
        throw RuntimeAssertionError.unreachable("Shouldn't be called in the testing code");
    }

    @Override
    public Class<?> externalWrapAsClass(java.lang.Class<?> input) {
        throw RuntimeAssertionError.unreachable("Shouldn't be called in the testing code");
    }

    @Override
    public int externalGetNextHashCodeAndIncrement() {
        return this.constantHashCode;
    }

    @Override
    public int externalPeekNextHashCode() {
        throw RuntimeAssertionError.unreachable("Shouldn't be called in the testing code");
    }

    @Override
    public int captureSnapshotAndNextHashCode() {
        throw RuntimeAssertionError.unreachable("Shouldn't be called in the testing code");
    }

    @Override
    public void applySnapshotAndNextHashCode(int nextHashCode) {
        throw RuntimeAssertionError.unreachable("Shouldn't be called in the testing code");
    }

    @Override
    public void externalBootstrapOnly() {
        if (!this.isBootstrapOnly) {
            throw RuntimeAssertionError.unreachable("Shouldn't be called in the testing code");
        }
    }

    @Override
    public void externalSetAbortState() {
        throw RuntimeAssertionError.unreachable("Shouldn't be called in the testing code");
    }
}
