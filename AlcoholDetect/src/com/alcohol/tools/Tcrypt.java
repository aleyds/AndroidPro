package com.alcohol.tools;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;

public class Tcrypt {
	
	private static final String mPBKDF2Salt = "1234567890abcdef";
	
	/*
	 * mPwd:输入迭代的数据
	 * interations:迭代次数
	 * */
	public static byte[] PBKDF2Encrypt(String mPwd, int interations) {
		char[] password = mPwd.toCharArray();
		byte[] salt_bytes;
		try {
			salt_bytes = mPBKDF2Salt.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return null;
		}
		int keylen = 16 * 8; //bits
		byte[] dk = null;
		PBEKeySpec spec = new PBEKeySpec(password, salt_bytes, interations, keylen);
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			SecretKey sk = skf.generateSecret(spec);
			dk = sk.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return dk;
	}
	
	/**
	 * Encrypt the byte array using fixed encryption algorithm
	 * (AES/CBC/PKCS5Padding).
	 * 
	 * @param value
	 *            The raw data.
	 * @param key
	 *            The encryption key.
	 * @return The encrypted data.
	 */
	@SuppressLint("TrulyRandom")
	private static byte[] AESEncrypt(byte[] value, byte[] key) {
		try {
			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);
			return cipher.doFinal(value);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Decrypt the data using fixed encryption algorithm (AES/CBC/PKCS5Padding).
	 * 
	 * @param value
	 *            The encrypted data.
	 * @param key
	 *            The decryption key.
	 * @return The decrypted data.
	 */
	private static  byte[]  AESDecrypt(byte[] value, byte[] key) {
		try {
			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, keySpec);
			byte[] result = cipher.doFinal(value);
			return result;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}
	

}
