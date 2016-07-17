package com.m2mkey.stcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.m2mkey.stcontrol.lescanner.Scanner;
import com.m2mkey.utils.M2MListUtils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class M2MBLEScanner {
	
	public interface M2MBLEScanListener {
		/**
		 * the callback is running in scanner thread (M2MBLEService thread)
		 * @param dev_list
		 */
		public void onDeviceDiscovered(final List<M2MBLEDevice> dev_list);
	}
	
	private static final String TAG = "BLEScanner";
	/**
	 * the default time of running BLE scan (ms)
	 */
	private static final long DEFAULT_SCAN_DURATION = 1500;
	private static final long POWER_SAVE_MODE_SCAN_INTERVAL = 100000;
	private static final long CASUAL_MODE_SCAN_INTERVAL = 3000;
	
	/**
	 * if a lock cannot be scanned for the time, we think the lock is out of range
	 */
	public static long DeviceDiscoveredThreshold = 10 * 1000;
	
	/**
	 * Bluetooth adapter
	 */
	private BluetoothAdapter mBluetoothAdapter = null;
	/**
	 * list store user specified devices
	 */
	private CopyOnWriteArrayList<M2MBLEDevice> mDeviceList = null;
	/**
	 * list store temporary devices scanned by BLE
	 */
	private CopyOnWriteArrayList<M2MBLEDevice> mDeviceCache = null;
	/**
	 * mark if BLE scan is running
	 */
	private AtomicBoolean mIsScanning = new AtomicBoolean(false);
	/**
	 * flag to stop BLE scan
	 */
	private AtomicBoolean mTerminateLeScan = new AtomicBoolean(false);
	/**
	 * the time of running BLE scan
	 */
	private AtomicLong mScanDuration = new AtomicLong(DEFAULT_SCAN_DURATION);
	/**
	 * mark if BLE scan callback is invoked when no device discovered
	 */
	private AtomicBoolean mFakeCallback = new AtomicBoolean(false);
	
	/**
	 * mark if power save mode is enable,
	 * if it is enabled, BLE scan will sleep for 10s between every scan
	 */
	private AtomicBoolean mPowerSaveEnabled = new AtomicBoolean(false);
	/**
	 * mark if casual mode is enable,
	 * in casual mode, BLE scan will sleep for 3s between every scan
	 */
	private AtomicBoolean mCasualModeEnabled = new AtomicBoolean(false);
	
	/**
	 * list of BLE scan listener
	 */
	private CopyOnWriteArrayList<M2MBLEScanListener> mScanListenerList = null;
	private Scanner mLeScanner = null;
	
	public M2MBLEScanner(BluetoothAdapter bluetooth) {
		mBluetoothAdapter = bluetooth;
		mDeviceList = new CopyOnWriteArrayList<M2MBLEDevice>();
		mDeviceCache = new CopyOnWriteArrayList<M2MBLEDevice>();
		mIsScanning.set(false);
		mLeScanner = Scanner.createLeScanner(mBluetoothAdapter, mDeviceCache);
		mScanListenerList = new CopyOnWriteArrayList<M2MBLEScanListener>();
	}
	
	public void setDevices(List<M2MBLEDevice> devices) {
		if(null == devices) {
			return;
		}
		mDeviceList = new CopyOnWriteArrayList<M2MBLEDevice>();
		for(M2MBLEDevice device : devices) {
			mDeviceList.add(device);
		}
	}
	
	public void setScanListener(M2MBLEScanListener listener) {
		if(!mScanListenerList.contains(listener)) {
			mScanListenerList.add(listener);
		}
	}
	
	public void removeScanListener(M2MBLEScanListener listener) {
		int index = mScanListenerList.indexOf(listener);
		if(-1 != index) {
			mScanListenerList.remove(index);
		}
	}
	
	/**
	 * set BLE scan parameters including scan duration.
	 * @param scan_duration - the time of running BLE scan, in ms and must be larger than 1000ms
	 */
	public void setScanParams(long scan_duration) {
		if(scan_duration < DEFAULT_SCAN_DURATION) {
			scan_duration = DEFAULT_SCAN_DURATION;
		}
		mScanDuration.getAndSet(scan_duration);
	}
	
	
	
	public boolean isScanning() {
		return mIsScanning.get();
	}
	
	public M2MBLEDevice getNearestDevice() {
		M2MBLEDevice nearest = null;
		for(int i=0; i<mDeviceList.size(); i++) {
			if(System.currentTimeMillis() - mDeviceList.get(i).getDiscoveredTime() < DeviceDiscoveredThreshold) {
				if(null == nearest) {
					nearest = mDeviceList.get(i);
				}
				else {
					if(mDeviceList.get(i).getRssi() > nearest.getRssi()) {
						nearest = mDeviceList.get(i);
					}
				}
			}
		}
		return nearest;
	}
	
	public M2MBLEDevice getDevice(String device_id) {
		M2MBLEDevice dev = null;
		for(M2MBLEDevice tmp : mDeviceList) {
			if(tmp.getAddress().equals(device_id)) {
				dev = tmp;
				break;
			}
		}
		return dev;
	}
	
	/**
	 * 获取候选列表中扫描到且信号最强的设备
	 * @param lock_id_list
	 * @return
	 * 有则返回设备，否则返回null
	 */
	public M2MBLEDevice getNearestDeviceInSet(List<String> lock_id_list) {
		M2MBLEDevice nearest = null;
		
		for(M2MBLEDevice dev : mDeviceList) {
			if(System.currentTimeMillis() - dev.getDiscoveredTime() < DeviceDiscoveredThreshold) {
				if(M2MListUtils.objectInList(lock_id_list, dev.getAddress())) {
					if(null == nearest) {
						nearest = dev;
					}
					else {
						if(dev.getRssi() > nearest.getRssi()) {
							nearest = dev;
						}
					}
				}
			}
		}
		return nearest;
	}
	
	public boolean isDeviceDiscovered(String device_id) {
		boolean is_discovered = false;
		for(int i=0; i<mDeviceList.size(); i++) {
			if(mDeviceList.get(i).getAddress().equals(device_id)
					&& System.currentTimeMillis() - mDeviceList.get(i).getDiscoveredTime() < DeviceDiscoveredThreshold) {
				is_discovered = true;
				break;
			}
		}
		return is_discovered;
	}
	
	public boolean isDeviceInDfuMode(String device_id) {
		boolean in_dfu = false;
		
		for(M2MBLEDevice device : mDeviceList) {
			if(device.getAddress().equals(device_id)) {
				if(device.inDfuMode()) {
					in_dfu = true;
				}
				break;
			}
		}
		
		return in_dfu;
	}
	
	public List<M2MBLEDevice> getDiscoveredDevices() {
		List<M2MBLEDevice> list = new ArrayList<M2MBLEDevice>();
		for(int i=0; i<mDeviceList.size(); i++) {
			if(System.currentTimeMillis() - mDeviceList.get(i).getDiscoveredTime() < DeviceDiscoveredThreshold) {
				list.add(mDeviceList.get(i));
			}
		}
		return list;
	}
	
	/**
	 * tell BLE scanner if the callback will be invoked when no device is scanned
	 * @param enable
	 */
	public void enableFakeCallback(boolean enable) {
		mFakeCallback.getAndSet(enable);
	}
	
	/**
	 * tell BLE scanner if power save mode is enabled,
	 * in power save mode, scanner will sleep for 10s after every scan process
	 * @param enable
	 */
	public void enablePowerSaveMode(boolean enable) {
		mPowerSaveEnabled.getAndSet(enable);
	}
	/**
	 * tell BLE scanner if casual mode is enabled,
	 * in casual mode, scanner will sleep for 3s after every scan process
	 * @param enable
	 */
	public void enableCasualMode(boolean enable) {
		mCasualModeEnabled.getAndSet(enable);
	}
	
	public void startScan() {
		if(null == mBluetoothAdapter) {
			Log.e(TAG, "Failed to start BLE scan as Bluetooth adapter is NULL");
			return;
		}
		Log.d(TAG, "starting BLE scan");
		mIsScanning.set(true);
		mTerminateLeScan.compareAndSet(true, false);
		new Thread(mLongTermLeScan).start();
	}
	
	public boolean disabled() {
		return mTerminateLeScan.get();
	}
	
	public void stopScan() {
		Log.d(TAG, "stopping BLE scan");
		mTerminateLeScan.compareAndSet(false, true);
		
	}
	
	private Runnable mLongTermLeScan = new Runnable() {

		@Override
		public void run() {
			Log.d(TAG, "started BLE scan");
			while(!mTerminateLeScan.get()) {
				M2MBLEMutex.lock(1); // occupy BLE, cannot do other BLE operations during the scan process
				mDeviceCache.clear();
				mLeScanner.startScan();
				try {
					Thread.sleep(mScanDuration.get()); // wait for BLE scan
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mLeScanner.stopScan();
				M2MBLEMutex.unlock(1); // release BLE
				
				updateDevices();
				triggerListener();
				
				long delay_time = (int)(Math.random() * 1000);
				if(mPowerSaveEnabled.get()) {
					Log.i(TAG, "power save mode enabled, sleep for 10s");
					delay_time += POWER_SAVE_MODE_SCAN_INTERVAL;
					sleepForPowerSaveMode(delay_time);
				}
				else if(mCasualModeEnabled.get()) {
					Log.i(TAG, "casual mode enabled, sleep for 3s");
					delay_time += CASUAL_MODE_SCAN_INTERVAL;
					sleepForCasualMode(delay_time);
				}
				else {
					sleep(delay_time);
				}
			}
			
			mIsScanning.set(false);
			Log.d(TAG, "stopped BLE scan");
		}
		
		private void sleep(long sleep_time) {
			try {
				Thread.sleep(sleep_time); // start next BLE scan for a random interval
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		private void sleepForCasualMode(long sleep_time) {
			for(long interval=0; interval<=sleep_time; interval += 1000) {
				sleep(1000);
				// 检查模式是否变化
				if(!mCasualModeEnabled.get()) {
					break;
				}
			}
		}
		
		private void sleepForPowerSaveMode(long sleep_time) {
			for(long interval=0; interval<=sleep_time; interval += 1000) {
				sleep(1000);
				// 检查模式是否变化
				if(!mPowerSaveEnabled.get()) {
					break;
				}
			}
		}
		
	};
	
	private void triggerListener() {
		if(!mScanListenerList.isEmpty()) {
			List<M2MBLEDevice> list = new ArrayList<M2MBLEDevice>();
			for(M2MBLEDevice discovered_device : mDeviceCache) {
				list.add(discovered_device);
			}
			if(!list.isEmpty()  || mFakeCallback.get()) {
				for(M2MBLEScanListener listener : mScanListenerList) {
					listener.onDeviceDiscovered(list);
				}
			}
		}
	}
	
	private void updateDevices() {
		if(!mDeviceList.isEmpty()) {
			for(M2MBLEDevice discovered_device : mDeviceCache) {
//				Log.d(TAG, "ID: " + discovered_device.getAddress() + " name: " + discovered_device.getBluetoothDeviceName());
				discovered_device.setDfuMode(M2MBLEDevice.DFU_MODE_TAG.equals(discovered_device.getBluetoothDeviceName()));
				for(M2MBLEDevice device : mDeviceList) {
					if(discovered_device.getAddress().equals(device.getAddress())) {
						device.setRssi(discovered_device.getRssi());
						device.setDiscoveredTime(discovered_device.getDiscoveredTime());
						device.setAdvData(discovered_device.getAdvData());
						device.setDfuMode(discovered_device.inDfuMode());
						Log.d(TAG, "ID: " + discovered_device.getAddress() + " RSSI: " + discovered_device.getRssi()
								+ " txPower: " + discovered_device.getTxPower());
					}
				}
			}
		}
	}

}
