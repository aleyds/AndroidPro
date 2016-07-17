package com.alcohol.location;

import com.m2mkey.stcontrol.M2MBLEDevice;

public abstract class DeviceDecorator {
	
	private M2MBLEDevice mDevice = null;
	private String mDeviceId = null;
	
	public DeviceDecorator(M2MBLEDevice device) {
		mDevice = device;
	}
	
	public DeviceDecorator(String device_id) {
		mDeviceId = device_id;
	}
	
	public M2MBLEDevice getDevice() {
		return mDevice;
	}
	
	public String getDeviceId() {
		if(null != mDeviceId) {
			return mDeviceId;
		}
		else {
			return mDevice.getAddress();
		}
	}

}
