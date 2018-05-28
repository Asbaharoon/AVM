package org.aion.avm.core.exceptionwrapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.aion.avm.core.TypeAwareClassWriter;
import org.aion.avm.core.classgeneration.CommonGenerators;
import org.aion.avm.core.classloading.AvmClassLoader;
import org.aion.avm.core.shadowing.ClassShadowing;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.internal.Helper;
import org.aion.avm.core.Forest;
import org.aion.avm.core.HierarchyTreeBuilder;
import org.aion.avm.core.SimpleRuntime;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;


public class ExceptionWrappingTest {
    private AvmClassLoader loader;
    private Class<?> testClass;

    @Before
    public void setup() throws Exception {
        TestHelpers.didUnwrap = false;
        TestHelpers.didWrap = false;
        
        // We know that we have an exception, in this test, but the forest normally needs to be populated from a jar so manually assemble it.
        String exceptionClassSlashName = TestExceptionResource.UserDefinedException.class.getName();
        Forest<String, byte[]> classHierarchy = new HierarchyTreeBuilder()
                .addClass(exceptionClassSlashName, "java.lang.Throwable", null)
                .addClass("org.aion.avm.core.exceptionwrapping.TestExceptionResource", "java.lang.Object", null)
                .asMutableForest();
        LazyWrappingTransformer transformer = new LazyWrappingTransformer(classHierarchy);
        
        String className = TestExceptionResource.class.getName();
        byte[] raw = Helpers.loadRequiredResourceAsBytes(className.replaceAll("\\.", "/") + ".class");
        transformer.transformClass(className, raw);
        
        String resourceName = className.replaceAll("\\.", "/") + "$UserDefinedException.class";
        String exceptionName = className + "$UserDefinedException";
        byte[] exceptionBytes = Helpers.loadRequiredResourceAsBytes(resourceName);
        transformer.transformClass(exceptionName, exceptionBytes);
        
        Map<String, byte[]> classes = new HashMap<>(CommonGenerators.generateExceptionShadowsAndWrappers());
        classes.putAll(transformer.getLateGeneratedClasses());
        
        this.loader = new AvmClassLoader(classes);
        Helper.setLateClassLoader(this.loader);
        
        this.testClass = this.loader.loadClass(className);
        
        // We don't really need the runtime but we do need the intern map initialized.
        Helper.setBlockchainRuntime(new SimpleRuntime(null, null, 0));
    }

    @After
    public void teardown() throws Exception {
        Helper.clearTestingState();
    }


    /**
     * Tests that a multi-catch, using only java/lang/* exception types, works correctly.
     */
    @Test
    public void testSimpleTryMultiCatchFinally() throws Exception {
        // We need to use reflection to call this, since the class was loaded by this other classloader.
        Method tryMultiCatchFinally = this.testClass.getMethod("tryMultiCatchFinally");
        
        // Create an array and make sure it is correct.
        Assert.assertFalse(TestHelpers.didUnwrap);
        int result = (Integer) tryMultiCatchFinally.invoke(null);
        Assert.assertTrue(TestHelpers.didUnwrap);
        Assert.assertEquals(3, result);
    }

    /**
     * Tests that a manually creating and throwing a java/lang/* exception type works correctly.
     */
    @Test
    public void testmSimpleManuallyThrowNull() throws Exception {
        // We need to use reflection to call this, since the class was loaded by this other classloader.
        Method manuallyThrowNull = this.testClass.getMethod("manuallyThrowNull");
        
        // Create an array and make sure it is correct.
        Assert.assertFalse(TestHelpers.didWrap);
        boolean didCatch = false;
        try {
            manuallyThrowNull.invoke(null);
        } catch (InvocationTargetException e) {
            // Make sure that this is the wrapper type that we normally expect to see.
            Class<?> compare = this.loader.loadClass("org.aion.avm.exceptionwrapper.java.lang.NullPointerException");
            didCatch = e.getCause().getClass() == compare;
        }
        Assert.assertTrue(TestHelpers.didWrap);
        Assert.assertTrue(didCatch);
    }

