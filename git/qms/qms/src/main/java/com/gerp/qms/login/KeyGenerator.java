package com.gerp.qms.login;

import java.security.SecureRandom;
import java.util.Base64;

public class KeyGenerator {
    private static final int KEY_SIZE = 32; // 256 bits
    private String secretKey;

    public KeyGenerator() {
        this.secretKey = generateKey();
    }

    private String generateKey() {
        SecureRandom sr = new SecureRandom();
        byte[] key = new byte[KEY_SIZE];
        sr.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    public String getSecretKey() {
        return secretKey;
    }
}
