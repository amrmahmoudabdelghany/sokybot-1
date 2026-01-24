package org.sokybot.settings.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.settings.security.ICredentialEncryptor;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256-GCM encryption implementation using PBKDF2 key derivation.
 */
@Component(service = ICredentialEncryptor.class)
public class AESCredentialEncryptor implements ICredentialEncryptor {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int ITERATION_COUNT = 65536;
    private static final int SALT_LENGTH = 16;
    
    private volatile SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public void unlock(String passphrase) {
        if (passphrase == null || passphrase.isEmpty()) {
            throw new IllegalArgumentException("Passphrase cannot be empty");
        }
        
        try {
            // Generate salt for key derivation
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);
            
            // Derive key from passphrase using PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to unlock encryptor", e);
        }
    }
    
    @Override
    public boolean isUnlocked() {
        return secretKey != null;
    }
    
    @Override
    public void lock() {
        secretKey = null;
    }
    
    @Override
    public String encrypt(String plaintext) {
        if (!isUnlocked()) {
            throw new IllegalStateException("Encryptor is locked. Call unlock() first.");
        }
        
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(IV_LENGTH + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);
            
            return Base64.getEncoder().encodeToString(byteBuffer.array());
            
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    @Override
    public String decrypt(String ciphertext) {
        if (!isUnlocked()) {
            throw new IllegalStateException("Encryptor is locked. Call unlock() first.");
        }
        
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            
            // Extract IV
            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);
            
            // Extract ciphertext
            byte[] ciphertextBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertextBytes);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertextBytes);
            return new String(plaintext, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
