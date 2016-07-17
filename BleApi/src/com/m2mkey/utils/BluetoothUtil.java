package com.m2mkey.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothUtil {
	private static final String TAG = "Tools-ToolBluetooth";

	public static boolean BluetoothDetecte(Context context){
		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			Log.e(TAG, "Failed to get Bluetooth service");
			return false;
		}
		if(!bluetoothAdapter.isEnabled()){
//			Log.w(TAG, "Bluetooth is Not Open");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			context.startActivity(enableBtIntent);
			return false;
		}
		return true;
	}//end function BluetoothDetecte
	
	public static boolean BluetoothDetecte(Activity act,int RequestCode){
		BluetoothManager bluetoothManager = (BluetoothManager) act.getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			Log.e(TAG, "Failed to get Bluetooth service");
			return false;
		}
		if(!bluetoothAdapter.isEnabled()){
//			Log.w(TAG, "Bluetooth is Not Open");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			act.startActivityForResult(enableBtIntent, RequestCode);
			return false;
		}
		return true;
	}//end function BluetoothDetecte
	
	public static BluetoothAdapter getAdapter(Context context){
		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		return bluetoothAdapter;
	}
	
	public static boolean bluetoothEnabled(Context context) {
		if(null == context) {
			return false;
		}
		BluetoothAdapter bluetooth = getAdapter(context);
		if(null == bluetooth || !bluetooth.isEnabled()) {
			return false;
		}
		else {
			return true;
		}
	}
	
	public static void enableBluetooth(Context context) {
		if(null != context) {
			BluetoothAdapter bluetooth = getAdapter(context);
			if(!bluetooth.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				context.startActivity(enableBtIntent);
			}
		}
	}
	
}//end class ToolBluetooth