    /**
     * Tests that we can correctly interact with exceptions from the java/lang/* hierarchy from within the catch block.
     */
    @Test
    public void testSimpleTryMultiCatchInteraction() throws Exception {
        // We need to use reflection to call this, since the class was loaded by this other classloader.
        Method tryMultiCatchFinally = this.testClass.getMethod("tryMultiCatch");
        
        // Create an array and make sure it is correct.
        Assert.assertFalse(TestHelpers.didUnwrap);
        int result = (Integer) tryMultiCatchFinally.invoke(null);
        Assert.assertTrue(TestHelpers.didUnwrap);
        Assert.assertEquals(2, result);
    }

    /**
     * Tests that we can re-throw VM-generated exceptions and re-catch them.
     */
    @Test
    public void testRecatchCoreException() throws Exception {
        // We need to use reflection to call this, since the class was loaded by this other classloader.
        Method outerCatch = this.testClass.getMethod("outerCatch");
        
        // Create an array and make sure it is correct.
        Assert.assertFalse(TestHelpers.didUnwrap);
        int result = (Integer) outerCatch.invoke(null);
        Assert.assertTrue(TestHelpers.didUnwrap);
        // 3 here will imply that the exception table wasn't re-written (since it only caught at the top-level Throwable).
        Assert.assertEquals(2, result);
    }


    // Note that we will delegate to the common Helper class to ensure that we maintain overall correctness.
    public static class TestHelpers {
        public static final String CLASS_NAME = TestHelpers.class.getName().replaceAll("\\.", "/");
        public static int countWrappedClasses;
        public static int countWrappedStrings;
        public static boolean didUnwrap = false;
        public static boolean didWrap = false;
        
        public static <T> org.aion.avm.java.lang.Class<T> wrapAsClass(Class<T> input) {
            countWrappedClasses += 1;
            return Helper.wrapAsClass(input);
        }
        public static org.aion.avm.java.lang.String wrapAsString(String input) {
            countWrappedStrings += 1;
            return Helper.wrapAsString(input);
        }
        public static org.aion.avm.java.lang.Object unwrapThrowable(Throwable t) {
            didUnwrap = true;
            return Helper.unwrapThrowable(t);
        }
        public static Throwable wrapAsThrowable(org.aion.avm.java.lang.Object arg) {
            didWrap = true;
            return Helper.wrapAsThrowable(arg);
        }
    }


    /**
     * Allows us to build an incremental class loader, which is able to dynamically generate and load its own classes,
     * if it wishes to, such that we can request the final map of classes once everything has been loaded/generated.
     */
    private static class LazyWrappingTransformer {
        private final Forest<String, byte[]> classHierarchy;
        private final Map<String, byte[]> transformedClasses;
        
        public LazyWrappingTransformer(Forest<String, byte[]> classHierarchy) {
            this.classHierarchy = classHierarchy;
            this.transformedClasses = new HashMap<>();
        }
        
        public void transformClass(String name, byte[] inputBytes) {
            ClassReader in = new ClassReader(inputBytes);
            ClassWriter out = new TypeAwareClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            
            BiConsumer<String, byte[]> generatedClassesSink = (slashName, bytes) -> LazyWrappingTransformer.this.transformedClasses.put(slashName.replaceAll("/", "."), bytes); 
            ClassShadowing cs = new ClassShadowing(out, TestHelpers.CLASS_NAME);
            ExceptionWrapping wrapping = new ExceptionWrapping(cs, TestHelpers.CLASS_NAME, this.classHierarchy, generatedClassesSink);
            in.accept(wrapping, ClassReader.SKIP_DEBUG);
            
            byte[] transformed = out.toByteArray();
            this.transformedClasses.put(name, transformed);
        }
        
        public Map<String, byte[]> getLateGeneratedClasses() {
            return Collections.unmodifiableMap(this.transformedClasses);
        }
    }
}
