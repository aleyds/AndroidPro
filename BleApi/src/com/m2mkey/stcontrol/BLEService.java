/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2mkey.stcontrol;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BLEService {
	
	private final static String TAG = "BLEService";

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt = null;
	public int mConnectionState = STATE_DISCONNECTED;
	public boolean mServiceDiscoverying = false;
	private BLEServiceCallback mCallback = null;
	
	public BLEService(Context context) {
		if(this.initialize(context)) {
//			Log.d(TAG, "BLEService initialized");
		}
		else {
			Log.e(TAG, "failed to initialize BLEService");
		}
	}
	
	public void setCallback(BLEServiceCallback callback) {
		mCallback = callback;
	}

	/*
	 * Android 4.3, 4.4 seems to have instability with reconnecting to existing
	 * mBluetoothGatt instances, so we will always use a new instance.
	 * 
	 * https://stackoverflow.com/a/18889509
	 */

	/*
	 * add queues to deal with synchronous nature of BLE stack
	 * https://stackoverflow.com/a/18207869
	 */
	private boolean enableTransactionQueue = true;
	private boolean bluetoothGattBusyAndWillReturnLater = false;
	private static final int TRANSACTION_RETRIES = 3;
	private Queue<BLETransaction> mTransactionQueue = null;

	private void addTransaction(BLETransaction bluetoothGattTransaction) {
		if (mTransactionQueue != null) {
			mTransactionQueue.add(bluetoothGattTransaction);
		}
	}

	/**
	 * Enables or disables notification on a given characteristic if possible
	 * 
	 * @param removeHead
	 *            If the current head of mTransactionQueue should be removed (due
	 *            to successful completion)
	 * @return If the BluetoothGatt stack is now with a transaction.
	 */
	private boolean processNextTransactionIfAvailable(boolean removeHead) {

		if (bluetoothGattBusyAndWillReturnLater) {
			return true;
		}

		if (mBluetoothGatt == null) {
			Log.e(TAG, "BluetoothGatt not initialized");
			return false;
		}

		if (mTransactionQueue == null) {
			Log.w(TAG, "mTransactionQueue not initialized");
			return false;
		}

		if (mTransactionQueue.isEmpty()) {
			Log.i(TAG, "mTransactionQueue is empty.");
			bluetoothGattBusyAndWillReturnLater = false;
			return false;
		}

		boolean returnVal = false;

		if (removeHead) {
			mTransactionQueue.remove();
		}

		BLETransaction bgt = mTransactionQueue.peek();

		if (bgt != null && bgt.retries == 0) {
			mTransactionQueue.remove();
			bgt = mTransactionQueue.peek(); // go to next one
		}

		if (bgt == null) {
//			Log.i(TAG, "No more transactions to process.");

		} else {
//			Log.i(TAG,
//					String.format(
//							"Processing head of mTransactionQueue, %d retries remaining.",
//							bgt.retries));
			bgt.retries--;

			final BluetoothGattCharacteristic c;
			final BluetoothGattDescriptor descriptor;
			final int charaProp;

			switch (bgt.type) {
			case BLETransaction.CHARACTERISTIC_READ:
				c = bgt.characteristic;
				charaProp = c.getProperties();
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//					Log.w(TAG, "Reading characteristic " + c.getUuid());
					bluetoothGattBusyAndWillReturnLater = true;//Read Char asynchronization
					returnVal = mBluetoothGatt.readCharacteristic(c);
					if (!returnVal) {
						Log.w(TAG, "Unable to initialize characteristic read!");
						processNextTransactionIfAvailable(false);
					} 
				} else {
					Log.w(TAG,
							"Tried to read from a non-readable characteristic!");
					mTransactionQueue.remove();
					processNextTransactionIfAvailable(false);
					returnVal = false;
				}
				break;

			case BLETransaction.CHARACTERISTIC_WRITE:
				c = bgt.characteristic;
				charaProp = c.getProperties();
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
					final byte[] value = c.getValue();
					String valueAsString = "null";
					if (value != null && value.length > 0) {
						final StringBuilder stringBuilder = new StringBuilder(
								value.length);
						for (byte byteChar : value) {
							stringBuilder.append(String.format("%02X ",
									byteChar));
						}
						valueAsString = new String(value) + "\n"
								+ stringBuilder.toString();
					}

//					Log.w(TAG, "Writing characteristic " + c.getUuid()
//							+ ", value: " + valueAsString);
					bluetoothGattBusyAndWillReturnLater = true;
					returnVal = mBluetoothGatt.writeCharacteristic(c);
					if (!returnVal) {
//						Log.w(TAG, "Unable to initialize characteristic write!");
						processNextTransactionIfAvailable(false);
					} 
				} else {
//					Log.w(TAG, "Tried to write to a non-writeable characteristic!");
					returnVal = false;
					mTransactionQueue.remove();
					processNextTransactionIfAvailable(false);
				}
				break;

			case BLETransaction.DESCRIPTOR_READ:
				descriptor = bgt.descriptor;
				c = descriptor.getCharacteristic();
				bluetoothGattBusyAndWillReturnLater = true;
				returnVal = mBluetoothGatt.readDescriptor(descriptor);
				if (!returnVal) {
//					Log.w(TAG, "Unable to initialize descriptor read!");
					processNextTransactionIfAvailable(false);
				}
				break;

			case BLETransaction.DESCRIPTOR_WRITE:
				descriptor = bgt.descriptor;
				c = descriptor.getCharacteristic();
//				Log.w(TAG, "Writing descriptor " + descriptor.getUuid());
				bluetoothGattBusyAndWillReturnLater = true;
				returnVal = mBluetoothGatt.writeDescriptor(descriptor);
				if (!returnVal) {
//					Log.w(TAG, "Unable to initialize descriptor write!");
					processNextTransactionIfAvailable(false);
				} 
				break;

			}
		}

		return returnVal;
	}

	public static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
	public static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
	public static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
	public static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

	/*
	 * Implements callback methods for GATT events that the app cares about. For
	 * example, connection change and services discovered.
	 */
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			Log.i(TAG, "onConnectionStateChange: status=" + status + ", newState=" + newState);
			
			// Note: There appears to be a bug in the Android BLE stack
			// in which it is possible to have GATT_SUCCESS and STATE_CONNECTED
			// but mBluetoothGatt is null
			if (mBluetoothGatt == null) {
				Log.d(TAG, "Got GATT_SUCESS and STATE_CONNECTED, but BluetoothGatt passed back is null! Disconnecting...");
				disconnect();
				return;
			}

