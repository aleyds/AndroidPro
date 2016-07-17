package com.alcohol.location;

import java.util.ArrayList;
import java.util.List;

import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

import android.content.Context;
import android.util.Log;

/**
 * 
 * @author baidu
 *
 */
public class LocationService {
	private LocationClient client = null;
	private LocationClientOption mOption,DIYoption;
	private Object  objLock = new Object();

	/***
	 * 
	 * @param locationContext
	 */
	public LocationService(Context locationContext){
		synchronized (objLock) {
			if(client == null){
				client = new LocationClient(locationContext);
				client.setLocOption(getDefaultLocationClientOption());
			}
		}
	}
	
	/***
	 * 
	 * @param listener
	 * @return
	 */
	
	public boolean registerListener(BDLocationListener listener){
		boolean isSuccess = false;
		if(listener != null){
			client.registerLocationListener(listener);
			isSuccess = true;
		}
		return  isSuccess;
	}
	
	public void unregisterListener(BDLocationListener listener){
		if(listener != null){
			client.unRegisterLocationListener(listener);
		}
	}
	
	/***
	 * 
	 * @param option
	 * @return isSuccessSetOption
	 */
	public boolean setLocationOption(LocationClientOption option){
		boolean isSuccess = false;
		if(option != null){
			if(client.isStarted())
				client.stop();
			DIYoption = option;
			client.setLocOption(option);
		}
		return isSuccess;
	}
	
	public LocationClientOption getOption(){
		return DIYoption;
	}
	/***
	 * 
	 * @return DefaultLocationClientOption
	 */
	public LocationClientOption getDefaultLocationClientOption(){
		if(mOption == null){
			mOption = new LocationClientOption();
			mOption.setLocationMode(LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
//			mOption.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
			mOption.setScanSpan(0);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
		    mOption.setIsNeedAddress(false);//可选，设置是否需要地址信息，默认不需要
		    mOption.setIsNeedLocationDescribe(true);//可选，设置是否需要地址描述
		    mOption.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
		    mOption.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
		    mOption.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死   
		    mOption.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
//		    mOption.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
		    mOption.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
		    mOption.setProdName("M2Mkey");
		}
		return mOption;
	}
	
	public void start(){
		synchronized (objLock) {
			if(client != null && !client.isStarted()){
				client.start();
			}
		}
	}
	public void stop(){
		synchronized (objLock) {
			if(client != null && client.isStarted()){
				client.stop();
			}
		}
	}
	
	public final static double MAX_DIST = 100000;
	private final static double EARTH_RADIUS = 6378.137;
	public static double computeDist(M2MLocation loc1, M2MLocation loc2) {
		if(null == loc1 || null == loc2) {
			return MAX_DIST;
		}
		
		double radLat1 = rad(loc1.getLatitude());
		double radLat2 = rad(loc2.getLatitude());
		double a = radLat1 - radLat2;
		double b = rad(loc1.getLongitude()) - rad(loc2.getLongitude());

		double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) +
				Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
		s = s * EARTH_RADIUS;
		s = Math.round(s * 1000);
		return s;
	}
	
	public M2MLocationDecorator getNearestDevice(List<M2MLocationDecorator> loc_list, M2MLocation curr_loc) {
		if(null == curr_loc) { // when current location is unknown, we treat is as in range
			return null;
		}
		M2MLocationDecorator nearest_dev = null;
		double min_dist = MAX_DIST;
		for(M2MLocationDecorator loc : loc_list) {
			
			M2MLocation same_type_loc = loc.getLocation(curr_loc.getLocationType());
			if(null != same_type_loc) {
				Log.d("river", "has same location type");
				double dist = computeDist(same_type_loc, curr_loc);
				if(dist < min_dist) {
					min_dist = dist;
				}
			}
			else {
				Log.d("river", "does not have same location type");
				List<Double> dist_list = new ArrayList<Double>();
				M2MLocation cellular_loc = loc.getCellularLocation();
				if(null != cellular_loc) {
					double dist = computeDist(cellular_loc, curr_loc);
					dist_list.add(dist);
				}
				M2MLocation wifi_loc = loc.getWiFiLocation();
				if(null != wifi_loc) {
					double dist = computeDist(wifi_loc, curr_loc);
					dist_list.add(dist);
				}
				M2MLocation gps_loc = loc.getGpsLocation();
				if(null != gps_loc) {
					double dist = computeDist(gps_loc, curr_loc);
					dist_list.add(dist);
				}
				M2MLocation offline_loc = loc.getOfflineLocation();
				if(null != offline_loc) {
					double dist = computeDist(offline_loc, curr_loc);
					dist_list.add(dist);
				}
				double dist = 0;
				for(Double d : dist_list) {
					dist += d.doubleValue();
				}
				dist = dist / dist_list.size();
				if(dist < min_dist) {
					min_dist = dist;
					nearest_dev = loc;
				}
			}
		}
		return nearest_dev;
	}
	
	public static final double BIG_RANGE_THRESHOLD = 240;
	public boolean hasDeviceInBigRange(List<M2MLocationDecorator> loc_list, M2MLocation curr_loc) {
		if(null == curr_loc) { // when current location is unknown, we treat is as in range
			return true;
		}
		boolean has_device_in_range = false;
		double min_dist = MAX_DIST;
		for(M2MLocationDecorator loc : loc_list) {
			
			M2MLocation same_type_loc = loc.getLocation(curr_loc.getLocationType());
			if(null != same_type_loc) {
//				Log.d("river", "has same location type");
				double dist = computeDist(same_type_loc, curr_loc);
				if(dist < min_dist) {
					min_dist = dist;
				}
			}
			else {
//				Log.d("river", "does not have same location type");
				List<Double> dist_list = new ArrayList<Double>();
				M2MLocation cellular_loc = loc.getCellularLocation();
				if(null != cellular_loc) {
					double dist = computeDist(cellular_loc, curr_loc);
					dist_list.add(dist);
				}
				M2MLocation wifi_loc = loc.getWiFiLocation();
				if(null != wifi_loc) {
					double dist = computeDist(wifi_loc, curr_loc);
					dist_list.add(dist);
				}
				M2MLocation gps_loc = loc.getGpsLocation();
				if(null != gps_loc) {
					double dist = computeDist(gps_loc, curr_loc);
					dist_list.add(dist);
				}
				M2MLocation offline_loc = loc.getOfflineLocation();
				if(null != offline_loc) {
					double dist = computeDist(offline_loc, curr_loc);
					dist_list.add(dist);
				}
				double dist = 0;
				for(Double d : dist_list) {
					dist += d.doubleValue();
				}
				dist = dist / dist_list.size();
				if(dist < min_dist) {
					min_dist = dist;
				}
			}
		}
//		Log.d("river", "get min dist: " + min_dist);
		if(min_dist < BIG_RANGE_THRESHOLD) {
			/*
			 *  240m is the largest error of Baidu LBS
			 *  see http://lbsyun.baidu.com/index.php?title=android-locsdk
			 */
			has_device_in_range = true;
		}
		return has_device_in_range;
	}
	
	private static double rad(double d)
	{
		return d * Math.PI / 180.0;
	}
	
}
