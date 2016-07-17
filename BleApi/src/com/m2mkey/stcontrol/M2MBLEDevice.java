package com.m2mkey.stcontrol;

import java.util.HashMap;
import java.util.Map;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class M2MBLEDevice {
	
	/**
	 * 管理员mKey PIN#
	 */
	public static final int ADMIN_MKEY_ID = 1;
	public static final String ADMIN_MKEY_COMMENT = "admin";
	/**
	 * mKey永久有效标识
	 */
	public static final long MKEY_NOEXPIRY = 0;
	
	/** Types of locks */
	public static final int TYPE_ALOCK1 = 1;//ALOCK--behind Lock
	public static final int TYPE_ILOCK = 0;//ILock,不区分sLock1和sLock2
	public static final int TYPE_ILOCK1 = 4;//ILOCK1--sLock1
	public static final int TYPE_ILOCK2 = 5;//ILOCK2--sLock2
	//public static final int TYPE_ILOCK3 = 2;//ILOCK3--auto sLock, no auto sLock
	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_DECRPT = -2;
	public static final int TYPE_ILOCK3 = 3; // ILOCK3--WiFi Lock
	public static final int TYPE_FLOCK1 = 6; // lock for finger&password
	public static final int TYPE_SBOX1 = 7; // lock for safe
	public static final int TYPE_CLOCK1 = 8; // lock for PM
	public static final int TYPE_DLOCK1 = 9; // sLock-1 for Guizhou university
	public static final int TYPE_CLOCK2 = 10; // sLock-3M for Guizhou university
	
	private static final String MODEL_ALOCK1 = "aLock-1";
	private static final String MODEL_CLOCK1 = "cLock-1";
	private static final String MODEL_CLOCK2 = "cLock-2";
	private static final String MODEL_DLOCK1 = "dLock-1";
	private static final String MODEL_SLOCK1 = "sLock-1";
	private static final String MODEL_SLOCK2 = "sLock-2";
	private static final String MODEL_SLOCK3 = "sLock-3";
	private static final String MODEL_FLOCK1 = "fLock-1";
	private static final String MODEL_SBOX1 = "sBox-1";
	private static final String MODEL_DECRPT = "Encrypted-M2Mkey-Device";
	
	protected static final String MODEL_UNKNOWN = "Unknown-Device";
	protected static final String DFU_MODE_TAG = "DfuTarg";
	
	public static final Map<String, Integer> DeviceModels;
	static
	{
		DeviceModels = new HashMap<String, Integer>();
		DeviceModels.put(MODEL_ALOCK1, TYPE_ALOCK1);
		DeviceModels.put(MODEL_CLOCK1, TYPE_CLOCK1);
		DeviceModels.put(MODEL_CLOCK2, TYPE_CLOCK2);
		DeviceModels.put(MODEL_DLOCK1, TYPE_DLOCK1);
		DeviceModels.put(MODEL_SLOCK1, TYPE_ILOCK1);
		DeviceModels.put(MODEL_SLOCK2, TYPE_ILOCK2);
		DeviceModels.put(MODEL_SLOCK3, TYPE_ILOCK3);
		DeviceModels.put(MODEL_FLOCK1, TYPE_FLOCK1);
		DeviceModels.put(MODEL_SBOX1, TYPE_SBOX1);
		DeviceModels.put(MODEL_DECRPT, TYPE_DECRPT);
		DeviceModels.put(MODEL_UNKNOWN, TYPE_UNKNOWN);
	}

	/** Bluetooth LE properties */
	private BluetoothDevice mDevice = null;
	private int mRssi = 0;
	private byte[] mAdvData = null;
	private long mDiscoveredTime = 0;
	/**
	 * 标识设备是否处于DFU模式
	 */
	private boolean mDfuMode = false;

	public M2MBLEDevice(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
		mDevice = device;
		mRssi = rssi;
		mAdvData = scanRecord;
	}
	
	public M2MBLEDevice(String address) {
		this(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address), 0, null);
	}

	public String getAddress() {
		if(null != mDevice) {
			return mDevice.getAddress();
		}
		else {
			return null;
		}
	}
	
	public int getRssi() {
		return mRssi;
	}
	
	public void setRssi(int rssi) {
		mRssi = rssi;
	}
	
	/**
	 * get TX power (RSSI in the distance of 1 meter) of the beacon
	 * @return RSSI value in 1 meter away or 0 if TX power is not available
	 */
	public int getTxPower() {
		return M2MBLEUtils.txPowerExtract(mAdvData);
	}
	
	/**
	 * get the time the device is discovered
	 * @return time in ms
	 */
	public long getDiscoveredTime() {
		return mDiscoveredTime;
	}
	
	public void setDiscoveredTime(long time) {
		mDiscoveredTime = time;
	}
	
	public void setAdvData(byte[] adv_data) {
		mAdvData = adv_data;
	}
	
	public byte[] getAdvData() {
		return mAdvData;
	}
	
	@Override
	public boolean equals(Object o) {
		if(null == o) {
			return false;
		}
		if(mDevice.getAddress().equals(((M2MBLEDevice)o).getAddress())) {
			return true;
		}
		else {
			return false;
		}
	}

	public BluetoothDevice getBluetoothDevice() {
		return mDevice;
	}
	
	public void setBluetoothDevice(BluetoothDevice device) {
		mDevice = device;
	}
	
	public String getBluetoothDeviceName() {
		String name = mDevice.getName();
		if(null == name) {
			name = M2MBLEUtils.getDeviceNameFromAdvData(mAdvData); 
		}
		return name;
	}
	
	/**
	 * 检查设备是否在DFU模式
	 * @return {@code true}表示设备处于DFU模式，{@code false}表示设备处于正常模式
	 */
	public boolean inDfuMode() {
		return mDfuMode;
	}
	
	/**
	 * 设置设备是否处于DFU模式
	 * @param dfu_mode
	 * 			{@code true}表示设备处于DFU模式，{@code false}表示设备处于正常模式
	 */
	protected void setDfuMode(boolean dfu_mode) {
		mDfuMode = dfu_mode;
	}
	
	public String getModelName() {
		String blt_name = getBluetoothDeviceName();
		
		String model_name = M2MBLEDevice.MODEL_UNKNOWN;
		if(null != blt_name && blt_name.length() > 0) {
			for(String lock_model : M2MBLEDevice.DeviceModels.keySet()) {
				if(blt_name.contains(lock_model)) {
					model_name = lock_model;
					break;
				}
			}
		}
		
		return model_name;
	}
	
	public String getReleaseName() {
		String model_name = getModelName();
		String release_name = "";
		int type = DeviceModels.get(model_name);
		switch(type) {
		case TYPE_DLOCK1:
			release_name = "sLock-1S";
			break;
		case TYPE_ALOCK1:
			release_name = "sLock-1A";
			break;
		case TYPE_CLOCK1:
		case TYPE_CLOCK2:
			release_name = "sLock-3M";
			break;
		case TYPE_FLOCK1:
			release_name = "sLock-3FC";
			break;
		default:
			release_name = model_name;
		}
		return release_name;
	}
}