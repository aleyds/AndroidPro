package com.m2mkey.stcontrol.lescanner;

import java.util.List;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.m2mkey.stcontrol.M2MBLEDevice;

@TargetApi(18)
public class ScannerForJellyBean extends Scanner {
	
	private List<M2MBLEDevice> mDeviceCache = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	
	public ScannerForJellyBean(BluetoothAdapter bluetooth, List<M2MBLEDevice> scan_cache) {
		mBluetoothAdapter = bluetooth;
		mDeviceCache = scan_cache;
	}

	@Override
	public void startScan() {
		if(null != mBluetoothAdapter && mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		}
	}

	@Override
	public void stopScan() {
		if(null != mBluetoothAdapter && mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}
	
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			M2MBLEDevice discovered_device = new M2MBLEDevice(device, rssi, scanRecord);
			discovered_device.setDiscoveredTime(System.currentTimeMillis());
			mDeviceCache.add(discovered_device);
		}

	};

	@Override
	public void enablePowerSave() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disablePowerSave() {
		// TODO Auto-generated method stub
		
	}

}
