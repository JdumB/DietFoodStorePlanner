package com.example.dietplanner;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {

    private static final String AES_KEY = "12345678901234567890123456789012";

    public static String encrypt(String cleartext) {
        if (cleartext == null || cleartext.isEmpty()) return "";
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(cleartext.getBytes());
            return Base64.encodeToString(encrypted, Base64.DEFAULT).trim();
        } catch (Exception e) {
            return "Error";
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] decodedValue = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] decrypted = cipher.doFinal(decodedValue);
            return new String(decrypted);
        } catch (Exception e) {
            return null;
        }
    }
}