//			if (status != BluetoothGatt.GATT_SUCCESS) {
//				Log.i(TAG, "Unsuccessful connection or disconnection attempt to GATT server");
//				// return;
//			}

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				mConnectionState = STATE_CONNECTED;
				mCallback.onGattServerConnected();
//				Log.d("River", "connect to GATT server END: " + (System.currentTimeMillis() - mTmp));
				// Attempts to discover services after successful connection.
				// If you disconnect during service discovery,
				// onStateConnectionChange callback may not be invoked by
				// BluetoothGatt, so keep track of discovery happening
				mServiceDiscoverying = true;
				
//				try {
//					Thread.sleep(200);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				if(gatt.discoverServices()) {
					mTmp = System.currentTimeMillis();
				}

			} else if (newState == BluetoothProfile.STATE_CONNECTING) {
				mConnectionState = STATE_CONNECTING;
				mCallback.onGattServerConnecting();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				mConnectionState = STATE_DISCONNECTED;
//				Log.i(TAG, "Disconnected from GATT server.");
				if (mTransactionQueue != null) {
					mTransactionQueue.clear();
					mTransactionQueue = null;
				}
				mServiceDiscoverying = false;
				if(!mDisconnectIntentionally) {
					disconnect();
				}
				mCallback.onGattServerDisconnected();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
				mConnectionState = STATE_DISCONNECTING;
				mCallback.onGattServerDisconnecting();
			}
			else {
				Log.e(TAG, "Gatt unkown error");
				mCallback.onGattServerUnknownError(status);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			mServiceDiscoverying = false;
			boolean discovered = true;
			// Prepare transaction queue
			if (status == BluetoothGatt.GATT_SUCCESS) {
//				Log.d("River", "discover services END: " + (System.currentTimeMillis() - mTmp));
				mTransactionQueue = new LinkedList<BLETransaction>();
				
			} else {
				Log.w(TAG, "failed to discover services");
				discovered =false;
			}
			mCallback.onGattServiceDiscovered(discovered);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			final byte[] value = characteristic.getValue();
//			String valueAsString = "null";
			if (value != null && value.length > 0) {
				final StringBuilder stringBuilder = new StringBuilder(value.length);
				for (byte byteChar : value) {
					stringBuilder.append(String.format("%02X ", byteChar));
				}
//				valueAsString = new String(value) + "\n" + stringBuilder.toString();
			}

//			Log.d(TAG, "onCharacteristicRead: " + characteristic.getUuid() + ", valueAsString: " + valueAsString);
			Log.d(TAG, "onCharacteristicRead");
			bluetoothGattBusyAndWillReturnLater = false;
			processNextTransactionIfAvailable(status == BluetoothGatt.GATT_SUCCESS);

			if (status == BluetoothGatt.GATT_SUCCESS) {
//				broadcastUpdate(ACTION_DATA_CHAR_READ_AVAILABLE, characteristic);
				mCallback.onGattReadAvailable(characteristic);
			}
			else {
				mCallback.onGattReadAvailable(null);
			}

		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//			broadcastUpdate(ACTION_DATA_CHAR_READ_AVAILABLE, characteristic);
			Log.d(TAG, "onCharacteristicChanged");
			mCallback.onGattReadAvailable(characteristic);
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			bluetoothGattBusyAndWillReturnLater = false;
			processNextTransactionIfAvailable(status == BluetoothGatt.GATT_SUCCESS);

			if (status == BluetoothGatt.GATT_SUCCESS) {
//				broadcastUpdate(ACTION_DATA_WRITE_SUCCESS, characteristic);
				mCallback.onGattWriteSuccess(true);
			}
			else {
				mCallback.onGattWriteSuccess(false);
			}
		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
//			if (status == BluetoothGatt.GATT_SUCCESS) {
//				broadcastUpdate(ACTION_DATA_RELIABLE_WRITE_SUCCESS, null);
//			}
			// TODO
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			bluetoothGattBusyAndWillReturnLater = false;
			processNextTransactionIfAvailable(status == BluetoothGatt.GATT_SUCCESS);

			if (status == BluetoothGatt.GATT_SUCCESS) {
				// broadcastUpdate(ACTION_DATA_READ_SUCCESS, descriptor);
				// TODO 
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			bluetoothGattBusyAndWillReturnLater = false;
			processNextTransactionIfAvailable(status == BluetoothGatt.GATT_SUCCESS);

			if (status == BluetoothGatt.GATT_SUCCESS) {
				// broadcastUpdate(ACTION_DATA_WRITE_SUCCESS, descriptor);
				mCallback.onGattWriteSuccess(true);
			}
			else {
				mCallback.onGattWriteSuccess(false);
			}
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			mConnRssi = rssi;
		}
	};

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	private boolean initialize(Context context) {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	private long mTmp = 0; // temporary variable for debugging BLE connection
	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(Context context, final String address, final boolean autoConnect) {
		if (address == null) {
//			Log.w(TAG, "address is null.");
			return false;
		}
		
		mConnRssi = -1000;

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
//			Log.w(TAG, "Invalid hardware address for Bluetooth device. Unable to connect.");
			return false;
		}
		mTmp = System.currentTimeMillis();
		
		M2MBLEMutex.lock(0);
		
		mBluetoothGatt = device.connectGatt(context, autoConnect, mGattCallback);
		
		// it seems no improvement by using new connectGatt() method
//		if(android.os.Build.VERSION.SDK_INT < 21) {
//			mBluetoothGatt = device.connectGatt(context, autoConnect, mGattCallback);
//		}
//		else {
//			Method connectGattMethod = null;
//
//            try {
//                connectGattMethod = device.getClass().getMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }
//
//            try {
//            	Log.w("debug", "use new connectGatt()");
//                mBluetoothGatt = (BluetoothGatt) connectGattMethod.invoke(device, context, false, mGattCallback, 2);
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (IllegalArgumentException e) {
//                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            }
//		}
		
//		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		mDisconnectIntentionally = false;
		return true;
	}
	
	// clear BLE cache
	// http://stackoverflow.com/questions/22596951/how-to-programmatically-force-bluetooth-low-energy-service-discovery-on-android
	private boolean refreshDeviceCache(BluetoothGatt gatt){
		boolean bool = false;
		try {  
			BluetoothGatt localBluetoothGatt = gatt;  
			Method localMethod = localBluetoothGatt.getClass().getMethod("refresh");  
			if (localMethod != null) {
//				Log.d("River", "clear device cache");
				bool = ((Boolean) localMethod.invoke(localBluetoothGatt)).booleanValue();  
			}
		}   
		catch (Exception localException) {  
//			Log.e("River", "An exception occured while refreshing device");  
		}  
		return bool;  
	}

	private boolean mDisconnectIntentionally = false;
	
	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public boolean disconnect() {
		boolean ok = false;
		mDisconnectIntentionally = true;
		if (mServiceDiscoverying) {
			Log.w(TAG,
					"Can't disconnect while discovering services,"
					+ " otherwise BluetoothGattCallback.onConnectionStateChange()"
					+ " may not be invoked as it should");
		}
		else {
			/*
			 * There appears to be a bug (maybe race condition?) in
			 * android.bluetooth.BluetoothGatt, where
			 * BluetoothGattCallback.onClientConnectionState() may be invoked AFTER
			 * BluetoothGatt.mCallback is set to null in
			 * BluetoothGatt.unregisterApp()
			 */
			
			if (mBluetoothGatt != null) {
//				clearGattCache();
//				Log.d(TAG, "close GATT client");
				// Always close on disconnect, don't reuse gatt instance due to bug
				// https://stackoverflow.com/a/18889509
				Log.i(TAG, "invoke mBluetoothGatt.disconnect()");
				mBluetoothGatt.disconnect();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				refreshDeviceCache(mBluetoothGatt);
				Log.i(TAG, "invoke mBluetoothGatt.close()");
				mBluetoothGatt.close();
				mBluetoothGatt = null;
			}
//			else {
//				Log.w(TAG, "BluetoothGatt wasn't initialized, simulating DISCONNECTED intent broadcast");
//			}
			ok = true;

			bluetoothGattBusyAndWillReturnLater = false;
		}
		
		M2MBLEMutex.unlock(0);
		
		return ok;
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothGatt not initialized");
			return false;
		}

		if (characteristic == null) {
			Log.w(TAG, "Tried to read a null characteristic!");
			return false;
		}

		if (!enableTransactionQueue) {
			final int charaProp = characteristic.getProperties();
			if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
				return mBluetoothGatt.readCharacteristic(characteristic);
			} else {
				Log.w(TAG, "Tried to read a non-readable characteristic!");
				return false;
			}
		} else {
//			Log.d(TAG, "BLE Server Readchar Transaction adding");
			addTransaction(new BLETransaction(characteristic,
					BLETransaction.CHARACTERISTIC_READ, TRANSACTION_RETRIES));
			processNextTransactionIfAvailable(false);
			return true;
		}
	}

	/**
	 * Request a write on a given {@code BluetoothGattCharacteristic}. The write
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public boolean writeCharacteristic(
			BluetoothGattCharacteristic characteristic) {
		if (mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothGatt not initialized");
			return false;
		}

		if (characteristic == null) {
			Log.w(TAG, "Tried to write a null characteristic!");
			return false;
		}

		if (!enableTransactionQueue) {
			final int charaProp = characteristic.getProperties();
			if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
//				Log.d(TAG, "BLE Server Writechar Gatt Write");
				return mBluetoothGatt.writeCharacteristic(characteristic);
			} else {
//				Log.w(TAG, "Tried to write a non-writeable characteristic!");
				return false;
			}
		} else {
//			Log.d(TAG, "BLE Server Writechar Transaction adding");
			addTransaction(new BLETransaction(characteristic,
					BLETransaction.CHARACTERISTIC_WRITE, TRANSACTION_RETRIES));
			processNextTransactionIfAvailable(false);
			return true;
		}
	}

	public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
		if (mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothGatt not initialized");
			return false;
		}

		if (!enableTransactionQueue) {
			return mBluetoothGatt.readDescriptor(descriptor);
		} else {
			addTransaction(new BLETransaction(descriptor,
					BLETransaction.DESCRIPTOR_READ, TRANSACTION_RETRIES));
			processNextTransactionIfAvailable(false);
			return true;
		}
	}

	public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
		if (mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothGatt not initialized");
			return false;
		}

		if (!enableTransactionQueue) {
			return mBluetoothGatt.writeDescriptor(descriptor);
		} else {
			addTransaction(new BLETransaction(descriptor,
					BLETransaction.DESCRIPTOR_WRITE, TRANSACTION_RETRIES));
			processNextTransactionIfAvailable(false);
			return true;
		}
	}

	/**
	 * Enables or disables notification on a given characteristic if possible
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	public void setCharacteristicNotification(
			BluetoothGattCharacteristic characteristic, boolean enabled) {

		if (mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothGatt not initialized");
			return;
		}

		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getServices() {
		if (mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothGatt not initialized");
			return null;
		}
		return mBluetoothGatt.getServices();
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public BluetoothGattService getService(UUID uuid) {
		if (mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothGatt not initialized");
			return null;
		}
		return mBluetoothGatt.getService(uuid);
	}

	public static void  restartBluetooth(Context context){
		
		 BluetoothManager Manager;
		 BluetoothAdapter Adapter;
		
		 Manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		if (Manager == null) {
			Log.e(TAG, "Not get Bluetooth Manager");
			return;
			}
		Adapter = Manager.getAdapter();
		if (Adapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return;
		}
		if(Adapter.isEnabled()){
			Adapter.disable();
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Adapter.enable();
		}
	}
	
	public BluetoothGattCharacteristic getCharacteristic(UUID serviceUUID,
			UUID characteristicUUID) {
		BluetoothGattService service = getService(serviceUUID);

		if (service == null) {
			Log.i(TAG, "Unable to get service: " + serviceUUID);
			return null;
		}

		BluetoothGattCharacteristic characteristic = service
				.getCharacteristic(characteristicUUID);

		if (characteristic == null) {
			Log.i(TAG, "Unable to get characteristic: " + characteristicUUID);
			return null;
		}

		return characteristic;
	}
	
	protected void readConnRssi() {
		mBluetoothGatt.readRemoteRssi();
	}
	
	private int mConnRssi = -1000;
	protected int getConnRssi() {
		return mConnRssi;
	}
}
