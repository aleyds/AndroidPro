package com.m2mkey.stcontrol;

import android.util.Log;

public class M2MBLEMutex {
	
	private static boolean IsBLEFree = true;
	private static int ThreadType = -1;
	
	public static void init() {
		IsBLEFree = true;
	}
	
	/**
	 * 
	 * @param thread_type - 0 means conn thread, 1 means scan thread
	 */
	synchronized public static void lock(int thread_type) {
		if(IsBLEFree) {
//			Log.d("debug", "[1]set condition to false");
			IsBLEFree = false;
			ThreadType = thread_type;
		}
		else {
//			Log.d("debug", "wait for condition");
			if(1 == thread_type) {
				while(!IsBLEFree) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
//			while(!IsBLEFree) {
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
			ThreadType = thread_type;
			IsBLEFree = false;
//			Log.d("debug", "[2]set condition to false");
		}
	}
	
	public static void unlock(int thread_type) {
//		Log.d("debug", "release condition");
		if(1 == thread_type && 0 == ThreadType) {
			return;
		}
		IsBLEFree = true;
	}

}
