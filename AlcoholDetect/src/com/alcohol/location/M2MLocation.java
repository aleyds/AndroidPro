package com.alcohol.location;

public class M2MLocation {
	public static final double INVALID_LBS = 361;
	public static final int LOCTYPE_OFFLINE = 0;
	public static final int LOCTYPE_CELLULAR = 1;
	public static final int LOCTYPE_WIFI = 2;
	public static final int LOCTYPE_GPS = 3;
	
	private double mLatitude;
	private double mLongitude;
	private String mPoi;
	/**
	 * location description
	 */
	private String mLocDesc;
	/**
	 * location type: cellular(3G/4G), WiFi, GPS, Offline
	 */
	private int mLocType;
	
	public M2MLocation(double longtitude, double latitude, String poi, String desc, int loc_type)
	{
		mLatitude=latitude;
		mLongitude=longtitude;
		if(null == poi) {
			poi = "";
		}
		mPoi = poi;
		if(null == desc) {
			desc = "";
		}
		mLocDesc = desc;
		mLocType = loc_type;
	}
	
	/**
	 * check if the location info is valid
	 * @return
	 */
	public boolean isValid() {
		if(INVALID_LBS == mLatitude || INVALID_LBS == mLongitude) {
			return false;
		}
		else {
			return true;
		}
	}

	public double getLatitude() {
		return mLatitude;
	}

	public void setLatitude(double latitude) {
		this.mLatitude = latitude;
	}

	public double getLongitude() {
		return mLongitude;
	}

	public void setLongitude(double longtitude) {
		this.mLongitude = longtitude;
	}

	public String getPoi() {
		return mPoi;
	}

	public void setPoi(String poi) {
		if(null == poi) {
			poi = "";
		}
		this.mPoi = poi;
	}

	public String getLocDesc() {
		return mLocDesc;
	}

	public void setLocDesc(String description) {
		if(null == description) {
			description = "";
		}
		this.mLocDesc = description;
	}
	
	public int getLocationType() {
		return mLocType;
	}
	
	public void setLocationType(int type) {
		mLocType = type;
	}
}
