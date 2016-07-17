package com.m2mkey.stcontrol;

import android.bluetooth.BluetoothGattCharacteristic;

public abstract class BLEServiceCallback {
	
	abstract void onGattServerConnected();
	abstract void onGattServerConnecting();
	abstract void onGattServerDisconnected();
	abstract void onGattServerDisconnecting();
	abstract void onGattServerUnknownError(int status);
	abstract void onGattServiceDiscovered(boolean discovered);
	abstract void onGattReadAvailable(BluetoothGattCharacteristic characteristic);
	abstract void onGattWriteSuccess(boolean success);

}
