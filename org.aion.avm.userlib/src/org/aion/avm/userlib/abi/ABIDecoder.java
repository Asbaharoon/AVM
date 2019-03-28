package org.aion.avm.userlib.abi;

import java.util.List;
import org.aion.avm.api.Address;
import org.aion.avm.userlib.abi.ABICodec.Tuple;

/**
 * Utility class for AVM ABI decoding. This class contains static methods
 * for parsing transaction data and invoking corresponding methods.
 */
public class ABIDecoder {
    /**
     * Creates a new decoder, and sets the data field.
     * @param data Subsequent calls to functions like decodeAByte() will read from this byte array.
     */
    public ABIDecoder(byte[] data){
        this.data = data;
        this.position = 0;
    }

    private static final int BYTE_MASK = 0xff;
    private static final int BYTE_SIZE = Byte.SIZE;

    private byte[] data;
    private int position;

    private static List<ABICodec.Tuple> checkAndParse(byte[] txData) {
        if (null == txData) {
            throw new NullPointerException();
        }
        if (txData.length == 0) {
            return null;
        }
        List<ABICodec.Tuple> parsed = ABICodec.parseEverything(txData);

        if (parsed.size() < 1) {
            throw new ABIException("Decoded as " + parsed.size() + " elements");
        }
        if (String.class != parsed.get(0).type) {
            throw new ABIException("First parsed value not String (method name)");
        }
        return parsed;
    }

    /**
     * Decode the transaction data and return the method name.
     * @param txData the transaction data that has the encoded method name to call with.
     * @return the decoded method name.
     */
    public static String decodeMethodName(byte[] txData) {
        List<ABICodec.Tuple> parsed = checkAndParse(txData);
        if(null == parsed) {
            return null;
        }
        return (String) parsed.get(0).value;
    }

    /**
     * Decode a method name from the data field. If the decoding fails, we assume no methodName was supplied,
     * such as the balance transfer case.
     * @return the decoded method name.
     */
    public String decodeMethodName() {
        String methodName;
        if (null == data || 0 == data.length) {
            methodName = null;
        } else {
            methodName = decodeOneString();
        }
        return methodName;
    }

    /**
     * Decode the transaction data and return the argument list that is encoded in it.
     * @param txData the transaction data that has the encoded arguments descriptor and arguments.
     * @return an object array that contains all of the arguments.
     */
    public static Object[] decodeArguments(byte[] txData) {
        List<ABICodec.Tuple> parsed = checkAndParse(txData);
        if(null == parsed) {
            return null;
        }
        Object[] argValues = new Object[parsed.size() - 1];
        for (int i = 1; i < parsed.size(); ++i) {
            argValues[i - 1] = parsed.get(i).value;
        }
        return argValues;
    }

    /**
     * Decode the transaction data that has one object encoded in it.
     * @param txData the transaction data that has one object encoded in it (with the descriptor).
     * @return the decoded object.
     */
    public static Object decodeOneObject(byte[] txData) {
        if (null == txData) {
            throw new NullPointerException();
        }
        // We will handle an empty payload as a null.
        Object result = null;
        if (txData.length > 0) {
            List<Tuple> parsed = ABICodec.parseEverything(txData);
            if(1 != parsed.size()) {
                throw new ABIException("Expected exactly one object from this decode call");
            }
            result = parsed.get(0).value;
        }
        return result;
    }

    /**
     * Decode the transaction data, returning the entire list of deployment arguments it defines.
     *
     * @param txData The transaction data to decode as the deployment arguments, according to the ABI.
     * @return an object array that contains all of the deployment arguments.
     */
    public static Object[] decodeDeploymentArguments(byte[] txData) {
        if (null == txData) {
            throw new NullPointerException();
        }

        List<ABICodec.Tuple> parsed;

        parsed = ABICodec.parseEverything(txData);

        Object[] argValues = new Object[parsed.size()];
        for (int i = 0; i < parsed.size(); ++i) {
            argValues[i] = parsed.get(i).value;
        }
        return argValues;
    }

    private short getShort() {
        short s = (short) (data[position] << BYTE_SIZE);
        s |= (data[position + 1] & BYTE_MASK);
        position += Short.BYTES;
        return s;
    }

    private int getInt() {
        int i = data[position] << BYTE_SIZE;
        i = (i | (data[position + 1] & BYTE_MASK)) << BYTE_SIZE;
        i = (i | (data[position + 2] & BYTE_MASK)) << BYTE_SIZE;
        i |= (data[position + 3] & BYTE_MASK);
        position += Integer.BYTES;
        return i;
    }

