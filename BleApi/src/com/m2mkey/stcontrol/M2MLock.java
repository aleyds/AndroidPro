package com.m2mkey.stcontrol;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.m2mkey.utils.M2MStringUtils;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

public class M2MLock extends M2MBLEDevice {
	
	private String mFriendlyName = null;
	private int mLockType = TYPE_UNKNOWN;

	// Specific to ILock, not all M2MkeyBleDevice
	// Persists across connections
	protected int mPIN;
	protected byte[] mLTK;
	protected long mExpiry;
	protected byte[] mNonce;
	protected long mMsgCounter;
	protected long mLastMsgCounter;
	private boolean statusValid = false;
	
	public boolean mLocked;
	public boolean mMotorOn;
	public boolean mAlarmOn;
	public boolean mDoorOpened;
	public boolean mHomeMode;
	/**
	 * true means device time has been synchronized
	 */
	public boolean mRTCSynced;
	public int mLastBatteryLevel;
	/**
	 * 锁端软件版本号
	 */
	private int mLockAppVersion = 0;
	/**
	 * 锁端硬件版本号
	 */
	private int mLockHwVersion = 0;
	
	// Status byte bitfields
	private static final int PROPERTY_LOCK = 0x01 << 0;
	private static final int PROPERTY_DOOR = 0x01 << 1;
	private static final int PROPERTY_ALARM = 0x01 << 2;
	private static final int PROPERTY_MODE_AUTOLOCK_DISABLE = 0x01 << 3;
	private static final int PROPERTY_MODE_IKEY_ADD = 0x01 << 4;
	private static final int PROPERTY_MOTOR = 0x01 << 5;
	private static final int PROPERTY_RTCSYNC = 0x01 << 6;
	private static final int PROPERTY_MODE_IKEY_RUN = 0;
	
	/**
	 * PIN number of all user fingerprints
	 */
	public static final int FINGERPRINT_MKEY = 200;
	/**
	 * PIN number of all combinations
	 */
	public static final int COMBINATION_MKEY = 201;
	/**
	 * door is open when lock is locked
	 */
	public static final int ILLEGAL_UNLOCK = 127;
	/**
	 * try to connect to device using incorrect mKey or sKey for 3 times
	 */
	public static final int ILLEGAL_TRIES = 128;

	public M2MLock(String device_mac, String friendly_name,int type, int pin, String ltk) {
		super(device_mac);
		mFriendlyName = friendly_name;
		mLockType = type;
		mPIN = pin;
		if(ADMIN_MKEY_ID == mPIN) {
			mLTK = M2MBLEMessage.getInstance().DKGen(ltk);
		}
		else {
			mLTK = M2MStringUtils.hexStringToByteArray(ltk);
		}
		mMsgCounter = 0;
		mLastMsgCounter = 0;
		mExpiry = 0;
	}
	
	public M2MLock(String device_mac, String friendly_name, int type, int pin, byte[] ltk) {
		super(device_mac);
		mFriendlyName = friendly_name;
		mLockType = type;
		mPIN = pin;
		mLTK = ltk;
		mMsgCounter = 0;
		mLastMsgCounter = 0;
		mExpiry = 0;
	}
	
//	public M2MLock(String device_mac, String friendly_name, byte[] ltk) {
//		super(M2MLock.TYPE_SLOCK, device_mac, friendly_name);
//		this.mPIN = 1;
//		this.mLTK = ltk;
//		this.mMsgCounter = 0;
//		this.mLastMsgCounter = 0;
//		this.mExpiry = 0;
//	}
	
