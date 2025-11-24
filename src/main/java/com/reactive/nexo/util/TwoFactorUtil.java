package com.reactive.nexo.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.ByteBuffer;

public class TwoFactorUtil {

    private static final String HMAC_SHA1 = "HmacSHA1";

    public static String generateNewSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160 bits
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeAsString(bytes);
    }

    public static boolean validateCode(String secret, String code) {
        String expectedCode = getTOTPCode(secret);
        return expectedCode.equals(code);
    }
    
    public static String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        // We handle the DecoderException here if it occurs during base32 decode
        String hexKey = Hex.encodeHexString(bytes);
        return getGAuthCode(hexKey);
    }

    private static String getGAuthCode(String hexKey) {
        long time = System.currentTimeMillis() / 1000;
        long counter = time / 30; // Codes valid for 30 seconds (TOTP standard)
        return generatePaddedCode(hexKey, counter);
    }

    private static String generatePaddedCode(String hexKey, long counter) {
        String result = "";
        try {
            byte[] hash = hmacSha1(hexKey, counter);
            int offset = hash[hash.length - 1] & 0xf;
            long truncatedHash = 0;
            for (int i = 0; i < 4; i++) {
                truncatedHash <<= 8;
                truncatedHash |= hash[offset + i] & 0xFF;
            }
            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= 1000000;

            result = String.format("%06d", truncatedHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // *** MODIFIED METHOD TO HANDLE DecoderException ***
    private static byte[] hmacSha1(String key, long value) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] keyBytes;
        try {
            // Hex.decodeHex can throw DecoderException
            keyBytes = Hex.decodeHex(key); 
        } catch (DecoderException e) {
            // Wrap and rethrow as a RuntimeException, as this should never happen with 
            // valid hex strings generated internally by the class.
            throw new RuntimeException("Error decoding hex key", e);
        }

        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, HMAC_SHA1);
        Mac mac = Mac.getInstance(HMAC_SHA1);
        mac.init(signingKey);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        return mac.doFinal(buffer.array());
    }

    public static String getQRUrl(String userIdentifier, String secretKey, String issuer) {
        return "otpauth://totp/" + issuer + ":" + userIdentifier + "?secret=" + secretKey + "&issuer=" + issuer;
    }
}

