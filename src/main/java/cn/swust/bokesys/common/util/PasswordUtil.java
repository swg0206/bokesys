package cn.swust.bokesys.common.util;

import cn.swust.bokesys.common.exception.BusinessException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * 密码加盐哈希工具，对应《概要设计》第 2 章「安全性」与 5.2.1：
 * 管理员密码与注册用户密码均采用加盐哈希方式存储，不以明文落库。
 *
 * <p>采用 JDK 自带的 PBKDF2WithHmacSHA256，每个用户一份独立的 16 字节随机盐，
 * 无需引入 Spring Security（引入 starter 会让所有接口被默认登录页接管）。
 *
 * <p>存储格式：{@code pbkdf2$迭代次数$盐(Base64)$哈希(Base64)}，
 * 把参数随密文一起保存，将来调整迭代次数也不会让存量密码失效。
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int DEFAULT_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private PasswordUtil() {
    }

    /**
     * 把明文密码编码为可落库的哈希串。
     *
     * @param rawPassword 明文密码
     * @return 形如 {@code pbkdf2$120000$xxxx$yyyy} 的密文
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, DEFAULT_ITERATIONS);
        return PREFIX + "$" + DEFAULT_ITERATIONS
                + "$" + ENCODER.encodeToString(salt)
                + "$" + ENCODER.encodeToString(hash);
    }

    /**
     * 校验明文密码与存量密文是否匹配。
     *
     * <p>比较时使用常量时间比较，避免因为提前 return 而泄漏前缀信息。
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        int iterations;
        byte[] salt;
        byte[] expected;
        try {
            iterations = Integer.parseInt(parts[1]);
            salt = DECODER.decode(parts[2]);
            expected = DECODER.decode(parts[3]);
        } catch (RuntimeException e) {
            return false;
        }
        byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new BusinessException("密码加密失败：" + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }
}