    private long getLong() {
        long l = data[position] << BYTE_SIZE;
        l = (l | (data[position + 1] & BYTE_MASK)) << BYTE_SIZE;
        l = (l | (data[position + 2] & BYTE_MASK)) << BYTE_SIZE;
        l = (l | (data[position + 3] & BYTE_MASK)) << BYTE_SIZE;
        l = (l | (data[position + 4] & BYTE_MASK)) << BYTE_SIZE;
        l = (l | (data[position + 5] & BYTE_MASK)) << BYTE_SIZE;
        l = (l | (data[position + 6] & BYTE_MASK)) << BYTE_SIZE;
        l |= data[position + 7] & BYTE_MASK;
        position += Long.BYTES;
        return l;
    }

    private int getLength(int bytesPerElement) {
        if (data.length - position < Short.BYTES) {
            throw new ABIException("Data field does not have enough bytes left to read an array.");
        }

        int arrayLength = getShort();
        if (data.length - position < arrayLength * bytesPerElement) {
            throw new ABIException(
                "Data field does not have enough bytes left to read this array.");
        }

        return arrayLength;
    }

    /**
     * Decode a byte from the data field.
     * @return the decoded byte.
     */
    public byte decodeOneByte() {
        checkNullEmptyData();

        if (data.length - position < Byte.BYTES + 1) {
            throw new ABIException("Data field does not have enough bytes left to read a byte.");
        }
        if (data[position++] != ABIToken.BYTE.identifier) {
            throw new ABIException("Next element in data field is not a byte.");
        }
        return data[position++];
    }

    /**
     * Decode a boolean from the data field.
     * @return the decoded boolean.
     */
    public boolean decodeOneBoolean() {
        checkNullEmptyData();

        if (data.length - position < Byte.BYTES + 1) {
            throw new ABIException("Data field does not have enough bytes left to read a boolean.");
        }
        if (data[position++] != ABIToken.BOOLEAN.identifier) {
            throw new ABIException("Next element in data field is not a boolean.");
        }
        return data[position++] != 0;
    }

    /**
     * Decode a char from the data field.
     * @return the decoded char.
     */
    public char decodeOneCharacter() {
        checkNullEmptyData();

        if (data.length - position < Character.BYTES + 1) {
            throw new ABIException("Data field does not have enough bytes left to read a short.");
        }
        if (data[position++] != ABIToken.CHAR.identifier) {
            throw new ABIException("Next element in data field is not a short.");
        }
        return (char) getShort();
    }

    /**
     * Decode a short from the data field.
     * @return the decoded short.
     */
    public short decodeOneShort() {
        checkNullEmptyData();

        if (data.length - position < Short.BYTES + 1) {
            throw new ABIException("Data field does not have enough bytes left to read a short.");
        }
        if (data[position++] != ABIToken.SHORT.identifier) {
            throw new ABIException("Next element in data field is not a short.");
        }
        return getShort();
    }

    /**
     * Decode an integer from the data field.
     * @return the decoded integer.
     */
    public int decodeOneInteger() {
        checkNullEmptyData();

        if (data.length - position < Integer.BYTES + 1) {
            throw new ABIException("Data field does not have enough bytes left to read an integer.");
        }
        if (data[position++] != ABIToken.INT.identifier) {
            throw new ABIException("Next element in data field is not an integer.");
        }
        return getInt();
    }

    /**
     * Decode a long from the data field.
     * @return the decoded long.
     */
    public long decodeOneLong() {
        checkNullEmptyData();

        if (data.length - position < Long.BYTES + 1) {
            throw new ABIException("Data field does not have enough bytes left to read a long.");
        }
        if (data[position++] != ABIToken.LONG.identifier) {
            throw new ABIException("Next element in data field is not a long.");
        }
        return getLong();
    }

    /**
     * Decode a float from the data field.
     * @return the decoded float.
     */
    public float decodeOneFloat() {
        checkNullEmptyData();

        if (data.length - position < Float.BYTES + 1) {
            throw new ABIException("Data field does not have enough bytes left to read a float.");
        }
        if (data[position++] != ABIToken.FLOAT.identifier) {
            throw new ABIException("Next element in data field is not a float.");
        }
        return Float.intBitsToFloat(getInt());
    }

    /**
     * Decode a double from the data field.
     * @return the decoded double.
     */
    public double decodeOneDouble() {
        checkNullEmptyData();

        if (data.length - position < Double.BYTES + 1) {
            throw new ABIException("Data field does not have enough bytes left to read a double.");
        }
        if (data[position++] != ABIToken.DOUBLE.identifier) {
            throw new ABIException("Next element in data field is not a double.");
        }
        return Double.longBitsToDouble(getLong());
    }

