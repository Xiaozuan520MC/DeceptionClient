package com.easycaikuai.deceptionclient.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 字符串加解密工具 —— 运行时解密被混淆的字符串
 * 对应编译时的 ASM 字节码修改器
 */
public class Obfuscator {
    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = {
        0x4D, 0x65, 0x6F, 0x77, 0x4D, 0x65, 0x6F, 0x77,
        0x44, 0x65, 0x63, 0x65, 0x70, 0x74, 0x69, 0x6F
    };

    private static final Cipher cipher = initCipher();

    private static Cipher initCipher() {
        try {
            Cipher c = Cipher.getInstance(ALGORITHM);
            SecretKeySpec spec = new SecretKeySpec(KEY, ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, spec);
            return c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 运行时解密字符串（被 ASM 字节码替换的加密字符串调用此方法） */
    public static String decrypt(String encrypted) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            return encrypted;
        }
    }

    /** 编译时加密字符串（由 Gradle Task 调用） */
    public static String encrypt(String plain) {
        try {
            Cipher c = Cipher.getInstance(ALGORITHM);
            SecretKeySpec spec = new SecretKeySpec(KEY, ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, spec);
            byte[] encrypted = c.doFinal(plain.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
