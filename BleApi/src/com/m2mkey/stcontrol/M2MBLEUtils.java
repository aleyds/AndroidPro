package com.m2mkey.stcontrol;

import android.util.Log;

public class M2MBLEUtils {
	
	private static final String TAG = "M2MBLEUtils";
	
	static public void printScanRecord(final byte[] scan_record) {
		StringBuilder buffer = new StringBuilder();
		for(int i=0; i<scan_record.length; i++) {
			buffer.append(String.format("%02x", scan_record[i]));
		}
		Log.d(TAG, buffer.toString());
	}
	
	static public int txPowerExtract(final byte[] scan_record) {
		// check if the BLE data is iBeacon advertisement
		if(0x4c == scan_record[5] && 0x00 == scan_record[6]) {
			// read txPower for iBeacon adv.
			return scan_record[29];
		}
		else {
			return 0;
		}
	}
	
	static public String getDeviceNameFromAdvData(byte[] adv_data) {
		String dev_name = M2MBLEDevice.MODEL_UNKNOWN;
		if(null == adv_data) {
			return dev_name;
		}
		int i = 0;
		int len = adv_data.length;
		while(i < len - 1)
		{
			int data_len = adv_data[i];
			int type = adv_data[i+1];
			if(type == 0x09)
			{
				char[] chars = new char[data_len-1];
				for(int j=0; j<data_len-1; j++) {
					if(i + 2 + j >= len) {
						break;
					}
					chars[j] = (char)adv_data[i+2+j];
				}
				dev_name = String.copyValueOf(chars);
				break;
			}
			i += data_len+1;
		}

		return dev_name;
	}
	
	static public int getDeviceType(String model_name) {
		if(M2MBLEDevice.DeviceModels.containsKey(model_name)) {
			return M2MBLEDevice.DeviceModels.get(model_name);
		}
		else {
			return M2MBLEDevice.TYPE_UNKNOWN;
		}
	}

}
