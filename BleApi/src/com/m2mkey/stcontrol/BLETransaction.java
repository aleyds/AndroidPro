package com.m2mkey.stcontrol;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public class BLETransaction {
	public int type;
	public int retries;

	public BluetoothGattCharacteristic characteristic;
	public BluetoothGattDescriptor descriptor;

	final public static int CHARACTERISTIC_READ = 0;
	final public static int CHARACTERISTIC_WRITE = 1;
	final public static int DESCRIPTOR_READ = 2;
	final public static int DESCRIPTOR_WRITE = 3;

	public BLETransaction(BluetoothGattCharacteristic c, int t, int r) {
		retries = r;
		characteristic = c;
		type = t;
	}

	public BLETransaction(BluetoothGattDescriptor d, int t, int r) {
		retries = r;
		descriptor = d;
		type = t;
	}
}
