package com.m2mkey.stcontrol;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.m2mkey.utils.M2MStringUtils;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;

public class M2MBLEMessage {
	private static final String TAG = "M2MBLEMessage";
	
	private static M2MBLEMessage Instance = null;
	public static M2MBLEMessage getInstance() {
		if(null == Instance) {
			Instance = new M2MBLEMessage();
		}
		return Instance;
	}
	
	private byte[] mMacSalt = M2MStringUtils.hexStringToByteArray("4d96c1c57dc79d9917af84d2d0eb9118");
	private byte[] mMsgSalt = M2MStringUtils.hexStringToByteArray("347125a6382be950e2ee814a6c321a66");
	private byte[] mAddKeySalt = M2MStringUtils.hexStringToByteArray("a4fce3a0078e47fdacfff151f3aac4c7");
	private String mPBKDFSalt = "4XIV9xUtD7WvV5Qf"; 
	private M2MBLEMessage() {
		
	}
	
	protected byte[] getMsgSalt() {
		return mMsgSalt;
	}

	private byte[] deriveKey(final byte[] nonce, final byte[] salt, final byte[] longTermKey) {
		if (nonce.length != 16 || longTermKey.length != 16) {
			Log.e(TAG, "Deriving key, but nonce or key is not 16 bytes!");
			return null;
		}

		byte[] input = new byte[16];
		// XOR nonce with MAC-salt
		for (int i = 0; i < 16; i++) {
			input[i] = (byte) (nonce[i] ^ salt[i]);
		}

		// encrypt with long-term key
		byte[] output = encrypt(input, longTermKey);

		return output;
	}

	public byte[] deriveNewMKey(final byte[] nonce, final byte[] longTermKey) {
		return deriveKey(nonce, mAddKeySalt, longTermKey);
	}

	protected byte[] calculateMac(final byte[] message, final byte[] macKey) {
		// encrypt with long-term key
		byte[] output = encrypt(message, macKey);
		
		// Return only first 4 bytes as MAC
		return Arrays.copyOfRange(output, 0, 4);
	}

	public byte[] calculateMac(final byte[] message, final byte[] nonce,
			final byte[] longTermKey) {
		if (message.length != 16 || nonce.length != 16
				|| longTermKey.length != 16) {
			Log.e(TAG, "Message, MAC, or MAC Key length incorrect!");
			return null;
		}

		return calculateMac(message, deriveKey(nonce, mMacSalt, longTermKey));
	}
	
	public byte[] computeMacKey(final byte[] nonce, final byte[] ltk) {
		return deriveKey(nonce, mMacSalt, ltk);
	}

	private boolean validateMac(final byte[] message, final byte[] mac, final byte[] macKey) {
		if (message.length != 16 || mac.length != 4 || macKey.length != 16) {
			Log.e(TAG, "Message, MAC, or MAC Key length incorrect!");
			return false;
		}

		byte[] calculatedMac = calculateMac(message, macKey);
		return Arrays.equals(mac, calculatedMac);
	}

	public boolean validateMac(final byte[] message, final byte[] mac,
			final byte[] nonce, final byte[] longTermKey) {

		return validateMac(message, mac, deriveKey(nonce, mMacSalt, longTermKey));
	}

	public byte[] decryptMessage(final byte[] message,
								final byte[] nonce, final byte[] longTermKey) {
		if (message.length != 16 || longTermKey.length != 16 || nonce.length != 16) {
			Log.e(TAG, "Message, nonce, or Long-Term Key length incorrect!");
			return null;
		}

		return decrypt(message, deriveKey(nonce, mMsgSalt, longTermKey));
	}

	public byte[] encryptMessage(final byte[] message,
			final byte[] nonce, final byte[] longTermKey) {
		if (message.length != 16 || longTermKey.length != 16
				|| nonce.length != 16) {
//			Log.d(TAG, "Message, nonce, or Long-Term Key length incorrect!");
			return null;
		}

		return encrypt(message, deriveKey(nonce, mMsgSalt, longTermKey));
	}
	
	public byte[] DKGen(String master) {
		char[] password = master.toCharArray();
		byte[] salt_bytes;
		try {
			salt_bytes = mPBKDFSalt.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return null;
		}
		int iterations = 1000;
		int keylen = 16 * 8; //bits
		byte[] dk = null;
		PBEKeySpec spec = new PBEKeySpec(password, salt_bytes, iterations, keylen);
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
	
	public byte[] computeMD5(byte[] data) {
		byte[] md5 = null;
		if(null == data) {
			return md5;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			md5 = digest.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			md5 = null;
		}
		return md5;
	}
	
	public String encryptmKey(String str_passwd,String str_plain_text) {
		String passwd = str_passwd; // 6 to 16 characters
		String plain_text =str_plain_text;
		
		try {
			byte[] key = passwd.getBytes("UTF-8");
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] key2 = md5.digest(key);
			SecretKeySpec sks = new SecretKeySpec(key2, "AES");
			
			// encryption
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, sks);
			byte[] encrypted = cipher.doFinal(plain_text.getBytes("UTF-8"));
			return (M2MStringUtils.byteArrayToHexString(encrypted));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
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
	
	public String decryptmKey(String str_passwd,String encrpted) {
		String passwd = str_passwd; // 6 to 16 characters
		try {
			byte[] key = passwd.getBytes("UTF-8");
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] key2 = md5.digest(key);
			SecretKeySpec sks = new SecretKeySpec(key2, "AES");
			Cipher decryp = Cipher.getInstance("AES/ECB/PKCS5Padding");
			decryp.init(Cipher.DECRYPT_MODE, sks);
			
			byte[] tmp = decryp.doFinal(M2MStringUtils.hexStringToByteArray(encrpted));
			String plain_text1=new String(tmp, "UTF-8");
			return plain_text1;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
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
	
	public String decryptPhoneNumber(String b64_str) {
		String phone = null;
		try {
			byte[] cipher_txt = Base64.decode(b64_str, Base64.URL_SAFE);
			SecretKeySpec sks = new SecretKeySpec(DKGen("m2mkey"), "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, sks);
			byte[] tmp = cipher.doFinal(cipher_txt);
			phone = new String(tmp, "UTF-8");
		} catch(Exception e) {
			e.printStackTrace();
			phone = null;
		}
		return phone;
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
	private byte[] encrypt(byte[] value, byte[] key) {
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
	private byte[] decrypt(byte[] value, byte[] key) {
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