    /**
     * Decode a byte array from the data field.
     * @return the decoded byte array.
     */
    public byte[] decodeOneByteArray() {
        checkNullEmptyData();
        checkMinLengthForObject();

        byte[] byteArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.A_BYTE.identifier) {
            position  += 2;
        } else {
            if (data[position++] != ABIToken.A_BYTE.identifier) {
                throw new ABIException("Next element in data field is not a byte array.");
            }

            int arrayLength = getLength(Byte.BYTES);

            byteArray = new byte[arrayLength];
            System.arraycopy(data, position, byteArray, 0, arrayLength);
            position += arrayLength;
        }
        return byteArray;
    }

    /**
     * Decode a boolean array from the data field.
     * @return the decoded boolean array.
     */
    public boolean[] decodeOneBooleanArray() {
        checkNullEmptyData();
        checkMinLengthForObject();

        boolean[] booleanArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.A_BOOLEAN.identifier) {
            position  += 2;
        } else {
            if (data[position++] != ABIToken.A_BOOLEAN.identifier) {
                throw new ABIException("Next element in data field is not a byte array.");
            }

            int arrayLength = getLength(Byte.BYTES);

            booleanArray = new boolean[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                booleanArray[i] = data[position++] != 0;
            }
        }
        return booleanArray;
    }

    /**
     * Decode a character array from the data field.
     * @return the decoded character array.
     */
    public char[] decodeOneCharacterArray() {
        checkNullEmptyData();
        checkMinLengthForObject();

        char[] characterArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.A_CHAR.identifier) {
            position  += 2;
        } else {
            if (data[position++] != ABIToken.A_CHAR.identifier) {
                throw new ABIException("Next element in data field is not a character array.");
            }

            int arrayLength = getLength(Character.BYTES);

            characterArray = new char[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                characterArray[i] = (char) getShort();
            }
        }
        return characterArray;
    }

    /**
     * Decode a short array from the data field.
     * @return the decoded short array.
     */
    public short[] decodeOneShortArray() {
        checkNullEmptyData();
        checkMinLengthForObject();

        short[] shortArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.A_SHORT.identifier) {
            position  += 2;
        } else {
            if (data[position++] != ABIToken.A_SHORT.identifier) {
                throw new ABIException("Next element in data field is not a short array.");
            }

            int arrayLength = getLength(Short.BYTES);

            shortArray = new short[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                shortArray[i] = getShort();
            }
        }
        return shortArray;
    }

    /**
     * Decode an integer array from the data field.
     * @return the decoded integer array.
     */
    public int[] decodeOneIntegerArray() {
        checkNullEmptyData();
        checkMinLengthForObject();

        int[] intArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.A_INT.identifier) {
            position  += 2;
        } else {
            if (data[position++] != ABIToken.A_INT.identifier) {
                throw new ABIException("Next element in data field is not an integer array.");
            }

            int arrayLength = getLength(Integer.BYTES);

            intArray = new int[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                intArray[i] = getInt();
            }
        }
        return intArray;
    }

    /**
     * Decode a long array from the data field.
     * @return the decoded long array.
     */
    public long[] decodeOneLongArray() {
        checkNullEmptyData();
        checkMinLengthForObject();

        long[] longArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.A_LONG.identifier) {
            position  += 2;
        } else {
            if (data[position++] != ABIToken.A_LONG.identifier) {
                throw new ABIException("Next element in data field is not a long array.");
            }

            int arrayLength = getLength(Long.BYTES);

            longArray = new long[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                longArray[i] = getLong();
            }
        }
        return longArray;
    }

    /**
     * Decode a float array from the data field.
     * @return the decoded float array.
     */
    public float[] decodeOneFloatArray() {
        checkNullEmptyData();
        checkMinLengthForObject();

        float[] floatArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.A_FLOAT.identifier) {
            position  += 2;
        } else {
            if (data[position++] != ABIToken.A_FLOAT.identifier) {
                throw new ABIException("Next element in data field is not a float array.");
            }

            int arrayLength = getLength(Float.BYTES);

            floatArray = new float[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                floatArray[i] = Float.intBitsToFloat(getInt());
            }
        }
        return floatArray;
    }

    /**
     * Decode a double array from the data field.
     * @return the decoded double array.
     */
    public double[] decodeOneDoubleArray() {
        checkNullEmptyData();
        checkMinLengthForObject();

        double[] doubleArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.A_DOUBLE.identifier) {
            position  += 2;
        } else {
            if (data[position++] != ABIToken.A_DOUBLE.identifier) {
                throw new ABIException("Next element in data field is not a double array.");
            }

            int arrayLength = getLength(Double.BYTES);

            doubleArray = new double[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                doubleArray[i] = Double.longBitsToDouble(getLong());
            }
        }
        return doubleArray;
    }

    /**
     * Decode a string from the data field.
     * @return the decoded string.
     */
    public String decodeOneString() {
        checkNullEmptyData();

        checkMinLengthForObject();

        String string = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.STRING.identifier) {
            position  += 2;
        } else {
            if (data[position++] != ABIToken.STRING.identifier) {
                throw new ABIException("Next element in data field is not a string.");
            }
            if (data.length - position < Short.BYTES) {
                throw new ABIException("Data field does not have enough bytes left to read a string.");
            }
            short stringLength = getShort();

            if (data.length - position < stringLength) {
                throw new ABIException(
                    "Data field does not have enough bytes left to read this string.");
            }

            byte[] stringBytes = new byte[stringLength];
            System.arraycopy(data, position, stringBytes, 0, stringLength);
            position += stringLength;
            string = new String(stringBytes);
        }
        return string;
    }

    /**
     * Decode an address from the data field.
     * @return the decoded address.
     */
    public Address decodeOneAddress() {
        checkNullEmptyData();

        checkMinLengthForObject();

        Address address;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ADDRESS.identifier) {
            position  += 2;
            address = null;
        } else {
            if (data[position++] != ABIToken.ADDRESS.identifier) {
                throw new ABIException("Next element in data field is not an address.");
            }

            if (data.length - position < Address.LENGTH) {
                throw new ABIException(
                    "Data field does not have enough bytes left to read an address.");
            }

            byte[] addressBytes = new byte[Address.LENGTH];
            System.arraycopy(data, position, addressBytes, 0, Address.LENGTH);
            position += Address.LENGTH;
            address = new Address(addressBytes);
        }
        return address;
    }

    /**
     * Decode a 2D byte array from the data field.
     * @return the decoded 2D byte array.
     */
    public byte[][] decodeOne2DByteArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        byte[][] byteArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.A_BYTE.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.A_BYTE.identifier) {
                throw new ABIException("Next element in data field is not a 2D byte array.");
            }

            // 2 bytes is the smallest a byte array can be, since null arrays are NULL followed by A_BYTE
            int arrayLength = getLength(2);

            byteArray = new byte[arrayLength][];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    byteArray[i] = decodeOneByteArray();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode a 2D byte array");
            }
        }
        return byteArray;
    }

    /**
     * Decode a 2D boolean array from the data field.
     * @return the decoded 2D boolean array.
     */
    public boolean[][] decodeOne2DBooleanArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        boolean[][] booleanArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.A_BOOLEAN.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.A_BOOLEAN.identifier) {
                throw new ABIException("Next element in data field is not a 2D boolean array.");
            }

            // 2 bytes is the smallest a boolean array can be, since null arrays are NULL followed by A_BOOLEAN
            int arrayLength = getLength(2);

            booleanArray = new boolean[arrayLength][];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    booleanArray[i] = decodeOneBooleanArray();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode a 2D boolean array");
            }
        }
        return booleanArray;
    }

    /**
     * Decode a 2D character array from the data field.
     * @return the decoded 2D character array.
     */
    public char[][] decodeOne2DCharacterArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        char[][] charArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.A_CHAR.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.A_CHAR.identifier) {
                throw new ABIException("Next element in data field is not a 2D character array.");
            }

            // 2 bytes is the smallest a character array can be, since null arrays are NULL followed by A_CHAR
            int arrayLength = getLength(2);

            charArray = new char[arrayLength][];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    charArray[i] = decodeOneCharacterArray();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode a 2D character array");
            }
        }
        return charArray;
    }

    /**
     * Decode a 2D short array from the data field.
     * @return the decoded 2D short array.
     */
    public short[][] decodeOne2DShortArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        short[][] shortArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.A_SHORT.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.A_SHORT.identifier) {
                throw new ABIException("Next element in data field is not a 2D short array.");
            }

            // 2 bytes is the smallest a short array can be, since null arrays are NULL followed by A_SHORT
            int arrayLength = getLength(2);

            shortArray = new short[arrayLength][];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    shortArray[i] = decodeOneShortArray();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode a 2D short array");
            }
        }
        return shortArray;
    }

    /**
     * Decode a 2D integer array from the data field.
     * @return the decoded 2D integer array.
     */
    public int[][] decodeOne2DIntegerArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        int[][] intArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.A_INT.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.A_INT.identifier) {
                throw new ABIException("Next element in data field is not a 2D integer array.");
            }

            // 2 bytes is the smallest an int array can be, since null arrays are NULL followed by A_INT
            int arrayLength = getLength(2);

            intArray = new int[arrayLength][];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    intArray[i] = decodeOneIntegerArray();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode a 2D integer array");
            }
        }
        return intArray;
    }

    /**
     * Decode a 2D long array from the data field.
     * @return the decoded 2D long array.
     */
    public long[][] decodeOne2DLongArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        long[][] longArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.A_LONG.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.A_LONG.identifier) {
                throw new ABIException("Next element in data field is not a 2D long array.");
            }

            // 2 bytes is the smallest a LONG array can be, since null arrays are NULL followed by A_LONG
            int arrayLength = getLength(2);

            longArray = new long[arrayLength][];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    longArray[i] = decodeOneLongArray();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode a 2D long array");
            }
        }
        return longArray;
    }

    /**
     * Decode a 2D float array from the data field.
     * @return the decoded 2D float array.
     */
    public float[][] decodeOne2DFloatArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        float[][] floatArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.A_FLOAT.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.A_FLOAT.identifier) {
                throw new ABIException("Next element in data field is not a 2D float array.");
            }

            // 2 bytes is the smallest a float array can be, since null arrays are NULL followed by A_FLOAT
            int arrayLength = getLength(2);

            floatArray = new float[arrayLength][];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    floatArray[i] = decodeOneFloatArray();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode a 2D float array");
            }
        }
        return floatArray;
    }

    /**
     * Decode a 2D double array from the data field.
     * @return the decoded 2D double array.
     */
    public double[][] decodeOne2DDoubleArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        double[][] doubleArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.A_DOUBLE.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.A_DOUBLE.identifier) {
                throw new ABIException("Next element in data field is not a 2D double array.");
            }

            // 2 bytes is the smallest a short array can be, since null arrays are NULL followed by A_DOUBLE
            int arrayLength = getLength(2);

            doubleArray = new double[arrayLength][];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    doubleArray[i] = decodeOneDoubleArray();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode a 2D short array");
            }
        }
        return doubleArray;
    }

    /**
     * Decode a string array from the data field.
     * @return the decoded string array.
     */
    public String[] decodeOneStringArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        String[] stringArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.STRING.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.STRING.identifier) {
                throw new ABIException("Next element in data field is not a string array.");
            }

            // 2 bytes is the smallest a string can be, since null arrays are NULL followed by STRING
            int arrayLength = getLength(2);

            stringArray = new String[arrayLength];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    stringArray[i] = decodeOneString();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode a string array");
            }
        }
        return stringArray;
    }

    /**
     * Decode an address array from the data field.
     * @return the decoded address array.
     */
    public Address[] decodeOneAddressArray() {
        checkNullEmptyData();
        checkMinLengthForObjectArray();

        Address[] addressArray = null;
        if (data[position] == ABIToken.NULL.identifier && data[position + 1] == ABIToken.ARRAY.identifier && data[position + 2] == ABIToken.ADDRESS.identifier) {
            position  += 3;
        } else {
            if (data[position++] != ABIToken.ARRAY.identifier || data[position++] != ABIToken.ADDRESS.identifier) {
                throw new ABIException("Next element in data field is not an address array.");
            }

            // 2 bytes is the smallest a string can be, since null arrays are NULL followed by ADDRESS
            int arrayLength = getLength(2);

            addressArray = new Address[arrayLength];
            try {
                for (int i = 0; i < arrayLength; i++) {
                    addressArray[i] = decodeOneAddress();
                }
            } catch (ABIException e) {
                throw new ABIException("Could not decode an address array");
            }
        }
        return addressArray;
    }

    private void checkNullEmptyData() {
        if (null == data || 0 == data.length) {
            throw new ABIException("Tried to decode from a null or empty data field.");
        }
    }

    private void checkMinLengthForObject() {
        if (data.length - position < 2) {
            throw new ABIException("Data field does not have enough bytes left to read an object.");
        }
    }

    private void checkMinLengthForObjectArray() {
        if (data.length - position < 3) {
            throw new ABIException("Data field does not have enough bytes left to read an object array.");
        }
    }
}
