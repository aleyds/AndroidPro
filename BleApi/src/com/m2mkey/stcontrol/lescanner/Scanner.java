package com.m2mkey.stcontrol.lescanner;

import java.util.List;

import com.m2mkey.stcontrol.M2MBLEDevice;

import android.bluetooth.BluetoothAdapter;

public abstract class Scanner {
	
	public static Scanner createLeScanner(BluetoothAdapter bluetooth, List<M2MBLEDevice> scan_cache) {
		
		if(android.os.Build.VERSION.SDK_INT < 21) {
			return new ScannerForJellyBean(bluetooth, scan_cache);
		}
		else {
			return new ScannerForLollipop(bluetooth, scan_cache);
		}
	}
	
	public abstract void startScan();
	
	public abstract void stopScan();
	
	public abstract void enablePowerSave();
	
	public abstract void disablePowerSave();

}
