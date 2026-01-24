package org.sokybot.settings.security;

/**
 * Encryption service for sensitive credential fields.
 * Uses user-provided passphrase with AES-256 encryption.
 */
public interface ICredentialEncryptor {
    
    /**
     * Initialize with user passphrase (called on first access or unlock).
     * 
     * @param passphrase User-provided passphrase
     */
    void unlock(String passphrase);
    
    /**
     * Check if encryptor is unlocked.
     * 
     * @return true if unlocked and ready to encrypt/decrypt
     */
    boolean isUnlocked();
    
    /**
     * Lock and clear passphrase from memory.
     */
    void lock();
    
    /**
     * Encrypt plaintext string.
     * 
     * @param plaintext Plain text to encrypt
     * @return Encrypted ciphertext (Base64 encoded)
     */
    String encrypt(String plaintext);
    
    /**
     * Decrypt ciphertext string.
     * 
     * @param ciphertext Encrypted text (Base64 encoded)
     * @return Decrypted plaintext
     */
    String decrypt(String ciphertext);
}
