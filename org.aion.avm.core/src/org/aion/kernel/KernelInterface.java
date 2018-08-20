package org.aion.kernel;

import org.aion.avm.core.util.ByteArrayWrapper;

import java.math.BigInteger;
import java.util.Map;


/**
 * Interface for accessing kernel features.
 */
public interface KernelInterface {

    /**
     * Sets the code of an account.
     *
     * @param address the account addres
     * @param code    the immortal code
     */
    void putCode(byte[] address, VersionedCode code);

    /**
     * Retrieves the code of an account.
     *
     * @param address the account address
     * @return the code of the account, or NULL if not exists.
     */
    VersionedCode getCode(byte[] address);

    /**
     * Put a key-value pair into the account's storage.
     *
     * @param address the account address
     * @param key     the storage key
     * @param value   the storage value
     */
    void putStorage(byte[] address, byte[] key, byte[] value);

    /**
     * Get the value that is mapped to the key, for the given account.
     *
     * @param address the account address
     * @param key     the storage key
     */
    byte[] getStorage(byte[] address, byte[] key);

    /**
     * Creates an account if not exist.
     *
     * @param address the account address
     */
    void createAccount(byte[] address);

    /**
     * Deletes an account.
     *
     * @param address the account address
     */
    void deleteAccount(byte[] address);

    /**
     * Returns whether an account exists.
     *
     * @param address the account address
     * @return
     */
    boolean isExists(byte[] address);

    /**
     * Returns the balance of an account.
     *
     * @param address the account address
     * @return
     */
    long getBalance(byte[] address);

    /**
     * Adds/removes the balance of an account.
     *
     * @param address the account address
     * @param delta   the change
     */
    void adjustBalance(byte[] address, long delta);

    /**
     * Returns the nonce of an account.
     *
     * @param address the account address
     * @return the nonce
     */
    long getNonce(byte[] address);

    /**
     * Increases the nonce of an account by 1.
     *
     * @param address the account address
     */
    void incrementNonce(byte[] address);


    /**
     * Returns all the storage entries of an account, for testing purpose only.
     */
    Map<ByteArrayWrapper, byte[]> getStorageEntries(byte[] address);
}
