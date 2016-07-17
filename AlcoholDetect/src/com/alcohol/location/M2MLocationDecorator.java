package com.alcohol.location;

import java.util.ArrayList;
import java.util.List;

import com.alcohol.db.CBL;
import com.m2mkey.stcontrol.M2MBLEDevice;

public class M2MLocationDecorator extends DeviceDecorator {
	
	/**
	 * cellular based location
	 */
	private M2MLocation mCellularLoc = null;
	/**
	 * WiFi based location
	 */
	private M2MLocation mWiFiLoc = null;
	/**
	 * GPS based location
	 */
	private M2MLocation mGpsLoc = null;
	/**
	 * Offline location
	 */
	private M2MLocation mOfflineLoc = null;
	
	/**
	 * 标识设备是否离开范围
	 */
	private boolean mOutOfRange= true;

	public M2MLocationDecorator(M2MBLEDevice device) {
		super(device.getAddress());
		// read location info from database
		CBL db = CBL.getInstance();
//		if(null != db) {
//			List<M2MLocation> loc_list = db.getDeviceLocation(device.getAddress());
//			for(M2MLocation loc : loc_list) {
//				switch(loc.getLocationType()) {
//				case M2MLocation.LOCTYPE_CELLULAR:
//					mCellularLoc = loc;
//					break;
//				case M2MLocation.LOCTYPE_GPS:
//					mGpsLoc = loc;
//					break;
//				case M2MLocation.LOCTYPE_OFFLINE:
//					mOfflineLoc = loc;
//					break;
//				case M2MLocation.LOCTYPE_WIFI:
//					mWiFiLoc = loc;
//					break;
//				default:
//					break;
//				}
//			}
//		}
	}
	
	public boolean hasLocation(int loc_type) {
		boolean has_loc = true;
		switch(loc_type) {
		case M2MLocation.LOCTYPE_CELLULAR:
			has_loc = hasCellularLocation();
			break;
		case M2MLocation.LOCTYPE_GPS:
			has_loc = hasGpsLocation();
			break;
		case M2MLocation.LOCTYPE_OFFLINE:
			has_loc = hasOfflineLocation();
			break;
		case M2MLocation.LOCTYPE_WIFI:
			has_loc = hasWiFiLocation();
			break;
		default:
			break;
		}
		return has_loc;
	}
	
	public boolean hasCellularLocation() {
		if(null != mCellularLoc) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean hasWiFiLocation() {
		if(null != mWiFiLoc) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean hasGpsLocation() {
		if(null != mGpsLoc) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean hasOfflineLocation() {
		if(null != mOfflineLoc) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public void setLocation(M2MLocation loc) {
		if(null == loc) {
			return;
		}
		switch(loc.getLocationType()) {
		case M2MLocation.LOCTYPE_CELLULAR:
			setCellularLocation(loc);
			break;
		case M2MLocation.LOCTYPE_GPS:
			setGpsLocation(loc);
			break;
		case M2MLocation.LOCTYPE_OFFLINE:
			setOfflineLocation(loc);
			break;
		case M2MLocation.LOCTYPE_WIFI:
			setWiFiLocation(loc);
			break;
		default:
			break;
		}
	}
	
	public M2MLocation getLocation(int location_type) {
		M2MLocation loc = null;
		switch(location_type) {
		case M2MLocation.LOCTYPE_CELLULAR:
			loc = getCellularLocation();
			break;
		case M2MLocation.LOCTYPE_GPS:
			loc = getGpsLocation();
			break;
		case M2MLocation.LOCTYPE_OFFLINE:
			loc = getOfflineLocation();
			break;
		case M2MLocation.LOCTYPE_WIFI:
			loc = getWiFiLocation();
			break;
		default:
			break;
		}
		
		return loc;
	}
	
	public void setCellularLocation(M2MLocation loc) {
		if(null != loc) {
			mCellularLoc = loc;
		}
	}
	
	public M2MLocation getCellularLocation() {
		return mCellularLoc;
	}
	
	public void setWiFiLocation(M2MLocation loc) {
		if(null != loc) {
			mWiFiLoc = loc;
		}
	}
	
	public M2MLocation getWiFiLocation() {
		return mWiFiLoc;
	}
	
	public void setGpsLocation(M2MLocation loc) {
		if(null != loc) {
			mGpsLoc = loc;
		}
	}
	
	public M2MLocation getGpsLocation() {
		return mGpsLoc;
	}
	
	public void setOfflineLocation(M2MLocation loc) {
		if(null != loc) {
			mOfflineLoc = loc;
		}
	}
	
	public M2MLocation getOfflineLocation() {
		return mOfflineLoc;
	}
	
	/**
	 * save device loations to database
	 * @return
	 */
	public boolean saveLocation() {
		List<M2MLocation> loc_list = new ArrayList<M2MLocation>();
		if(null != mCellularLoc) {
			loc_list.add(mCellularLoc);
		}
		if(null != mWiFiLoc) {
			loc_list.add(mWiFiLoc);
		}
		if(null != mGpsLoc) {
			loc_list.add(mGpsLoc);
		}
		if(null != mOfflineLoc) {
			loc_list.add(mOfflineLoc);
		}
		CBL db = CBL.getInstance();
		return true;
//		if(null != db) {
//			return db.setDeviceLocation(getDeviceId(), loc_list);
//		}
//		else {
//			return false;
//		}
	}
	
	public void setOutOfRange(boolean is_out_of_range) {
		mOutOfRange = is_out_of_range;
	}
	
	public boolean getOutOfRange() {
		return mOutOfRange;
	}

}