	public static String mKeyQREncode(String device_mac, String friendly_name, int type,
										int mkey_pin, String mkey_ltk, long expiry_in_seconds) {
		if(null == device_mac || null == friendly_name
				|| null == mkey_ltk || mkey_pin < 2 || mkey_pin > 21) {
			return null;
		}
		String key_id = String.valueOf(mkey_pin);
		String encode=friendly_name.concat("^")+device_mac.concat("^")+mkey_ltk.concat("^")+
				key_id.concat("^") + String.valueOf(expiry_in_seconds).concat("^")
				+ String.valueOf(type);
		try {
			return M2MStringUtils.byteArrayToHexString(encode.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static M2MLock mKeyQRDecode(String mkey_qr_string) {
		String result = null;
		try {
			byte[] tmp = M2MStringUtils.hexStringToByteArray(mkey_qr_string);
			result = new String(tmp, "UTF_8");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		Pattern p = Pattern.compile(".*"+"\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}"+".*");
		Matcher m = p.matcher(result);
		if(!m.matches()){
			return null;
		}
		String [] lock_info = result.split("\\^");
		if(6 != lock_info.length) {
			return null;
		}
		String friendly_name = lock_info[0];
		String lock_mac = lock_info[1];
		int lock_type = Integer.parseInt(lock_info[5]);
		int pin = Integer.parseInt(lock_info[3]);
		long timestmp_end = Long.parseLong(lock_info[4]);
		M2MLock lock = new M2MLock(lock_mac, friendly_name, lock_type, pin, lock_info[2]);
		lock.mExpiry = timestmp_end;
		return lock;
	}
	
	private static byte[] keyDerivePBKDF2(String master) {
		final String SALT = "4XIV9xUtD7WvV5Qf";
		char[] password = master.toCharArray();
		byte[] salt_bytes = SALT.getBytes();
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
	
	public static byte[] decryptLTK(String encrypted_ltk_hex) {
		String pass_phrase = "YykBtJ7uQh2JFLxa";
		SecretKeySpec sks = new SecretKeySpec(keyDerivePBKDF2(pass_phrase), "AES");
		Cipher cipher;
		byte[] decrypted = null;
		try {
			byte[] input = M2MStringUtils.hexStringToByteArray(encrypted_ltk_hex);
			cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, sks);
			decrypted = cipher.doFinal(input);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		if(null == decrypted) {
			return null;
		}
		else {
			return decrypted;
		}
	}
	
	public String getFriendlyName() {
		return mFriendlyName;
	}
	
	public void setFriendlyName(String name) {
		mFriendlyName = name;
	}
	
	public int getType() {
		return mLockType;
	}
	
	public void setType(int type) {
		mLockType = type;
	}
	
	public int getPIN() {
		return mPIN;
	}
	
	public String getLtkHexString() {
		return M2MStringUtils.byteArrayToHexString(mLTK);
	}
	
	public byte[] getLTK() {
		return mLTK;
	}
	
	public long getExpiryTime() {
		return mExpiry;
	}
	
	
	public int getmKeyCapacity() {
		if(M2MBLEDevice.TYPE_DLOCK1 == mLockType) {
			return 2;
		}
		else {
			return 20;
		}
	}
	
	public int getsKeyCapacity() {
		if(M2MBLEDevice.TYPE_DLOCK1 == mLockType) {
			return 59;
		}
		else if(M2MBLEDevice.TYPE_CLOCK2 == mLockType) {
			return 640;
		}
		else {
			return 20;
		}
	}
	
	/**
	 * 获取sKey配对超时时间
	 * @return
	 */
	public long getsKeyPairTimeout() {
		if(M2MBLEDevice.TYPE_ILOCK3 == mLockType) {
			return 30000;
		}
		else {
			return 60000;
		}
	}
	
	public int getLogCapacity() {
		if(M2MBLEDevice.TYPE_DLOCK1 == mLockType) {
			return 30;
		}
		else if(M2MBLEDevice.TYPE_CLOCK2 == mLockType) {
			return 512;
		}
		else {
			return 50;
		}
	}
	
	/**
	 * get the size of log data
	 * @return
	 */
	public int getLogSize() {
		if(M2MBLEDevice.TYPE_CLOCK2 == mLockType) {
			return 10;
		}
		else {
			return 9;
		}
	}
	
	/**
	 * 获取锁的电池容量
	 * @return
	 * 容量值，单位毫安（mA），若设备接电，则返回0
	 */
	public int getBatteryCapacity() {
		if(M2MBLEDevice.TYPE_CLOCK1 == mLockType || M2MBLEDevice.TYPE_CLOCK2 == mLockType) {
			return 0;
		}
		else if(M2MBLEDevice.TYPE_ILOCK3 == mLockType) {
			return 6500;
		}
		else {
			return 2000;
		}
	}
	/**
	 * 设置锁的软件版本
	 * @param app_ver
	 */
	public void setLockAppVersion(int app_ver) {
		mLockAppVersion = app_ver;
	}
	
	public void setLockAppVersion(String app_ver_str) {
		mLockAppVersion = Integer.valueOf(app_ver_str.substring(2), 10);
	}
	
	/**
	 * 将蓝牙数据转换为锁的软件版本号
	 * @param ble_data
	 * @return
	 * 锁的软件版本号，成功时值大于0，若转换失败，则返回0
	 */
	public static int convertBleDataToLockAppVersion(byte[] ble_data) {
		int app_version = 0;
		if(null != ble_data) {
			try {
				String tmp = new String(ble_data, "ASCII");
				app_version = Integer.valueOf(tmp.substring(2), 10);
			}
			catch (UnsupportedEncodingException e) {
				app_version = 0;
			}
			catch(IndexOutOfBoundsException e) {
				app_version = 0;
			}
		}
		return app_version;
	}
	
	public int getLockAppVersion() {
		return mLockAppVersion;
	}
	
	/**
	 * 设置锁的硬件版本号
	 * @param hw_ver
	 */
	public void setLockHwVersion(int hw_ver) {
		mLockHwVersion = hw_ver;
	}
	
	public static int convertBleDataToLockHwVersion(byte[] ble_data) {
		int hw_version = -1;
		if(null != ble_data) {
			try {
				String tmp = new String(ble_data, "ASCII").substring(2);
				hw_version = Integer.valueOf(tmp,16);
			}
			catch (UnsupportedEncodingException e) {
				hw_version = -1;
			}
			catch(IndexOutOfBoundsException e) {
				hw_version = -1;
			}
		}
		return hw_version;
	}
	
	/**
	 * 获取锁的硬件版本号
	 * @return
	 */
	public int getLockHwVersion(){
		if(mLockAppVersion <= 2) {
			mLockHwVersion = 0;
		}
		return mLockHwVersion;
	}

	protected void parseStatus(BluetoothGattCharacteristic characteristic) {
		// TODO refactor into M2Mkey message processing class

		// We use the BluetoothGattCharacteristic.setValue because it manages
		// the endian-ness

		// 2-bytes for message type
		final int msgType = characteristic.getIntValue(
				BluetoothGattCharacteristic.FORMAT_UINT16, M2MBLEController.OFFSET_TYPE);

		// 1-byte for battery level
		//final int batteryLevel = characteristic.getIntValue(
		//		BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_BATT_LEVEL);
		//Log.d(TAG, "Battery level: " + batteryLevel);
		mLastBatteryLevel = characteristic.getIntValue(
						BluetoothGattCharacteristic.FORMAT_UINT8, M2MBLEController.OFFSET_BATT_LEVEL);

		// 1-byte for status bits
		final int status = characteristic.getIntValue(
				BluetoothGattCharacteristic.FORMAT_UINT8, M2MBLEController.OFFSET_STATUS_BITS);

		// Unpack status bits
		mLocked = !((status & PROPERTY_LOCK) > 0);
		mDoorOpened = (status & PROPERTY_DOOR) > 0;
		
		
		
		mAlarmOn = (status & PROPERTY_ALARM) > 0;
		mHomeMode = !((status & PROPERTY_MODE_AUTOLOCK_DISABLE) > 0);
		mMotorOn = (status & PROPERTY_MOTOR) > 0;
		mRTCSynced = (status & PROPERTY_RTCSYNC) > 0;
		statusValid = true;
	}
	
	private static final int OFFSET_BATT_LEVEL = 4;
	private static final int OFFSET_STATUS_BITS = 5;
	public void parseStatus(byte[] data) {
		if((data == null) || (data.length < 5))
		{
			Log.e("debug", "Parse Data fail !!!!");
			return;
		}
		mLastBatteryLevel =  (data[OFFSET_BATT_LEVEL]&0xFF);
		
		int status = (data[OFFSET_STATUS_BITS]&0xFF);
		mLocked = (status & PROPERTY_LOCK) > 0;
		mDoorOpened = (status & PROPERTY_DOOR) > 0;
		mAlarmOn = (status & PROPERTY_ALARM) > 0;
		mHomeMode = !((status & PROPERTY_MODE_AUTOLOCK_DISABLE) > 0);
		mMotorOn = (status & PROPERTY_MOTOR) > 0;
		//mLock.RtcSyncOn = (status & ILock.PROPERTY_RTCSYNC) > 0;
		mRTCSynced = (status & PROPERTY_RTCSYNC) > 0;
		statusValid = true;
	}

	public boolean isStatusValid() {
		return statusValid;
	}
}
