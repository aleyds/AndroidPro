package com.m2mkey.stcontrol;

import java.util.List;

import com.m2mkey.model.Combination;
import com.m2mkey.stcontrol.M2MBLEController.CommError;
import com.m2mkey.stcontrol.M2MBLEController.ConnState;
import com.m2mkey.stcontrol.M2MBLEController.WiFiConfigError;
import com.m2mkey.stcontrol.M2MBLEController.mKeyError;
import com.m2mkey.stcontrol.M2MBLEController.sKeyError;


public interface M2MBLECtrlCallback {
	
	/**
	 * callback to indicate that Bluetooth is disabled
	 */
	abstract public void onBluetoothDisabled();
	/**
	 * callback to indicate that the device cannot be scanned
	 */
	abstract public void onDeviceOutofRange();
	/**
	 * callback to indicate that the deivce is in DFU mode
	 */
	abstract public void onDeviceInDfuMode();
	/**
	 * callback to indicate that BLE controller is searching the device
	 */
	abstract public void onLockScanning();
	/**
	 * callback to indicate that BLE controller is connecting device
	 */
	abstract public void onLockConnecting();
	/**
	 * callback to indicate the results of connecting device
	 * @param state
	 */
	abstract public void onLockConnected(ConnState state);
	/**
	 * callback to indicate the errors during communication
	 * @param error
	 */
	abstract public void onCommunicationError(CommError error);
	abstract public void onLockConnTimeout();
	abstract public void onLockStateChanged();
	abstract public void onFactoryDataReset(boolean ok);
	abstract public void onNoActionDisconnect();
	public void onLockDisconnected();
	abstract public void onLogRead(List<M2MLog> log_list);
	abstract public void onLogReadTimeout();
	abstract public void onTimeSynced();
	abstract public void onMKeyReceived(int pin, String ltk);
	abstract public void onMKeyReclaimed(int pin);
	abstract public void onMKeyError(mKeyError error);
	abstract public void onMKeyReset();
	abstract public void onAdminPasswdModified();
	abstract public void onSKeyPairReady();
	abstract public void onSKeyPaired(int keyid, String romid);
	abstract public void onSKeyUnpaired(String romid);
	abstract public void onSKeyReset();
	abstract public void onSKeyError(sKeyError error);
	/**
	 * callback of adding fingerprint
	 * @param result
	 * 0 - operation succeeds
	 * 1 - operation fails
	 * @param code
	 * if result is 0, then code can be
	 * FINGER_RECORD_1ST - start first record
	 * FINGER_RECORD_2ND - start second record
	 * FINGER_RECORD_3RD - start third record
	 * or fingerprint ID ranging from 1 to 1000
	 * 
	 * if result is 1, then code can be
	 * FINGER_RECORD_BAD_QUALITY - bad image quality
	 * FINGER_RECORD_TIMEOUT - timeout
	 * FINGER_INTERNAL_ERR - internal error
	 * FINGER_RECORD_CONFILICT - the fingerprint already exists,
	 * code constants defined in M2MLockBLEControl
	 * @param data
	 * if code is FINGER_RECORD_CONFILICT, data is ID of the existed fingerprint,
	 * otherwise data is 0
	 */
	abstract public void onFingerprintAdd(int result, int code, int data);
	/**
	 * callback of deleting fingerprint
	 * @param result
	 * 0 - operation succeeds
	 * 1 - operation fails
	 * @param code
	 * if result is 0, code is ID of the removed fingerprint
	 * if result is 1, then code can be
	 * FINGER_DEL_INVALID_ID - invalid fingerprint ID
	 * FINGER_DEL_NOT_EXIST - the fingerprint does not exist
	 * FINGER_INTERNEL_ERR - internal error,
	 * code constants defined in M2MLockBLEControl
	 */
	abstract public void onFingerprintDel(int result, int code);
	/**
	 * callback of resetting fingerprint
	 * @param result
	 * 0 - operation succeeds
	 * 1 - operation fails
	 * @param code
	 * if result is 0, code is the total number of removed fingerprints
	 * if result is 1, then code can be
	 * FINGER_INTERNEL_ERR - internal error,
	 * code constants defined in M2MLockBLEControl
	 */
	abstract public void onFingerprintReset(int result, int code);
	/**
	 * 设置固定密码回调
	 * @param success
	 * 			成功值为{@code true}，否则值为{@code false}
	 */
	abstract public void onPermanentCombinationSet(boolean success);
	/**
	 * 重新生成一次性密码库回调
	 * @param success
	 * 			生成成功值为{@code true}，否则值为{@code false}
	 */
	abstract public void onOneTimeCombRepoGenerated(boolean success);
	/**
	 * 获取一次性密码库回调
	 * @param cache
	 * 			一次性密码列表，包含密码库中的所有密码
	 */
	abstract public void onOneTimeCombRepoReceived(List<Combination> cache);
	/**
	 * WiFi配置成功回调
	 */
	abstract public void onWiFiConfigured();
	/**
	 * WiFi配置出错的回调
	 * @param error
	 */
	abstract public void onWiFiConfigureError(WiFiConfigError error);
	/**
	 * WiFi状态变化的回调。目前主要是开/关
	 * @param on
	 * true - WiFi模块打开
	 * false - WiFi模块关闭
	 */
	abstract public void onWiFiModuleStateChanged(boolean on);
}
