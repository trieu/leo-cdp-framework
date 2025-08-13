package leotech.system.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import rfx.core.util.HashUtil;

/**
 * AES encryption
 * 
 * @author tantrieuf31
 * @since 2018
 *
 */
public class EncryptorAES {

	private static final String AES = "AES";
	private static final String UTF_8 = "UTF-8";
	static final String KEY = "Bar12345Bar12345"; // 128 bit key
	static final String INIT_VECTOR = "RandomInitVector"; // 16 bytes IV

	/**
	 * @param key
	 * @param initVector
	 * @param value
	 * @return
	 */
	public static String encrypt(String key, String initVector, String value) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(UTF_8));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(UTF_8), AES);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

			byte[] encrypted = cipher.doFinal(value.getBytes());
			return Base64.encodeBase64String(encrypted);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return "";
	}

	/**
	 * @param key
	 * @param initVector
	 * @param encrypted
	 * @return
	 */
	public static String decrypt(String key, String initVector, String encrypted) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(UTF_8));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(UTF_8), AES);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

			byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
			return new String(original);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return "";
	}

	/**
	 * @param value
	 * @return
	 */
	public static String encrypt(String value) {
		return encrypt(KEY, INIT_VECTOR, value);
	}

	/**
	 * @param value
	 * @return
	 */
	public static String decrypt(String value) {
		return decrypt(KEY, INIT_VECTOR, value);
	}

	/**
	 * @param userLogin
	 * @param userPass
	 * @return
	 */
	public static String passwordHash(String userLogin, String userPass) {
		return HashUtil.sha1(userLogin + userPass + KEY);
	}
	
	public static String passwordHash(String userLogin, String userPass, String keyHint) {
		String s = keyHint + "_" + userLogin + userPass;
		return HashUtil.sha1(s);
	}
	
}
