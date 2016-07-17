package com.m2mkey.stcontrol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.m2mkey.model.Combination;
import com.m2mkey.utils.BluetoothUtil;
import com.m2mkey.utils.M2MStringUtils;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class M2MBLEController extends BLEServiceCallback {
	/* change1 */
	// BLE feedback message layout within the characteristic's value
	protected static final int OFFSET_COUNTER = 0;
	protected static final int OFFSET_TYPE = 2;
	protected static final int OFFSET_DATA = 4;
	protected static final int OFFSET_NEW_MKEY_ID = 4;
	protected static final int VERIFY_OLDPASSWORD_ID = 4;
	protected static final int VERIFY_NEWPASSWORD_ID = 4;
	protected static final int OFFSET_BATT_LEVEL = 4;
	protected static final int OFFSET_STATUS_BITS = 5;
	protected static final int OFFSET_KEY_ID = 4;
	protected static final int OFFSET_IKEY_MODE = 5;
	protected static final int OFFSET_IKEY_READ = 4;
	protected static final int OFFSET_LOG_READ = 4;
	protected static final int OFFSET_FINGER_RET = 4;
	protected static final int OFFSET_FINGER_CODE = 5;
	protected static final int OFFSET_FINGER_EXIST = 7;
	protected static final int OFFSET_PASSWD_RET = 4;
	protected static final int OFFSET_WIFI_RET_CODE = 4;
	protected static final int OFFSET_WIFI_DATA_SECTION_ID = 5;

	public enum CmdType {
		CMDTYPE_UNLOCK,
		CMDTYPE_OFFICEMODE,
		CMDTYPE_HOMEMODE,
		CMDTYPE_ALARMON,
		CMDTYPE_ALARMOFF,
		CMDTYPE_SYNCTIME,
		CMDTYPE_ILLEGAL_UNLOCK,
		CMDTYPE_ILLEGAL_TRIES,
		CMDTYPE_UNKNOWN
	};
	
	public enum ModeType {
		MODE_HOME,
		MODE_OFFICE
	}
	
	public enum mKeyError {
		MKEY_OUT_OF_USE,
		MKEY_CANNOT_RECLAIM,
		MKEY_INVALID_PASSWD,
		MKEY_INCORRECT_OLD_PASSWD,
		MKEY_FAILED_TO_CHANGE_PASSWD
	};
	
	public enum sKeyError {
		SKEY_OUT_OF_USE,
		SKEY_INVALID_ROMID,
		SKEY_FAILED_TO_PAIR,
		SKEY_FAILED_TO_UNPAIR,
		SKEY_FAILED_TO_RESET
	}
	
	public enum ConnState {
		CONN_OK,
		MKEY_ERROR,
		BLE_ERROR
	}
	
	public enum CommError {
		CMD_SENT_TOO_FAST,
		BLE_MSG_INCORRECT,
		MAC_INCORRECT,
		COUNTER_INCORRECT_FROM_DEVICE,
		COUNTER_INCORRECT_FROM_APP
	}
	
	public enum WiFiConfigError {
		/**
		 * 无效的WiFi SSID
		 */
		WIFI_SSID_INVALID,
		/**
		 * 无效的WiFi密码
		 */
		WIFI_PWD_INVALID,
		/**
		 * 无效的验证码
		 */
		VERIF_CODE_INVALID,
		/**
		 * WiFi SSID传输错误
		 */
		WIFI_SSID_FAIL,
		/**
		 * WiFi密码传输错误
		 */
		WIFI_PWD_FAIL,
		/**
		 * 验证码传输错误
		 */
		VERIF_CODE_FAIL,
		/**
		 * 电量低，开启WiFi模块失败
		 */
		WIFI_SWITCH_FAIL_LOWPOWER,
		/**
		 * 内部错误，开/关WiFi失败
		 */
		WIFI_SWITCH_FAIL_OTHER
	}
	
	public static final int FINGER_RECORD_1ST = 0xFFF1;
	public static final int FINGER_RECORD_2ND = 0xFFF2;
	public static final int FINGER_RECORD_3RD = 0xFFF3;
	public static final int FINGER_RECORD_BAD_QUALITY = 0x21;
	public static final int FINGER_RECORD_TIMEOUT = 0x23;
	public static final int FINGER_INTERNAL_ERR = 0x51;
	public static final int FINGER_RECORD_CONFILICT = 0x19;
	public static final int FINGER_DEL_INVALID_ID = 0x60;
	public static final int FINGER_DEL_NOT_EXIST = 0x13;
	
	// Send message types
	protected static final int MSG_GET_STATUS = 0;
//	protected static final int MSG_LOCK = 1;
	protected static final int MSG_UNLOCK = 2;
	protected static final int MSG_AUTOLOCK_DISABLE = 3;
	protected static final int MSG_AUTOLOCK_ENABLE = 4;
	protected static final int MSG_ALARM_ON = 5;
	protected static final int MSG_ALARM_OFF = 6;
	protected static final int MSG_IKEY_ADD_MODE = 7;
	protected static final int MSG_IKEY_RUN_MODE = 8;
//	protected static final int MSG_IKEY_READ_ROMID = 9;
	protected static final int MSG_IKEY_DELETE_ROMID = 10;
	protected static final int MSG_IKEY_DELETE_ALL = 11;
	protected static final int MSG_START_SESSION = 12;
	protected static final int MSG_MKEY_ADD = 13;
	protected static final int MSG_MKEY_DEL = 14;
	protected static final int MSG_LOG_READ = 12;
	protected static final int MSG_RTC_SYNC = 15;
	protected static final int MSG_LTK_VERIFY = 16;
	protected static final int MSG_LTK_WRITE = 17;
	protected static final int MSG_MKEY_DELALL = 18;
	protected static final int MSG_WIFI_SSID = 21;
	protected static final int MSG_WIFI_PWD = 22;
	protected static final int MSG_REMOTE_CONF_VERIF_CODE = 24;
	protected static final int MSG_FINGER_RECORD = 30;
	protected static final int MSG_FINGER_DELETE = 31;
	protected static final int MSG_FINGER_RESET = 32;
	protected static final int MSG_SET_PERMANENT_COMB = 35;
	protected static final int MSG_UPDATE_ONETIME_COMBREPO = 41;
	protected static final int MSG_GET_ONETIME_COMBREPO = 42;
	/**
	 * 开启WiFi模块
	 */
	protected static final int MSG_CTL_WIFI_ON = 37;
	/**
	 * 关闭WiFi模块
	 */
	protected static final int MSG_CTL_WIFI_OFF = 38;
	/**
	 * 获取WiFi模块状态
	 */
	protected static final int MSG_CTL_WIFI_STATE = 39;
	protected static final int MSG_ENTER_DFU = 40;
	
	/*
	 * RFID 操作指令
	 * */
	protected static final int MSG_RFID_CMD = 50;
//	protected static final int MSG_ILLEGAL = 1111;
//	protected static final int MSG_UNKNOWN = 1000;

	//		public static final int ILLEGAL_KEYID = 127;


	// Receive message types
	protected static final int RECV_MSG_STATUS = 0;
	protected static final int RECV_MSG_REED_SWITCH_CHANGE = 1;
	protected static final int RECV_MSG_LOCK_STATUS_CHANGE = 2;
	protected static final int RECV_MSG_BATTERY_MEASURE_CHANGE = 3;
	protected static final int RECV_MSG_ALARM_STATUS_CHANGE = 4;
	protected static final int RECV_MSG_MOTOR_STATUS_CHANGE = 5;
	protected static final int RECV_MSG_IKEY_MODE_CHANGE = 6;
	protected static final int RECV_MSG_IKEY_READ_RESPONSE = 7;
	//		public static final int RECV_MSG_LOG_READ_PREPARE_RESPONSE = 7; //removed
	protected static final int RECV_MSG_LOG_READ_RESPONSE = 8;

	protected static final int RECV_MSG_IKEY_DELETE_SUCCESS = 10;

	protected static final int RECV_MSG_CONNECTION_SETUP = 13;
	protected static final int RECV_MSG_MKEY_ADD_LTK = 14;
	protected static final int RECV_MSG_MKEY_ADD_SUCCESS = 15;
	protected static final int RECV_MSG_MKEY_DELETE_SUCCESS = 16;
	protected static final int RECV_MSG_LTK_VERIFY=17;
	protected static final int RECV_MSG_LTK_WRITE=18;
	protected static final int RECV_MSG_FACTORY_DATA_RESET = 20;
	protected static final int RECV_MSG_WIFI_SSID=22;
	protected static final int RECV_MSG_WIFI_PWD=23;
	protected static final int RECV_MSG_REMOTE_CONF_VERIF_CODE = 25;
	protected static final int RECV_MSG_FINGER_RECORD = 30;
	protected static final int RECV_MSG_FINGER_DELETE = 31;
	protected static final int RECV_MSG_FINGER_RESET = 32;
	protected static final int RECV_MSG_SET_PERMANENT_COMB = 35;
	protected static final int RECV_MSG_CTL_WIFI_ON = 37;
	protected static final int RECV_MSG_CTL_WIFI_OFF = 38;
	protected static final int RECV_MSG_CTL_WIFI_STATE = 39;
	protected static final int RECV_MSG_UPDATE_ONETIME_COMBREPO = 41;
	protected static final int RECV_MSG_GET_ONETIME_COMBREPO = 42;
	protected static final int RECV_MSG_RFID_CMD = 50;
	private static final int PROPERTY_MODE_IKEY_ADD = 0x01 << 4;
	
	private static final int IKEY_ROMID_BYTES = 8;
	
	// log
	protected int mLogCounter = 0;
	
	private final static String TAG = "M2MBLEController";
	
	// iLock
	protected M2MLock mILock;
	// Bluetooth LE Device
	private BLEService mBLEService;
	private boolean mDisconnectIntentionally = false;
	private static final int CONNECT_RETRIES = 2;
	private int mConnRetryRemaining = CONNECT_RETRIES;

	// status flags
	private boolean mBLEInit = false; // is BLE service initialized
	protected boolean mSessionStarted = false; // is the lock connected
	private boolean mIKeyAddMode = false; // distinguish ikey feedback as both ikey add and ikey read return msg 7

	// Keep track of state across multiple messages
	private boolean addingNewMKey = false;
	private int addingNewMKeyId = -1;
	
	/**
	 * store the communication error
	 */
	private CommError mCommError;

	// service and char references
//	private BluetoothGattService svcILock;
	private static BluetoothGattCharacteristic mCharNonce, mCharSessionStart, mCharCommand, mCharStatus, mCharAppVersion;
	private static BluetoothGattCharacteristic mCharHwVersion;
	
	// log cache for handling communication fails during log reading process
	protected List<M2MLog> mLogCache = null;
//	private int connectCount=0;
	
//	public static void ForceClear(){
//		sContext = null;
//		sLockController = null;
//	}
	
	private static M2MBLEController SingletonInstance = null;
	public static void initSingletonInstance(Context app_context) {
		SingletonInstance = new M2MBLEController(app_context);
	}
	
	public static M2MBLEController getSingletonInstance() {
		return SingletonInstance;
	}
	
	/**
	 * 指令管理器，用于保证指令的发送时序，具体如下：
	 * 1. 记录当前指令的状态
	 * 2. 判断下一条指令是否可以发送
	 */
	protected M2MCmdManager mCmdMgr = new M2MCmdManager();

	private Context mAppContext = null;
	/**
	 * create a BLE controller and initialize BLE service
	 * @param app_context
	 */
	public M2MBLEController(Context app_context) {
		mILock = null;
		mAppContext = app_context;
		if(!initBLE()) {
			Log.e(TAG, "failed to initialize BLE service");
		}
		
		mLogCache = new ArrayList<M2MLog>();
	}
	
	/**
	 * just create a BLE controller without initializing BLE service
	 */
	protected M2MBLEController() {
		mILock = null;
		mLogCache = new ArrayList<M2MLog>();
	}
	
	M2MBLECtrlCallback mBleCallback = null;
	public void setCallback(M2MBLECtrlCallback callback) {
		mBleCallback = callback;
	}
	
	M2MBLEScanner mScanner = null;
	public void setScanner(M2MBLEScanner scanner) {
		mScanner = scanner;
	}
	
	public M2MBLEScanner getScanner() {
		return mScanner;
	}
	
	private boolean initBLE() {
		if(null != mAppContext) {
			mBLEService = new BLEService(mAppContext);
			mBLEService.setCallback(this);
			mBLEInit = true;
		}
		else {
			mBLEService = null;
			mBLEInit = false;
		}
		return mBLEInit;
	}
	
	/*
	 * release BLE service
	 */
	public void release() {
		mBLEService.disconnect();
		mILock = null;
		mBLEService = null;
		mTimeoutHandler = null;
	}
	
	public void setLock(M2MLock lock) {
		this.mILock = lock;
	}
	
	public M2MLock getLock() {
		return this.mILock;
	}
	
	protected void setApplicationContext(Context context) {
		mAppContext = context;
	}
	
	protected Context getApplicationContext() {
		return mAppContext;
	}
	
	public long getConnTime() {
		return mConnEndTime - mConnStartTime;
	}
	
	/**
	 * check if BLE controller is connecting to a lock
	 * @return
	 * true - is connecting
	 * false - is not connecting
	 */
	public boolean isLockConnecting() {
		return mLockConnecting;
	}
	
	/**
	 * check if a lock is connected
	 * @return
	 * true - is connected
	 * false - is not connected
	 */
	public boolean isLockConnected() {
		return mSessionStarted;
	}

	/*
	 * establish a BLE connection with a sLock device
	 */
	protected boolean mLockConnecting = false;
	private long mConnStartTime = 0;
	private long mConnEndTime = 0;
	public void lockConnect() {
		// check bluetooth
		if(!BluetoothUtil.bluetoothEnabled(mAppContext)) {
			if(null != mBleCallback) {
				mBleCallback.onBluetoothDisabled();
			}
			return;
		}
		
		// check controller states
		if(null == mILock || mLockConnecting || mSessionStarted || !mBLEInit) {
			return;
		}
		mLockConnecting = true;
		if(mScanner.isDeviceDiscovered(mILock.getAddress())) {
			/*
			 * as Mutex is used to limit Bluetooth usage between controller and scanner, sometime this will lead to UI delay,
			 * to this delay, we use postDelay to invoke connect method
			 */
			new Handler().postDelayed(new Runnable() {

				@Override
				public void run() {
					bleConnect(); // connect to device
				}
				
			}, 100);
			if(null != mBleCallback) {
				mBleCallback.onLockConnecting();
			}
		}
		else {
			Thread t = new Thread(new Runnable() { // start new thread for scanning device

				@Override
				public void run() {
					long start_time = System.currentTimeMillis();
					boolean discovered = false;
					while(System.currentTimeMillis() - start_time < BLE_SCAN_TIMEOUT_THRESHOLD) {
						if(mScanner.isDeviceDiscovered(mILock.getAddress())) {
							discovered = true;
							break;
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if(discovered) {
						mTimeoutHandler.sendEmptyMessage(MSG_BLE_SCAN_SUCCESS);
					}
					else {
						mTimeoutHandler.sendEmptyMessage(MSG_BLE_SCAN_TIMEOUT);
					}
				}
				
			});
			t.start();
			if(null != mBleCallback) {
				mBleCallback.onLockScanning();
			}
		}
	}
	
	public void lockVerify() {
		if(null == mILock || mLockConnecting || mSessionStarted || !mBLEInit) {
			return;
		}
		
		mLockConnecting = true;
		if(null != mBleCallback) {
			mBleCallback.onLockConnecting();
		}
		bleConnect(); // connect to device
	}
	
	/*
	 * release the BLE connection with a sLock device
	 */
	public void lockDisconnect() {
		
		mDisconnectIntentionally = true;
		
		if(!mSessionStarted) {
			disconnect();
		}
		
		if(!mBLEService.disconnect()) {
			Log.w(TAG, "failed to disconnect from device");
			mBLEService.setCallback(new BLEServiceCallback() {

				@Override
				public void onGattServerConnected() {
				}

				@Override
				public void onGattServerConnecting() {
				}

				@Override
				public void onGattServerDisconnected() {
				}

				@Override
				public void onGattServerDisconnecting() {
				}

				@Override
				public void onGattServiceDiscovered(boolean discovered) {
				}

				@Override
				public void onGattReadAvailable(BluetoothGattCharacteristic characteristic) {
				}

				@Override
				public void onGattWriteSuccess(boolean success) {
				}

				@Override
				void onGattServerUnknownError(int status) {
				}
				
			});
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mBLEService = null;
			Log.w(TAG, "create a new BLE service");
			initBLE();
			disconnect();
		}
	}
	
	private static final int BLE_CMD_DATA_LEN = 12;
	/**
	 * request sLock states
	 */
	public void lockStateRequest() {
		if(mSessionStarted) {
			writeSecureCmd(MSG_GET_STATUS, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	/**
	 *  enable the motor to make the lock can be opened
	 */
	public void unLock() {
		if(mSessionStarted) {
			writeSecureCmd(MSG_UNLOCK, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	/**
	 * switch alarm on/off
	 * @param on
	 */
	public void alarmSwitch(boolean on) {
		if(!mSessionStarted) {
			return;
		}
		if (on) {
			writeSecureCmd(MSG_ALARM_ON, new byte[BLE_CMD_DATA_LEN]);
		} else {
			writeSecureCmd(MSG_ALARM_OFF, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	/**
	 * switch WiFi module on/off
	 * @param on
	 */
	public void wifiSwitch(boolean on) {
		if(!mSessionStarted) {
			return;
		}
		if(on) {
			writeSecureCmd(MSG_CTL_WIFI_ON, new byte[BLE_CMD_DATA_LEN]);
		}
		else {
			writeSecureCmd(MSG_CTL_WIFI_OFF, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	/**
	 * get on/off state of WiFi module
	 */
	public void wifiStateRequest() {
		if(!mSessionStarted) {
			return;
		}
		writeSecureCmd(MSG_CTL_WIFI_STATE, new byte[BLE_CMD_DATA_LEN]);
	}
	
	/**
	 *  switch mode family/home
	 *  @param mode
	 */
	public void modeSwitch(ModeType mode) {
		if(!mSessionStarted) {
			return;
		}
		switch(mode) {
		case MODE_HOME: //switch to home mode
			writeSecureCmd(MSG_AUTOLOCK_ENABLE, new byte[BLE_CMD_DATA_LEN]);
			break;
		case MODE_OFFICE: //switch to office mode 
			writeSecureCmd(MSG_AUTOLOCK_DISABLE, new byte[BLE_CMD_DATA_LEN]);
			break;
		default:
			break;
		}
	}
	
	/**
	 * read logs start from specific log ID
	 * @param start
	 */
	protected int mLogReadStart = 0;
	public void logRead(int start) {
		mLogReadTerminated = false;
		mLogCache.clear();
		if(mSessionStarted) {
			mLogReadStart = start;
			mLogCounter = mILock.getLogCapacity();
			bleLogRead(mLogCounter);
			resetReadLogTimer();
		}
	}
	
	public void logReadTerminate() {
		logReadStop();
	}
	
	/**
	 * synchronize time to sLock
	 * @param time_stamp_seconds
	 */
	private boolean mTimeSync = false;
	public void timeSync(long time_stamp_seconds, long time_zoo_offset) {
		if(mSessionStarted) {
			mTimeSync = true;
//			String local_time = Long.toHexString(time_stamp_seconds) + "0000000000000000";
			byte [] data_RTC_buffer = convertTimeStamp(time_stamp_seconds);
			byte [] data_RTC = new byte[BLE_CMD_DATA_LEN];
			for(int i = 0; i < 4; i++){
				data_RTC[i] = data_RTC_buffer[3 - i];
			}
			data_RTC[4] = (byte)time_zoo_offset;
			writeSecureCmd(MSG_RTC_SYNC, data_RTC);
		}
	}
	
	/**
	 * request a new mKey
	 * @param expiry_in_seconds
	 */
	public void mKeyRequest(long expiry_in_seconds) {
		if(mSessionStarted) {
			byte[] data1 = null;
			if(M2MLock.MKEY_NOEXPIRY == expiry_in_seconds) {
				data1 = new byte[BLE_CMD_DATA_LEN];
				for(int i=0; i<BLE_CMD_DATA_LEN; i++) {
					data1[i] = (byte)0xff;
				}
			}
			else {
//				String str_time=Long.toHexString(expiry_in_seconds)+"0000000000000000";//Long.toHexString(0xffffffff)+"0000000000000000";
//				byte[] data=M2MStringUtils.hexStringToByteArray(str_time);
				byte[] data = convertTimeStamp(expiry_in_seconds);
				data1 = new byte[BLE_CMD_DATA_LEN];
				for(int i = 0; i < 4; i++){
					data1[i] = data[3 - i];
				}
			}
			
			writeSecureCmd(MSG_MKEY_ADD, data1);
		}
	}
	
	private byte[] convertTimeStamp(long time) {
		String str_time = Long.toHexString(time) + "0000000000000000";
		return M2MStringUtils.hexStringToByteArray(str_time);
	}
	
	/**
	 *  reclaim a mKey
	 *  @param mkey_pin
	 */
	public void mKeyReclaim(int mkey_pin) {
		if(mSessionStarted) {
			byte bid = (byte)mkey_pin;
			byte[] data = new byte[BLE_CMD_DATA_LEN];
			data[0] = bid;
			writeSecureCmd(MSG_MKEY_DEL, data);
		}
	}
	
	/**
	 *  reset mkey on the lock
	 */
	public void mKeyReset(){
		if(mSessionStarted) {
			writeSecureCmd(MSG_MKEY_DELALL, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	/**
	 * change admin password
	 * @param old_passwd
	 * @param new_passwd
	 */
	protected String mNewAdminPasswd = null;
	public void adminPasswdModify(String old_passwd, String new_passwd) {
		if(mSessionStarted) {
			if(null == old_passwd || null == new_passwd
					|| old_passwd.isEmpty() || new_passwd.isEmpty()
					|| old_passwd.length() < 8 || old_passwd.length() > 16
					|| new_passwd.length() < 8 || new_passwd.length() > 16)
			{
				if(null != mBleCallback) {
					mBleCallback.onMKeyError(mKeyError.MKEY_INVALID_PASSWD);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
			else {
				mNewAdminPasswd = new_passwd;
				verifyAdminPasswd(M2MBLEMessage.getInstance().DKGen(old_passwd));
			}
		}
	}
	
	/**
	 *  send command to add a new sKey
	 */
	public void sKeyPair() {
		if(mSessionStarted) {
			writeSecureCmd(MSG_IKEY_ADD_MODE, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	/**
	 * cancel the operation of adding sKey
	 */
	public void sKeyPairCancel() {
		if(mSessionStarted) {
			writeSecureCmd(MSG_IKEY_RUN_MODE, new byte[BLE_CMD_DATA_LEN]);
		}
	}

	
	/**
	 *  unpair a sKey
	 *  @param romid
	 */
	private boolean mUnpairOrResetsKey = true; // true - unpair sKey, false - reset sKey
	private String mDelRomid = null;
	public void sKeyUnpair(String romid) {
		if(!mSessionStarted) {
			return;
		}
		if(null == romid || romid.isEmpty()) {
			if(null != mBleCallback) {
				mBleCallback.onSKeyError(sKeyError.SKEY_INVALID_ROMID);
			}
			else {
				Log.w(TAG, "Callback is null");
			}
			return;
		}
		byte[] rom_id = M2MStringUtils.hexStringToByteArray(romid);
		byte[] data = new byte[BLE_CMD_DATA_LEN];
		for(int i=0; i<rom_id.length; i++) {
			data[i] = rom_id[i];
		}
		mUnpairOrResetsKey = true;
		mDelRomid = romid;
		writeSecureCmd(MSG_IKEY_DELETE_ROMID, data);
	}
	
	/**
	 *  reset sKey on the lock
	 */
	public void sKeyReset(){
		if(mSessionStarted) {
			mUnpairOrResetsKey = false;
			writeSecureCmd(MSG_IKEY_DELETE_ALL, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	/**
	 * add new fingerprint
	 */
	public void fingerprintAdd() {
		if(mSessionStarted) {
			writeSecureCmd(MSG_FINGER_RECORD, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	/**
	 * delete specified fingerprint
	 * @param id - fingerprint ID
	 */
	public void fingerprintDelete(int id) {
		if(mSessionStarted) {
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(id);
			byte[] data = new byte[BLE_CMD_DATA_LEN];
			byte[] tmp = bb.array();
			data[0] = tmp[3];
			data[1] = tmp[2];
			writeSecureCmd(MSG_FINGER_DELETE, data);
		}
	}
	
	/**
	 * delete all fingerprints
	 */
	public void fingerprintReset() {
		if(mSessionStarted) {
			writeSecureCmd(MSG_FINGER_RESET, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	public void rfidAdd(){
		if(mSessionStarted){
			byte[] SendData = new byte[BLE_CMD_DATA_LEN];
			SendData[0] = 1;
			writeSecureCmd(MSG_RFID_CMD, SendData);
		}
	}
	
	public void rfidRun(){
		if(mSessionStarted){
			byte[] SendData = new byte[BLE_CMD_DATA_LEN];
			SendData[0] = 0;
			writeSecureCmd(MSG_RFID_CMD, SendData);
		}
	}
	
	public void rfidDelete(String _CardID, byte _Pos){
		if(!mSessionStarted) {
			return;
		}
		if(null == _CardID || _CardID.isEmpty()) {
//			if(null != mBleCallback) {
//				mBleCallback.onSKeyError(sKeyError.SKEY_INVALID_ROMID);
//			}
//			else {
//				Log.w(TAG, "Callback is null");
//			}
			return;
		}
		byte[] CardID = M2MStringUtils.hexStringToByteArray(_CardID);
		byte[] SendData = new byte[BLE_CMD_DATA_LEN];
		SendData[0] = 2;
		for(int i = 0; i < CardID.length; i++) {
			SendData[i+1] = CardID[i];
		}
		SendData[5] = _Pos;
//		mUnpairOrResetsKey = true;
//		mDelRomid = romid;
		writeSecureCmd(MSG_RFID_CMD, SendData);
	}
	
	public void rfidReset(){
		if(mSessionStarted){
			byte[] SendData = new byte[BLE_CMD_DATA_LEN];
			SendData[0] = 3;
			writeSecureCmd(MSG_RFID_CMD, SendData);
		}
	}
	
	/**
	 * 设置固定密码
	 * @param combination
	 * 			固定密码字符数组，例如“123456”，必须全为数字字符
	 * @param comb_size
	 * 			固定密码长度，目前固定密码长度为6
	 */
	public void setPermanentCombination(char[] combination, int comb_size) {
		if(mSessionStarted) {
			byte[] data = new byte[BLE_CMD_DATA_LEN];
			for(int i=0; i<comb_size; i++) {
				if(i % 2 == 0) {
					data[i] = (byte)combination[i + 1];
				}
				else {
					data[i] = (byte)combination[i - 1];
				}
			}
			writeSecureCmd(MSG_SET_PERMANENT_COMB, data);
		}
	}
	
	/**
	 * 存放一次性密码库
	 */
	private List<Combination> mCombCache = null;
	/**
	 * 一次性密码读取索引
	 */
	private int mRandPwdId = 1;
	/**
	 * 更新一次性密码，过程分为两个阶段，首先锁端重新生成密码库，然后，App从锁端获取密码库。
	 * 密码库生成完成时，回收到回调{@code onOneTimeCombRepoGenerated}。密码获取完成时会收到
	 * 回调{@code onOneTimeCombRepoReceived}
	 */
	public void updateOneTimeCombination() {
		if(mSessionStarted) {
			mCombCache = new ArrayList<Combination>();
			writeSecureCmd(MSG_UPDATE_ONETIME_COMBREPO, new byte[BLE_CMD_DATA_LEN]);
		}
	}
	
	/**
	 * 从锁端获取密码库
	 * @param idx - 索引，取值1-255
	 */
	private void getRandomPasswd(int idx) {
		if(mSessionStarted) {
			byte[] data = new byte[BLE_CMD_DATA_LEN];
			data[0] = (byte)idx;
			writeSecureCmd(MSG_GET_ONETIME_COMBREPO, data);
		}
	}
	
	/**
	 * 正在发送的WiFi配置数据
	 */
	private byte[] mSendingWiFiData = null;
	/**
	 * 还需要发送的WiFi配置数据
	 */
	private Map<String, Object> mRemainingWiFiConfData = null;
	private static final int WIFI_DATA_SECTION_LEN = 8;
	private static final int WIFI_DATA_MAX_LEN = 32;
	private static final int REMOTE_CONF_CODE_LEN = 6;
	private static final String MAPKEY_WIFI_PWD = "pwd";
	private static final String MAPKEY_REMOTE_CONF_CODE = "code";
	/**
	 * 配置设备WiFi，该方法适用于开放的WiFi热点
	 * @param ssid
	 * @param verif_code
	 */
	public void configureOpenWiFi(String ssid, String verif_code) {
		mSendingWiFiData = checkWiFiData(ssid);
		byte[] byte_code = checkRemoteConfCode(verif_code);
		if(null != mSendingWiFiData && null != byte_code) {
			mRemainingWiFiConfData = new HashMap<String, Object>();
			mRemainingWiFiConfData.put(MAPKEY_REMOTE_CONF_CODE, byte_code);
			sendWiFiSSID(mSendingWiFiData, 0);
		}
		else {
			if(null == mSendingWiFiData) {
				if(null != mBleCallback) {
					mBleCallback.onWiFiConfigureError(WiFiConfigError.WIFI_SSID_INVALID);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
			else {
				if(null != mBleCallback) {
					mBleCallback.onWiFiConfigureError(WiFiConfigError.VERIF_CODE_INVALID);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
		}
	}

	/**
	 * 配置设备WiFi，该方法适用于加密的WiFi热点，支持WEP和WPA加密类型
	 * @param ssid
	 * @param password
	 * @param verif_code
	 */
	public void configureSecureWiFi(String ssid, String password, String verif_code) {
		mSendingWiFiData = checkWiFiData(ssid);
		byte[] byte_pwd = checkWiFiData(password);
		byte[] byte_code = checkRemoteConfCode(verif_code);
		
		if(null != mSendingWiFiData && null != byte_pwd && null != byte_code) {
			mRemainingWiFiConfData = new HashMap<String, Object>();
			mRemainingWiFiConfData.put(MAPKEY_WIFI_PWD, byte_pwd);
			mRemainingWiFiConfData.put(MAPKEY_REMOTE_CONF_CODE, byte_code);
			sendWiFiSSID(mSendingWiFiData, 0);
		}
		else {
			if(null == mSendingWiFiData) {
				if(null != mBleCallback) {
					mBleCallback.onWiFiConfigureError(WiFiConfigError.WIFI_SSID_INVALID);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
			else if(null == byte_pwd) {
				if(null != mBleCallback) {
					mBleCallback.onWiFiConfigureError(WiFiConfigError.WIFI_PWD_INVALID);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
			else {
				if(null != mBleCallback) {
					mBleCallback.onWiFiConfigureError(WiFiConfigError.VERIF_CODE_INVALID);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
		}
	}
	
	/**
	 * 检查WiFi参数，若参数有效，则返回相应的byte数组，若参数无效，则返回null
	 * @param data
	 * @return
	 */
	private byte[] checkWiFiData(String data) {
		if(null == data || data.isEmpty()) {
			return null;
		}
		byte[] data_byte = null;
		try {
			data_byte = data.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if(null == data_byte || data_byte.length > WIFI_DATA_MAX_LEN) {
			return null;
		}
		else {
			return data_byte;
		}
	}
	
	/**
	 * 检查远程配置验证码，若验证码有效，则返回用于蓝牙指令数据的byte数组，若参数无效，则返回null
	 * @param code
	 * @return
	 */
	private byte[] checkRemoteConfCode(String code) {
		if(null == code || code.isEmpty()
				|| code.length() != REMOTE_CONF_CODE_LEN) {
			return null;
		}
		else {
			byte[] data = new byte[BLE_CMD_DATA_LEN];
			byte[] byte_code = null;
			try {
				byte_code = code.getBytes("ASCII");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if(null != byte_code) {
				for(int i=0; i<byte_code.length; i++) {
					data[i] = byte_code[i];
				}
			}
			return data;
		}
	}
	
	/**
	 * 发送WiFi SSID
	 * @param data
	 * @param section_id
	 * @return
	 * -1 - 数据无效
	 * 0 - 数据已全部发送
	 * 1 - 发送成功
	 */
	private int sendWiFiSSID(byte[] data, int section_id) {
		if(null == data || data.length <= 0 || section_id < 0) {
			return -1;
		}
		byte[] data_section = getWiFiPartialData(data, section_id, WIFI_DATA_SECTION_LEN);
		if(null == data_section) {
			return 0;
		}
		else {
			writeSecureCmd(MSG_WIFI_SSID, data_section);
			return 1;
		}
	}
	
	/**
	 * 发送WiFi密码
	 * @param data
	 * @param section_id
	 * @return
	 * -1 - 数据无效
	 * 0 - 数据已全部发送
	 * 1 - 发送成功
	 */
	private int sendWiFiPwd(byte[] data, int section_id) {
		if(null == data || data.length <= 0 || section_id < 0) {
			return -1;
		}
		byte[] data_section = getWiFiPartialData(data, section_id, WIFI_DATA_SECTION_LEN);
		if(null == data_section) {
			return 0;
		}
		else {
			writeSecureCmd(MSG_WIFI_PWD, data_section);
			return 1;
		}
	}
	
	/**
	 * 发送远程配置的验证码
	 * @param data
	 */
	private void sendRemoteConfVerifCode(byte[] data) {
		if(null == data || data.length <= 0) {
			return;
		}
		writeSecureCmd(MSG_REMOTE_CONF_VERIF_CODE, data);
	}
	
	/**
	 * 由于每次蓝牙指令只能发送8 bytes的数据，而WiFi的SSID和密码最大支持32 bytes，故通过该方法将数据分段（每段有一个ID和偏移） 。
	 * 分段后的数据通过蓝牙发送，最后设备端再重新组合
	 * @param data
	 * @param section_id
	 * @param section_len
	 * @return
	 * 用于蓝牙指令数据的分段，如果所有分段已完，则返回null
	 */
	private byte[] getWiFiPartialData(byte[] data, int section_id, int section_len) {
		byte[] data_section = null;
		if(null == data || section_id < 0 || section_len <=0) {
			return data_section;
		}
		
		// compute the total sections that the data can be divided into 
		int n = data.length / section_len;
		int total_sections = (data.length % section_len == 0) ? n : n + 1;
		
		if(section_id >= total_sections) {
			return data_section;
		}
		
		int offset = section_id * section_len;
		int actual_section_len = 0;
		if(data.length >= (section_len * (section_id + 1))) {
			actual_section_len = section_len;
		}
		else {
			actual_section_len = data.length - (section_len * section_id);
		}
		data_section = new byte[BLE_CMD_DATA_LEN];
		data_section[0] = (byte)(((section_id<<4)&0xf0)|(actual_section_len&0x0f));
		data_section[1] = (byte)offset;
		for(int i=0; i<actual_section_len; i++) {
			data_section[2 + i] = data[offset + i];
		}
		
		return data_section;
	}
	
	/**
	 * enter boot loader for DFU upgrade
	 */
	public void enterBootloader() {
		if(mSessionStarted) {
			writeSecureCmd(MSG_ENTER_DFU, new byte[12]);
		}
	}
	
	/*
	 *  verify old admin password
	 */
	private void verifyAdminPasswd(byte[]  oldData ){
		writeNonce(MSG_LTK_VERIFY,oldData);
		writeSecureCmd(MSG_LTK_VERIFY, new byte[12]);
	}
	
	/*
	 *  set new admin password
	 */
	protected void setAdminPasswd(byte[] newData){
		writeNonce(MSG_LTK_WRITE, newData);
		writeSecureCmd(MSG_LTK_WRITE, new byte[12]);
	}
	
	/*
	 *  stop the process of reading logs
	 */
	protected boolean mLogReadTerminated = false;
	private void logReadStop() {
		mLogCounter = 0;
		mLogReadTerminated = true;
	}
	
	protected void bleLogRead(int log_pointer) {
		byte blog_pointer = (byte)log_pointer;
		byte[] data = new byte[12];
		if(M2MBLEDevice.TYPE_CLOCK2 == mILock.getType()) {
			data[0] = (byte)(log_pointer >> 8);
			data[1] = (byte)(log_pointer & 0xFF);
		}
		else {
			data[0] = blog_pointer;
		}
		writeSecureCmd(MSG_LOG_READ, data);
	}
	
	private void bleConnect() {
		mSessionStarted = false;
		mILock.mMsgCounter = 0;
		mILock.mLastMsgCounter = 0;
		mConnStartTime = System.currentTimeMillis();
		// 检查设备是否处于DFU模式
		if(mScanner.isDeviceInDfuMode(mILock.getAddress())) {
			mLockConnecting = false;
			if(null != mBleCallback) {
				mBleCallback.onDeviceInDfuMode();
			}
			return;
		}
		// Connect to the device upon successful start-up
		// We want to directly connect to the device, so we are setting the
		// autoConnect parameter to false.
		if(mBLEService.connect(mAppContext, mILock.getAddress(), false)) {
			resetBleConnTimeoutTimer();
			mLockConnecting = true;
		}
		else {
			if(null != mBleCallback) {
				mBleCallback.onLockConnected(ConnState.BLE_ERROR);
			}
		}
	}
	
	private void disconnect() {
		stopBleConnTimeoutTimer();
		stopAutoDisconnTimer();
		stopReadLogTimer();
		mILock.mMsgCounter = 0;
		mILock.mLastMsgCounter = 0;
		mSessionStarted = false;
		mLockConnecting = false;
		
		if(null != mBleCallback) {
			mBleCallback.onLockDisconnected();
		}
		else {
			Log.w(TAG, "Callback is null");
		}
		
	}

	protected void writeSecureCmd(int type, byte[] data) {
		if (!mSessionStarted) {
			Log.w(TAG, "Not connected yet, discarding writeSecureCmd!");
			return;
		}
		// check if the command is sent too fast
		if(!mCmdMgr.isCmdSendable(type)) {
			if(null != mBleCallback) {
				mBleCallback.onCommunicationError(CommError.CMD_SENT_TOO_FAST);
			}
			else {
				Log.w(TAG, "Callback is null");
			}
		}
		else {
			insertCommand(mCharCommand, type);
			insertData(mCharCommand, data);
			if(encryptAndSignMessage(mCharCommand)) {
				resetAutoDisconnTimer();
				mBLEService.writeCharacteristic(mCharCommand);
				mCmdMgr.setCmd(type); // 指令管理器记录发送的指令
			}
			else {
				Log.w(TAG, "Failed to encrypt and sign BLE message");
				lockDisconnect();
			}
		}
	}
	
	private boolean encryptAndSignMessage(
			BluetoothGattCharacteristic characteristic) {

		// Insert counter
		// 2-bytes for message counter
		mILock.mMsgCounter++;
		mILock.mLastMsgCounter = mILock.mMsgCounter;

		if (!characteristic.setValue((int) mILock.mMsgCounter,
				BluetoothGattCharacteristic.FORMAT_UINT16, OFFSET_COUNTER)) {
			return false;
		}

		// Encrypt message
		byte[] plaintext = Arrays.copyOfRange(characteristic.getValue(), 0, 16);
		byte[] ciphertext = M2MBLEMessage.getInstance().encryptMessage(plaintext, mILock.mNonce, mILock.mLTK);
		
		if (ciphertext == null) {
			Log.e(TAG, "Unable to encrypt message to ciphertext!");
			return false;
		}
		
		// Calculate MAC
		byte[] mac = M2MBLEMessage.getInstance().calculateMac(ciphertext, mILock.mNonce, mILock.mLTK);
		if (mac == null) {
			Log.e(TAG, "Unable to calculate message MAC!");
			return false;
		}

		// Append MAC
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(ciphertext);
			outputStream.write(mac);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "Unable to concatenate ciphertext and MAC!");
			return false;
		}

		byte value[] = outputStream.toByteArray();

		// Put ciphertext+MAC back into characteristic
		if (!characteristic.setValue(value)) {
			Log.e(TAG, "Unable to set characteristic value to ciphertext+MAC!");
			return false;
		}

		return true;
	}
	
	private boolean encryptAndSignPasswdNonce(
			BluetoothGattCharacteristic characteristic) {

		// Insert counter
		// 2-bytes for message counter
//		mMsgCounter++;
//		lastSendCounter = mMsgCounter;
//
//		Log.d(TAG, "Creating message with message counter: " + mMsgCounter);
//		if (!characteristic.setValue(mMsgCounter,
//				BluetoothGattCharacteristic.FORMAT_UINT16, OFFSET_COUNTER)) {
//			Log.e(TAG, "Unable to set message counter!");
//			return false;
//		}

		// Encrypt message
		byte[] plaintext = Arrays.copyOfRange(characteristic.getValue(), 0, 16);
		byte[] ciphertext = M2MBLEMessage.getInstance().encryptMessage(plaintext, mILock.mNonce, mILock.mLTK);
		if (ciphertext == null) {
			Log.e(TAG, "Unable to encrypt message to ciphertext!");
			return false;
		}

		// Calculate MAC
		byte[] mac = M2MBLEMessage.getInstance().calculateMac(ciphertext, mILock.mNonce, mILock.mLTK);
		if (mac == null) {
			Log.e(TAG, "Unable to calculate message MAC!");
			return false;
		}

		// Append MAC
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(ciphertext);
			outputStream.write(mac);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "Unable to concatenate ciphertext and MAC!");
			return false;
		}

		byte value[] = outputStream.toByteArray();

		// Put ciphertext+MAC back into characteristic
		if (!characteristic.setValue(value)) {
			Log.e(TAG, "Unable to set characteristic value to ciphertext+MAC!");
			return false;
		}

		return true;
	}
	
	private boolean validateAndDecryptMessageIn(BluetoothGattCharacteristic characteristic) {
		if(characteristic.getValue().length != 20) { // invalid data
			mCommError = CommError.BLE_MSG_INCORRECT;
			return false;
		}

		byte[] ciphertext = Arrays
				.copyOfRange(characteristic.getValue(), 0, 16);
		byte[] mac = Arrays.copyOfRange(characteristic.getValue(), 16, 20);
		

		// Verify MAC
		boolean macValid = M2MBLEMessage.getInstance().validateMac(
								ciphertext, mac, mILock.mNonce, mILock.mLTK);

		if (!macValid) {
			mCommError = CommError.MAC_INCORRECT;
			return false;
		}

		// Decrypt message and put back into characteristic's value
		byte[] plaintext = M2MBLEMessage.getInstance().decryptMessage(
							ciphertext, mILock.mNonce, mILock.mLTK);
		characteristic.setValue(plaintext);

		// 2-bytes for message counter
		int msg_counter = characteristic.getIntValue(
							BluetoothGattCharacteristic.FORMAT_UINT16, OFFSET_COUNTER);
		if(msg_counter == (int)0xFFFF) {
			Log.w(TAG, "Counter invalid! Message counter: " + msg_counter);
			mCommError = CommError.COUNTER_INCORRECT_FROM_DEVICE;
			return false;
		}
		else {
		}

		// Check message counter
		if (msg_counter > mILock.mMsgCounter) {
			mILock.mMsgCounter = msg_counter;
			return true;
		}
		else {
			mCommError = CommError.COUNTER_INCORRECT_FROM_APP;
			Log.e(TAG, "msg counter is incorrect.");
			return false;
		}
	}
	
	private BluetoothGattCharacteristic insertCommand(BluetoothGattCharacteristic characteristic, int type) {
		// We use the BluetoothGattCharacteristic.setValue because it manages
		// the endian-ness

		// need to setup an empty array first so we can write into offsets
		characteristic.setValue(new byte[GattUUIDs.CHAR_MAXIMUM_BYTES]);
		if (!characteristic.setValue(type,
				BluetoothGattCharacteristic.FORMAT_UINT16, OFFSET_TYPE)) {
			Log.e(TAG, "Unable to set message type!");
		}

		return characteristic;
	}
	
	private BluetoothGattCharacteristic insertData(BluetoothGattCharacteristic characteristic, byte [] data){
		byte[] type = characteristic.getValue();
		byte [] type_data = type; 
		for(int i = 0; i < 12; i++){
			type_data[i + 4] = data[i];
		}
		if(!characteristic.setValue(type_data)){
			Log.e(TAG, "Unable to set message data!");
		}
		
		
		return characteristic;
	}
	
	private void writeNonce(int type,byte[] data){
		if (mBLEService.mConnectionState != BLEService.STATE_CONNECTED) {
			Log.i(TAG, "Not connected yet, discarding writeNonce!");
			return;
		}
		BluetoothGattService iLockService = mBLEService.getService(GattUUIDs.SVC_ILOCK_V2);
		BluetoothGattCharacteristic modify_password_nonce=iLockService
				.getCharacteristic(GattUUIDs.CHAR_NONCE);
		//mCharNonce.getValue();
		modify_password_nonce = insertCommand(modify_password_nonce, type);
		modify_password_nonce.setValue(data);
//		for(int i=0; i<16; i++) {
//			modify_password_nonce.setValue(data[i],
//					BluetoothGattCharacteristic.FORMAT_UINT8, i);
//		}
		
		//mILock.insertAllData(mCharNonce, new byte[16]);
		encryptAndSignPasswdNonce(modify_password_nonce);
		mBLEService.writeCharacteristic(modify_password_nonce);
	}
	
	private boolean encryptSession(BluetoothGattCharacteristic characteristic, int type, int pin) {
		byte Bytetype = (byte) type;
		byte BytePIN = (byte) pin;
		byte mnull = 0;
		
		byte[] plaintext = M2MBLEMessage.getInstance().getMsgSalt();
		byte[] ciphertext = M2MBLEMessage.getInstance().encryptMessage(plaintext, mILock.mNonce, mILock.mLTK);
		if (ciphertext == null) {
			Log.e(TAG, "Unable to encrypt message to ciphertext!");
			return false;
		}
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(Bytetype);
			outputStream.write(BytePIN);
			outputStream.write(mnull);
			outputStream.write(mnull);
			outputStream.write(ciphertext);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Unable to concatenate ciphertext!");
			return false;
		}

		byte value[] = outputStream.toByteArray();
		if (!characteristic.setValue(value)) {
			Log.e(TAG, "Unable to set characteristic value");
			return false;
		}
		return true;
	}

	private void handleCharacteristicRead(BluetoothGattCharacteristic characteristic) {
		// Check characteristic type
		UUID characteristicUUID = characteristic.getUuid();

		if (GattUUIDs.CHAR_NONCE.equals(characteristicUUID)) {
			// SESSION NONCE
			byte[] newNonce = characteristic.getValue();
			
			Log.i(TAG,
					"Got nonce: "
							+ M2MStringUtils.byteArrayToHexString(newNonce));

			if (!mSessionStarted) {
				// Remember this nonce for the duration of the session
				mILock.mNonce = newNonce;
				
				// read sLock hardware version
				getLockHwVersion();
				
				// Send message to start session char specifying which
				// long term key to use, and sign with MAC? TODO
//				mCharSessionStart.setValue(new byte[16]);
//				insertCommand(mCharSessionStart, MSG_START_SESSION);
//				mCharSessionStart.setValue(mILock.mPIN,
//						BluetoothGattCharacteristic.FORMAT_UINT8,
//						OFFSET_KEY_ID);
//				mBLEService.writeCharacteristic(mCharSessionStart);
			} else if (addingNewMKey) {
				// Derive the new LTK from the nonce and display mKey and mKey#
				// to user
				addingNewMKey = false;
				byte[] newMKeyNonce = newNonce;
				final byte[] newMKey = M2MBLEMessage.getInstance().deriveNewMKey(newMKeyNonce, mILock.mLTK);
				if(null != mBleCallback) {
					mBleCallback.onMKeyReceived(addingNewMKeyId, M2MStringUtils.byteArrayToHexString(newMKey));
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}

		} else if (GattUUIDs.CHAR_STATUS.equals(characteristicUUID)) {
			// STATUS (really all sorts of replies)
			handleStatusMessage(characteristic);
		} else if(GattUUIDs.INFO_SW_VERSION.equals(characteristicUUID)) { // get sLock app version
			mILock.setLockAppVersion(M2MLock.convertBleDataToLockAppVersion(mCharAppVersion.getValue()));
			Log.i(TAG, "get lock software version: " + mILock.getLockAppVersion());
			if(mILock.getLockAppVersion() > 1) {
				startSession2();
			}
			else {
				startSession1();
			}
		} else if(GattUUIDs.INFO_HW_VERSION.equals(characteristicUUID)){
			mILock.setLockHwVersion(M2MLock.convertBleDataToLockHwVersion(mCharHwVersion.getValue()));
			getLockAppVersion();
		}
	}

	private void handleStatusMessage(BluetoothGattCharacteristic characteristic) {
		if(mLockAppVersion > 1) { // need session check
			if(!sessionCheck(characteristic)) {
				// session check is failed (mKey is incorrect)
				Log.w(TAG, "incorrect mKey");
				lockDisconnect();
				if(null != mBleCallback) {
					mBleCallback.onLockConnected(ConnState.MKEY_ERROR);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
				return;
			}
		}
		if (validateAndDecryptMessageIn(characteristic)) {
			// Check message type
			final int msgType = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, OFFSET_TYPE);
			Log.i(TAG, "Received valid message of type: " + msgType);
			if (msgType == RECV_MSG_CONNECTION_SETUP) {
				Log.i(TAG, "Encrypted session successfully initiated");
				mSessionStarted = true;
				mLockConnecting = false;
				stopBleConnTimeoutTimer();
				resetAutoDisconnTimer();
				mCmdMgr.initCmdStatus(); //初始化指令状态，用于处理指令时序方面的问题
				if(null != mBleCallback) {
					mConnEndTime = System.currentTimeMillis();
					mBleCallback.onLockConnected(ConnState.CONN_OK);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			} else if (msgType == RECV_MSG_MKEY_ADD_SUCCESS) {
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				addingNewMKeyId = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_NEW_MKEY_ID);
				if(addingNewMKeyId == 255){
					if(null != mBleCallback) {
						mBleCallback.onMKeyError(mKeyError.MKEY_OUT_OF_USE);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}else{
//					Log.d(TAG,
//							String.format(
//									"BL600 added new mKey %d successfully, initiating read of nonce to derive key...",
//									addingNewMKeyId));
					if (mBLEService.readCharacteristic(mCharNonce)) {
						// so when we receive the new nonce, we know it's for mKey
						addingNewMKey = true;
						Log.i(TAG,
								"Reading nonce characteristic for adding new mKey...");
					} else {
						Log.i(TAG,
								"Couldn't init read of nonce characteristic for adding new mKey!");
					}
				}
			}else if((msgType == RECV_MSG_STATUS) 
					| (msgType == RECV_MSG_REED_SWITCH_CHANGE)
					| (msgType == RECV_MSG_BATTERY_MEASURE_CHANGE) 
					| (msgType == RECV_MSG_LOCK_STATUS_CHANGE)
					| (msgType ==  RECV_MSG_MOTOR_STATUS_CHANGE)
					| (msgType == RECV_MSG_ALARM_STATUS_CHANGE)){
				
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回

				mILock.parseStatus(characteristic);
				if (mILock.isStatusValid()) {
					if(!mTimeSync) {
						if(null != mBleCallback) {
							mBleCallback.onLockStateChanged();
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
					else {
						mTimeSync = false;
						if(null != mBleCallback) {
							mBleCallback.onTimeSynced();
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
				}
			}
			else if(RECV_MSG_IKEY_MODE_CHANGE == msgType) {
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				
				//check if sKey mode is add mode
				int mode = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_IKEY_MODE);
				if((PROPERTY_MODE_IKEY_ADD & mode) != 0) {
					mIKeyAddMode = true;
					if(null != mBleCallback) {
						mBleCallback.onSKeyPairReady();
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
				
			}
			else if(msgType == RECV_MSG_IKEY_READ_RESPONSE){
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				if(mIKeyAddMode) {
					// receive a new IKey
					byte[] data = new byte[M2MBLEController.IKEY_ROMID_BYTES];
					for(int i=0; i<M2MBLEController.IKEY_ROMID_BYTES; i++) {
						data[i] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
																OFFSET_IKEY_READ + i).byteValue();
					}
					
					int key_id;
					if(M2MBLEDevice.TYPE_CLOCK2 == mILock.getType()) {
						key_id = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,
																		OFFSET_IKEY_READ + M2MBLEController.IKEY_ROMID_BYTES);
					}
					else {
						key_id = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
																		OFFSET_IKEY_READ + M2MBLEController.IKEY_ROMID_BYTES);
					}
					Log.d("river", "key id: " + key_id);
					mIKeyAddMode = false;
					String romid = M2MStringUtils.byteArrayToHexString(data);
					if(romid.equals("0000000000000000")) {
						if(null != mBleCallback) {
							mBleCallback.onSKeyError(sKeyError.SKEY_FAILED_TO_PAIR);
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
					else if(romid.equals("FFFFFFFFFFFFFFFF")) {
						if(null != mBleCallback) {
							mBleCallback.onSKeyError(sKeyError.SKEY_OUT_OF_USE);
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
					else {
						if(null != mBleCallback) {
							mBleCallback.onSKeyPaired(key_id, romid);
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
				}
				else{
					// receive a old IKey
				}

			}
			else if(msgType == RECV_MSG_IKEY_DELETE_SUCCESS) {
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
//				int skey_id = characteristic.getIntValue(
//						BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_KEY_ID);
				byte[] data = new byte[IKEY_ROMID_BYTES];
				for(int i=0; i<IKEY_ROMID_BYTES; i++) {
					data[i] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
															OFFSET_IKEY_READ + i).byteValue();
				}
				String romid = M2MStringUtils.byteArrayToHexString(data);
				if(mUnpairOrResetsKey) { // unpair single sKey
					if(romid.equals(mDelRomid)) {
						if(null != mBleCallback) {
							mBleCallback.onSKeyUnpaired(romid);
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
					else {
						if(null != mBleCallback) {
							mBleCallback.onSKeyError(sKeyError.SKEY_FAILED_TO_UNPAIR);
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
				}
				else { // reset sKey
					if(romid.equals("0000000000000000")) {
						if(null != mBleCallback) {
							mBleCallback.onSKeyReset();
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
					else {
						if(null != mBleCallback) {
							mBleCallback.onSKeyError(sKeyError.SKEY_FAILED_TO_RESET);
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
				}
			}
			else if(msgType == RECV_MSG_LOG_READ_RESPONSE){
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				//log reading response
				byte[] log = new byte[mILock.getLogSize()];
				for(int i=0; i<mILock.getLogSize(); i++) {
					log[i] = characteristic.getIntValue(
							BluetoothGattCharacteristic.FORMAT_UINT8,
							4 + i).byteValue();
				}
				M2MLog slog = new M2MLog(log, mILock.getType());
				if(slog.mLogId > mLogReadStart) {
					mLogCache.add(slog);
				}
				if(mLogCounter > 1 && slog.mLogId > mLogReadStart + 1) {
					bleLogRead(--mLogCounter);
				}
				else
				{
					if(!mLogReadTerminated) {
						stopReadLogTimer();
						Collections.sort(mLogCache, new LogComparator());
						if(null != mBleCallback) {
							mBleCallback.onLogRead(mLogCache);
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
					else {
						if(null != mBleCallback) {
							mBleCallback.onLogReadTimeout();
						}
						else {
							Log.w(TAG, "Callback is null");
						}
					}
				}

			} else if(msgType == RECV_MSG_MKEY_DELETE_SUCCESS) {
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				int mkey_id = characteristic.getIntValue(
						BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_KEY_ID);
				if(255 == mkey_id) {
					if(null != mBleCallback) {
						mBleCallback.onMKeyError(mKeyError.MKEY_CANNOT_RECLAIM);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
				else if(0 == mkey_id) {
					if(null != mBleCallback) {
						mBleCallback.onMKeyReset();
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
				else {
					if(null != mBleCallback) {
						mBleCallback.onMKeyReclaimed(mkey_id);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
			}
			else if(msgType == RECV_MSG_FINGER_RECORD) {
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				int result = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,  OFFSET_FINGER_RET);
				int flag_code = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, OFFSET_FINGER_CODE);
				int exist_finger = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, OFFSET_FINGER_EXIST);
				if(null != mBleCallback) {
					mBleCallback.onFingerprintAdd(result, flag_code, exist_finger);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
			else if(msgType == RECV_MSG_FINGER_DELETE) {
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				int result = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_FINGER_RET);
				int flag_code = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, OFFSET_FINGER_CODE);
				if(null != mBleCallback) {
					mBleCallback.onFingerprintDel(result, flag_code);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
			else if(msgType == RECV_MSG_FINGER_RESET) {
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				int result = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_FINGER_RET);
				int flag_code = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, OFFSET_FINGER_CODE);
				if(null != mBleCallback) {
					mBleCallback.onFingerprintReset(result, flag_code);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
			else if(msgType == RECV_MSG_SET_PERMANENT_COMB) {
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				int result = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_PASSWD_RET);
				if(null != mBleCallback) {
					boolean success = false;
					if(0 == result) {
						success = true;
					}
					mBleCallback.onPermanentCombinationSet(success);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
			else if(msgType==RECV_MSG_LTK_VERIFY){
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				// response about old admin password verification
				int verifyOldPasswordId = characteristic.getIntValue(
						BluetoothGattCharacteristic.FORMAT_UINT8,
						VERIFY_OLDPASSWORD_ID);
				if(0 == verifyOldPasswordId) {
					setAdminPasswd(M2MBLEMessage.getInstance().DKGen(this.mNewAdminPasswd));
				}
				else {
					if(null != mBleCallback) {
						mBleCallback.onMKeyError(mKeyError.MKEY_INCORRECT_OLD_PASSWD);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
			}
			else if(msgType==RECV_MSG_LTK_WRITE){
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				//response about setting new admin password
				int new_passwd_id = characteristic.getIntValue(
												BluetoothGattCharacteristic.FORMAT_UINT8,
												VERIFY_NEWPASSWORD_ID);
				if(0 == new_passwd_id) {
					lockDisconnect();
					if(null != mBleCallback) {
						mBleCallback.onAdminPasswdModified();
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
				else {
					if(null != mBleCallback) {
						mBleCallback.onMKeyError(mKeyError.MKEY_FAILED_TO_CHANGE_PASSWD);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
			}
			else if(RECV_MSG_FACTORY_DATA_RESET == msgType) {
				mCmdMgr.setCmdGetFeedback(true); // 告诉指令管理器，已收到设备返回
				boolean ok = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_DATA) == 0 ? true : false;
				if(!ok) {
					Log.e(TAG, "Failed to reset factory data");
				}
				disconnect();
				if(null != mBleCallback) {
					mBleCallback.onFactoryDataReset(ok);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
			else if(RECV_MSG_WIFI_SSID == msgType) { // WiFi SSID数据接收的返回
				mCmdMgr.setCmdGetFeedback(true);
				int code = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_WIFI_RET_CODE);
				int received_section_id = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_WIFI_DATA_SECTION_ID);
				if(0xff == code) { // 数据接收失败
					if(null != mBleCallback) {
						mBleCallback.onWiFiConfigureError(WiFiConfigError.WIFI_SSID_FAIL);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
				else {
					int n = sendWiFiSSID(mSendingWiFiData, received_section_id + 1);
					if(0 == n) { // SSID数据已发送完，发送WiFi密码或远程配置验证码
						if(mRemainingWiFiConfData.containsKey(MAPKEY_WIFI_PWD)) {
							mSendingWiFiData = (byte [])mRemainingWiFiConfData.get(MAPKEY_WIFI_PWD);
							sendWiFiPwd(mSendingWiFiData, 0);
							Log.d("river", "发送password");
						}
						else {
							mSendingWiFiData = (byte [])mRemainingWiFiConfData.get(MAPKEY_REMOTE_CONF_CODE);
							sendRemoteConfVerifCode(mSendingWiFiData);
						}
					}
					else if(-1 == n) { // 数据无效，若出现这种情况，应为bug！！！
						
					}
					else {
						// SSID数据已发送给设备端，等待返回
						Log.d("river", "发送SSID");
					}
				}
			}
			else if(RECV_MSG_WIFI_PWD == msgType) { // WiFi密码数据接收返回
				mCmdMgr.setCmdGetFeedback(true);
				int code = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_WIFI_RET_CODE);
				int received_section_id = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_WIFI_DATA_SECTION_ID);
				if(0xff == code) { // 数据接收失败
					if(null != mBleCallback) {
						mBleCallback.onWiFiConfigureError(WiFiConfigError.WIFI_PWD_FAIL);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
				else {
					int n = sendWiFiPwd(mSendingWiFiData, received_section_id + 1);
					if(0 == n) { // 密码数据已发送完，发送远程配置验证码
						mSendingWiFiData = (byte [])mRemainingWiFiConfData.get(MAPKEY_REMOTE_CONF_CODE);
						sendRemoteConfVerifCode(mSendingWiFiData);
						Log.d("river", "发送验证码");
					}
					else if(-1 == n) { // 数据无效，若出现这种情况，应为bug！！！

					}
					else {
						// 密码数据已发送给设备端，等待返回
					}
				}
			}
			else if(RECV_MSG_REMOTE_CONF_VERIF_CODE == msgType) { // 远程配置验证码数据返回
				mCmdMgr.setCmdGetFeedback(true);
				int code = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_WIFI_RET_CODE);
				if(0xff == code) { // 数据接收失败， bug？
					if(null != mBleCallback) {
						mBleCallback.onWiFiConfigureError(WiFiConfigError.VERIF_CODE_FAIL);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
				else {
					if(null != mBleCallback) {
						mBleCallback.onWiFiConfigured();
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
			}
			else if(RECV_MSG_CTL_WIFI_ON == msgType
					|| RECV_MSG_CTL_WIFI_OFF == msgType) { // 开启/关闭WiFi模块返回
				mCmdMgr.setCmdGetFeedback(true);
				int code = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_DATA);
				if(0 == code) {
					if(null != mBleCallback) {
						boolean on = true;
						if(RECV_MSG_CTL_WIFI_OFF == msgType) {
							on = false;
						}
						mBleCallback.onWiFiModuleStateChanged(on);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
				else if(128 == code) {
					if(null != mBleCallback) {
						mBleCallback.onWiFiConfigureError(WiFiConfigError.WIFI_SWITCH_FAIL_LOWPOWER);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
				else {
					if(null != mBleCallback) {
						mBleCallback.onWiFiConfigureError(WiFiConfigError.WIFI_SWITCH_FAIL_OTHER);
					}
					else {
						Log.w(TAG, "Callback is null");
					}
				}
			}
			else if(RECV_MSG_CTL_WIFI_STATE == msgType) { // 请求WiFi模块状态返回
				mCmdMgr.setCmdGetFeedback(true);
				int state = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_DATA);
				if(null != mBleCallback) {
					boolean on = true;
					if(0 == state) {
						on = false;
					}
					mBleCallback.onWiFiModuleStateChanged(on);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
				
			}
			else if(RECV_MSG_UPDATE_ONETIME_COMBREPO == msgType) { // 重新生成随机密码返回
				mCmdMgr.setCmdGetFeedback(true);
				int ret = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_DATA);
				boolean success = false;
				if(ret == 0) {
					success = true;
					mRandPwdId = 1;
					mTimeoutHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							getRandomPasswd(mRandPwdId);
						}
						
					}, 5000);
				}
				else {
					Log.e(TAG, "重新生成随机密码库失败");
				}
				if(null != mBleCallback) {
					mBleCallback.onOneTimeCombRepoGenerated(success);
				}
			}
			else if(RECV_MSG_GET_ONETIME_COMBREPO == msgType) { // 获取随机密码返回
				mCmdMgr.setCmdGetFeedback(true);
				byte[] tmp = new byte[12];
//				String log = "passwd group: ";
				for(int i=0; i<12; i++) {
					tmp[i] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, OFFSET_DATA + i).byteValue();
					if((i+1) % 3 == 0) {
						int pwd = (int)(tmp[i-2] & 0xFF | (tmp[i-1] & 0xFF) << 8 | (tmp[i] & 0xFF) << 16);
//						log += pwd + "[" + mRandPwdId + "]" + ", ";
						if(mRandPwdId <= 255) {
							Combination comb = new Combination();
							comb.mId = mRandPwdId + 1;
							comb.mPwd = String.format("%06d", pwd);
							mCombCache.add(comb);
						}
						mRandPwdId++;
					}
				}
//				Log.i("debug", log);
				if(mRandPwdId > 255) {
					if(null != mBleCallback) {
						mBleCallback.onOneTimeCombRepoReceived(mCombCache);
					}
				}
				else {
					getRandomPasswd(mRandPwdId);
				}
			}
			else {
				Log.i(TAG, "Unknown message type received!");
			}
		} 
		else {
			if (!mSessionStarted) {
				Log.w(TAG, "Invalid message received while trying to setup session, wrong encryption key?");
//				disconnect();
				lockDisconnect(); // lock will not terminate the connection automatically if mKey is wrong
				if(null != mBleCallback) {
					mBleCallback.onLockConnected(ConnState.MKEY_ERROR);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			} else {
				Log.w(TAG, "Invalid message received during interaction, connection will be terminate");
				if(CommError.COUNTER_INCORRECT_FROM_DEVICE == mCommError) {
					disconnect();
				}
				else {
					lockDisconnect();
				}
				if(null != mBleCallback) {
					mBleCallback.onCommunicationError(mCommError);
				}
				else {
					Log.w(TAG, "Callback is null");
				}
			}
		}
	}
	
	private void getLockHwVersion(){
		BluetoothGattService info_service = mBLEService.getService(GattUUIDs.SVC_DEVICE_INFO);
		if(null != info_service) {
			mCharHwVersion = info_service.getCharacteristic(GattUUIDs.INFO_HW_VERSION);
			mBLEService.readCharacteristic(mCharHwVersion);
		}
		else {
			Log.e(TAG, "GATT service cannot read device info");
			disconnect();
		}
	}
	
	private void getLockAppVersion() {
		BluetoothGattService info_service = mBLEService.getService(GattUUIDs.SVC_DEVICE_INFO);
		if(null != info_service) {
			mCharAppVersion = info_service.getCharacteristic(GattUUIDs.INFO_SW_VERSION);
			mBLEService.readCharacteristic(mCharAppVersion);
		}
		else {
			Log.e(TAG, "GATT service cannot read device info");
			disconnect();
		}
	}
	
	private int mLockAppVersion = 1; // 1 - V0.1, 2 - V0.2
	private void startSession2() {
		mLockAppVersion = 2;
		if(encryptSession(mCharSessionStart, MSG_START_SESSION, mILock.mPIN)) {
			mBLEService.writeCharacteristic(mCharSessionStart);
		}
		else {
			Log.e(TAG, "failed to encyrpte session data, connection will be terminated");
			lockDisconnect();
		}
	}
	
	private void startSession1() {
		mLockAppVersion = 1;
		mCharSessionStart.setValue(new byte[16]);
		insertCommand(mCharSessionStart, MSG_START_SESSION);
		mCharSessionStart.setValue(mILock.mPIN,
				BluetoothGattCharacteristic.FORMAT_UINT8,
				OFFSET_KEY_ID);
		mBLEService.writeCharacteristic(mCharSessionStart);
	}
	
	private boolean sessionCheck(BluetoothGattCharacteristic characteristic)
	{
		byte[] value = characteristic.getValue();
		if(value != null)
		{
			String strData = new String(value);
			if(strData.equals("error1")){
				return false;
			}
			else if(strData.equals("error2")){
				return false;
			}
			else if(strData.equals("error3")){
				return false;
			}
			else if(strData.equals("error4")){
				return false;
			}
		}
		return true; //true: mKey is valid, false: mKey is invalid
	}
	
	/**
	 * 蓝牙连接超时阈值
	 */
	private final static long BLE_CONN_TIMEOUT_THRESHOLD = 10000;
	/**
	 * 设备扫描超时阈值
	 */
	private final static long BLE_SCAN_TIMEOUT_THRESHOLD = 10000;
	/**
	 * 蓝牙连接自动断开超时阈值，用户无任何操作时，蓝牙会自动断开
	 */
	private final static long BLE_AUTO_DISCONN_TIMEOUT_THRESHOLD = 175000;
	
	/**
	 * 蓝牙连接超时消息
	 */
	private final static int MSG_BLE_CONN_TIMEOUT = 0;
	/**
	 * 设备扫描超时消息
	 */
	private final static int MSG_BLE_SCAN_TIMEOUT = 1;
	/**
	 * 设备扫描成功消息
	 */
	private final static int MSG_BLE_SCAN_SUCCESS = 2;
	/**
	 * 读取日志超时
	 */
	private final static int MSG_LOG_READ_TIMEOUT = 3;
	/**
	 * 自动断开消息
	 */
	private final static int MSG_AUTO_DISCONN_TIMEOUT = 4;
	
	
	
	private Handler mTimeoutHandler = new TimeoutHandler(this);
	
	private static class TimeoutHandler extends Handler {
		WeakReference<M2MBLEController> mLockControllerRef;
		
		TimeoutHandler(M2MBLEController lock_controller) {
			mLockControllerRef = new WeakReference<M2MBLEController>(lock_controller);
		}
		
		public void handleMessage(Message msg) {
			if(null == mLockControllerRef.get()) {
				return;
			}
			M2MBLEController controller = mLockControllerRef.get();
			switch(msg.what) {
			case MSG_BLE_CONN_TIMEOUT:
				controller.disconnect();
				if(null != controller.mBleCallback) {
					controller.mBleCallback.onLockConnTimeout();
				}
				break;
			case MSG_BLE_SCAN_TIMEOUT:
				if(null != controller.mBleCallback) {
					controller.mLockConnecting = false;
					controller.mBleCallback.onDeviceOutofRange();
				}
				break;
			case MSG_BLE_SCAN_SUCCESS:
				controller.bleConnect(); // connect to sLock
				if(null != controller.mBleCallback) {
					controller.mBleCallback.onLockConnecting();
				}
				break;
			case MSG_LOG_READ_TIMEOUT:
				controller.logReadStop();
				if(null != controller.mBleCallback) {
					controller.mBleCallback.onLogReadTimeout();
				}
				break;
			case MSG_AUTO_DISCONN_TIMEOUT:
				Log.i(TAG, "no user interaction, automatically disconnect");
				controller.lockDisconnect();
				controller.mSessionStarted = false;
				if(null != controller.mBleCallback) {
					controller.mBleCallback.onNoActionDisconnect();
				}
				break;
			default:
				break;
			}
		}
	}
	
	private final static int TIMER_TIMEOUT = 0;
	
	private Runnable mBleConnTimeoutCallback = new Runnable() {
		@Override
		public void run() {
			if(null != mTimeoutHandler) {
				mTimeoutHandler.sendEmptyMessage(MSG_BLE_CONN_TIMEOUT);
			}
			
		}	
	};
	
	private Runnable mReadLogTimeoutCallback = new Runnable() {

		@Override
		public void run() {
			if(null != mTimeoutHandler) {
				mTimeoutHandler.sendEmptyMessage(TIMER_TIMEOUT);
			}
		}
		
	};
	
	private Runnable mAutoDisconnCallback = new Runnable() {
		@Override
		public void run() {
			mTimeoutHandler.sendEmptyMessage(TIMER_TIMEOUT);
		}
	};
	
	/**
	 * 重启蓝牙连接超时计时器
	 */
	private void resetBleConnTimeoutTimer() {
		if(null != mTimeoutHandler && null != mBleConnTimeoutCallback) {
			mTimeoutHandler.removeCallbacks(mBleConnTimeoutCallback);
			mTimeoutHandler.postDelayed(mBleConnTimeoutCallback, BLE_CONN_TIMEOUT_THRESHOLD);
		}
	}
	
	/**
	 * 关闭蓝牙连接超时计时器
	 */
	private void stopBleConnTimeoutTimer() {
		if(null != mTimeoutHandler && null != mBleConnTimeoutCallback) {
			mTimeoutHandler.removeCallbacks(mBleConnTimeoutCallback);
		}
	}
	
	/**
	 * 重启日志读取超时计时器
	 */
	private void resetReadLogTimer() {
		if(null != mTimeoutHandler && null != mReadLogTimeoutCallback) {
			mTimeoutHandler.removeCallbacks(mReadLogTimeoutCallback);
			long timeout = mILock.getLogCapacity() * 500; // compute timeout time according to log capacity
			mTimeoutHandler.postDelayed(mReadLogTimeoutCallback, timeout);
		}
	}
	
	/**
	 * 关闭日志读取超时计时器
	 */
	protected void stopReadLogTimer() {
		if(null != mTimeoutHandler && null != mReadLogTimeoutCallback) {
			mTimeoutHandler.removeCallbacks(mReadLogTimeoutCallback);
		}
	}
	
	private void resetAutoDisconnTimer(){
		if(null != mTimeoutHandler && null != mAutoDisconnCallback) {
			mTimeoutHandler.removeCallbacks(mAutoDisconnCallback);
			mTimeoutHandler.postDelayed(mAutoDisconnCallback, BLE_AUTO_DISCONN_TIMEOUT_THRESHOLD);
		}
	}
	private void stopAutoDisconnTimer(){
		if(null != mTimeoutHandler && null != mAutoDisconnCallback) {
			mTimeoutHandler.removeCallbacks(mAutoDisconnCallback);
		}
	}
	
	public class LogComparator implements Comparator<M2MLog> {

		@Override
		public int compare(M2MLog arg0, M2MLog arg1) {
			return arg0.mLogId - arg1.mLogId;
		}
		
	}
	
	private Handler mBLEServiceHandler = new BLEServiceHandler(this);
	private static class BLEServiceHandler extends Handler {
		WeakReference<M2MBLEController> mLockControllerRef;

		BLEServiceHandler(M2MBLEController lock_controller) {
			mLockControllerRef = new WeakReference<M2MBLEController>(lock_controller);
		}

		public void handleMessage(Message msg) {
			M2MBLEController controller = mLockControllerRef.get();
			if(null == controller) {
				return;
			}
			switch(msg.what) {
			case BLE_CB_GATT_SERVER_CONNECTED:
				controller.mDisconnectIntentionally = false;
				controller.mConnRetryRemaining = CONNECT_RETRIES;
				break;
			case BLE_CB_GATT_SERVER_DISCONNECTED:
				Log.w("debug", "BLE_CB_GATT_SERVER_DISCONNECTED");
				boolean is_connecting = controller.isLockConnecting();
				controller.disconnect();
				if(is_connecting) {
					if(null != controller.mBleCallback) {
						controller.mBleCallback.onLockConnected(ConnState.BLE_ERROR);
					}
				}
				break;
			case BLE_CB_GATT_SERVICE_DISCOVERED_OK:
				controller.onServiceDiscovered();
				break;
			case BLE_CB_GATT_SERVICE_DISCOVERED_FAIL:
				// 收到该消息后，GATT随后会返回BLE_CB_GATT_SERVER_DISCONNECTED消息，故不要在收到该消息时调用disconnect()方法
				Log.w("debug", "BLE_CB_GATT_SERVICE_DISCOVERED_FAIL");
				break;
			case BLE_CB_GATT_READ_AVAILABLE:
				controller.handleCharacteristicRead((BluetoothGattCharacteristic)msg.obj);
				break;
			case BLE_CB_GATT_READ_UNAVAILABLE:
				Log.w("debug", "BLE_CB_GATT_READ_UNAVAILABLE");
				controller.lockDisconnect();
				break;
			case BLE_CB_GATT_UNKNOWN_ERROR:
				// TODO 何时会收到该消息？收到该消息后，GATT Server是否会自动断开？
				Log.e("debug", "BLE_CB_GATT_UNKNOWN_ERROR");
//				if(null != controller.mBleCallback) {
//					controller.mBleCallback.onLockConnected(ConnState.BLE_ERROR);
//				}
				break;
			default:
				break;
			}
		}
	}

	private static final int BLE_CB_GATT_SERVER_CONNECTED = 0;
	@Override
	void onGattServerConnected() {
		mBLEServiceHandler.sendEmptyMessage(BLE_CB_GATT_SERVER_CONNECTED);
	}

	@Override
	void onGattServerConnecting() {
		// TODO Auto-generated method stub
		
	}

	private static final int BLE_CB_GATT_SERVER_DISCONNECTED = 1;
	@Override
	void onGattServerDisconnected() {
		mBLEServiceHandler.sendEmptyMessage(BLE_CB_GATT_SERVER_DISCONNECTED);
	}

	@Override
	void onGattServerDisconnecting() {
		// TODO Auto-generated method stub
		
	}

	private static final int BLE_CB_GATT_SERVICE_DISCOVERED_OK = 2;
	private static final int BLE_CB_GATT_SERVICE_DISCOVERED_FAIL = 3;
	private void onServiceDiscovered() {
		BluetoothGattService slock_service = mBLEService.getService(GattUUIDs.SVC_ILOCK_V2);
		if (slock_service != null) {
			//					Log.i(TAG, "Discovered services on this device, setting up slock_service's characteristics");

			// keep all the service and characteristic references
			// TODO change all references to these
			mCharNonce = slock_service.getCharacteristic(GattUUIDs.CHAR_NONCE);
			mCharStatus = slock_service.getCharacteristic(GattUUIDs.CHAR_STATUS);
			mCharCommand = slock_service.getCharacteristic(GattUUIDs.CHAR_SECURE_CMD);
			mCharSessionStart = slock_service.getCharacteristic(GattUUIDs.CHAR_SESSION_START);

			if (mCharStatus == null) {
				Log.e(TAG, "charStatus null");
			}
			if (mCharCommand == null) {
				Log.e(TAG, "charCommand null");
			}
			if (mCharSessionStart == null) {
				Log.e(TAG, "charSessionStart null");
			}
			if (mCharNonce == null) {
				Log.e(TAG, "charNonce null");
			}

			// characteristic for status feedback
			BluetoothGattCharacteristic characteristic = slock_service.getCharacteristic(GattUUIDs.CHAR_STATUS);

			if (characteristic != null) {

				// Enable characteristic notify or indicate in the
				// Android BLE stack
				mBLEService.setCharacteristicNotification(characteristic, true);

				// Write the descriptor to enable indication or
				// notification on the BLE device
				// Need to write descriptors first before doing any
				// reads or writes:
				// http://stackoverflow.com/a/18207869
				final int charaProp = characteristic.getProperties();
				BluetoothGattDescriptor desc = characteristic.getDescriptor(GattUUIDs.CLIENT_CHARACTERISTIC_CONFIG);

				if (desc != null) {
					if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
						desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					} else if ((charaProp | BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
						desc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
					}
					mBLEService.writeDescriptor(desc);
				}
				// Get the session nonce
				if (mBLEService.readCharacteristic(mCharNonce)) {
				} else {
					Log.e(TAG, "Couldn't init read of nonce characteristic!");
				}

			} else {
				Log.e(TAG, "Couldn't get slock_service CHAR_DOORSTATUS");

			}
		}
	}
	@Override
	void onGattServiceDiscovered(boolean discovered) {
		// Setup iLock services and characteristics
		if(discovered) {
			mBLEServiceHandler.sendEmptyMessage(BLE_CB_GATT_SERVICE_DISCOVERED_OK);
		}
		else {
			Log.e(TAG, "failed to discover GATT service");
			mBLEServiceHandler.sendEmptyMessage(BLE_CB_GATT_SERVICE_DISCOVERED_FAIL);
		}
		
	}

	private static final int BLE_CB_GATT_READ_AVAILABLE = 4;
	private static final int BLE_CB_GATT_READ_UNAVAILABLE = 5;
	@Override
	void onGattReadAvailable(BluetoothGattCharacteristic characteristic) {
		if (null != characteristic) {
//			Log.d("debug", "onGattReadAvailable");
			Message msg = new Message();
			msg.what = BLE_CB_GATT_READ_AVAILABLE;
			msg.obj = characteristic;
			mBLEServiceHandler.sendMessage(msg);
		} else {
			// CHARACTERITSIC MIGHT BE NULL IF MESSAGE RECEIVED
			// BEFORE SERVICE DISCOVERY COMPLETES
			Log.e(TAG, "Received ACTION_DATA_CHAR_READ_AVAILABLE on null characteristic, something's wrong!");
			mBLEServiceHandler.sendEmptyMessage(BLE_CB_GATT_READ_UNAVAILABLE);
		}
	}

	@Override
	void onGattWriteSuccess(boolean success) {
		// TODO Auto-generated method stub
		
	}

	private static final int BLE_CB_GATT_UNKNOWN_ERROR = 6;
	@Override
	void onGattServerUnknownError(int status) {
		Log.d(TAG, "GATT unkown error, status=" + status);
		mBLEServiceHandler.sendEmptyMessage(BLE_CB_GATT_UNKNOWN_ERROR);
	}

}
