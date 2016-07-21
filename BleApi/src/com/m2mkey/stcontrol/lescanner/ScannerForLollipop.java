package com.m2mkey.stcontrol.lescanner;

import java.util.ArrayList;
import java.util.List;

import com.m2mkey.stcontrol.M2MBLEDevice;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.net.wifi.ScanResult;
import android.util.Log;

@TargetApi(21)
public class ScannerForLollipop extends Scanner{
	
	private static final String TAG = "ScannerForLollipop";
	
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothLeScanner mScanner;
	private List<M2MBLEDevice> mDeviceCache = null;
	List<ScanFilter> mFilters = new ArrayList<ScanFilter>();
    ScanSettings mSettings;
	
	public ScannerForLollipop(BluetoothAdapter bluetooth, List<M2MBLEDevice> scan_cache) {
		mBluetoothAdapter = bluetooth;
		mDeviceCache = scan_cache;
		disablePowerSave();
	}

	@Override
	public void startScan() {
		mScanner = mBluetoothAdapter.getBluetoothLeScanner();
		if(null != mScanner) {
			try {
				mScanner.startScan(mFilters, mSettings, mLeScanCallback);
			}
			catch(NullPointerException npe) {
                // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
                Log.w(TAG, "Cannot start scan.  Unexpected NPE.", npe);
            }
		}
	}

	@Override
	public void stopScan() {
		if(null != mScanner) {
			try{
				mScanner.stopScan(mLeScanCallback);
			}
			catch (NullPointerException npe) {
                // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
                Log.w(TAG, "Cannot stop scan.  Unexpected NPE.", npe);
            }
			catch(IllegalStateException e) {
				// in case that BT is turned off
				Log.w(TAG, "Cannot stop scan. Is BT turned off?", e);
			}
		}
	}
	
	private ScanCallback mLeScanCallback = new ScanCallback() {

		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			M2MBLEDevice discovered_device = new M2MBLEDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
			discovered_device.setDiscoveredTime(System.currentTimeMillis());
			mDeviceCache.add(discovered_device);
		}
		
	};

	@Override
	public void enablePowerSave() {
		mSettings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
	}

	@Override
	public void disablePowerSave() {
		mSettings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)).build();
	}

}
