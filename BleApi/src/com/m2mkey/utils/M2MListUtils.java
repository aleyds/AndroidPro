package com.m2mkey.utils;

import java.util.List;

import com.m2mkey.stcontrol.M2MBLEDevice;
import com.m2mkey.stcontrol.M2MLog;

public class M2MListUtils {
	
	public static boolean objectInList(List<M2MBLEDevice> list, M2MBLEDevice obj) {
		if(null == list) {
			return false;
		}
		boolean flag = false;
		for(M2MBLEDevice tmp : list) {
			if(tmp.equals(obj)) {
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	public static boolean objectInList(List<String> list, String obj) {
		if(null == list) {
			return false;
		}
		boolean flag = false;
		for(String tmp : list) {
			if(tmp.equals(obj)) {
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	public static boolean objectInList(List<M2MLog> list, M2MLog obj) {
		if(null == list) {
			return false;
		}
		boolean flag = false;
		for(M2MLog tmp : list) {
			if(tmp.mLogId == obj.mLogId) {
				flag = true;
				break;
			}
		}
		return flag;
	}
}
