package com.alcohol.activity;

import java.util.ArrayList;
import java.util.List;

import com.alcohol.db.CBL;
import com.alcohol.db.CBLReplicator;
import com.alcohol.location.M2MLocationDecorator;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

// report content: https://gist.github.com/KevinGaudin/5557961

public class AlcoholApplication extends Application{
	
	public static final int NOTIFICATION_ID_FASTUNLOCKSERVICE = 100;
	
	public static final String TAG = "APP-Application";
	
	private Handler mAppHandler = null;
	private Runnable mCloseDB = new Runnable() {

		@Override
		public void run() {
			if(CBLReplicator.getReplicator().isReplicationFinished()) {
				CBL.getInstance().releaseCBL();
				mAppHandler = null;
			}
			else {
				mAppHandler.postDelayed(mCloseDB, 3000);
			}
		}
		
	};
	
	/**
	 * release resources and start fast-unlock service if the function has been enabled
	 */
	public void appExit() {
//		MsgBoxUtil.clearToast();
//		M2MSystemUtils.clearNotification(getApplicationContext());
		// 等待数据库同步完成
		if(null == mAppHandler) {
			Log.e("debug", "handler is null");
		}
		if(null == mCloseDB) {
			Log.e("debug", "callback is null");
		}
		mAppHandler.postDelayed(mCloseDB, 200);
//		GlobalParameter.AppExited = true;
	}
	
	private boolean mForeground = false;
	private List<String> mFastUnlockList = null;
//	private M2MDeviceTrackerManager mDeviceTrackerMgr = new M2MDeviceTrackerManager();
	/**
	 * mark if the fast-unlock activity is started
	 */
	public boolean mFastUnlockActivityStarted = false;
	public boolean mFastUnlockEnalbed = false;
	/**
	 * mark if the user wants to use system keyguard for fast-unlock
	 * if true, after user click the unlock button, screen unlock will be required before sending unlock command to device
	 */
	public boolean mKeyguardEnabled = false;
	
//	private LocationService mLocationService;
	/**
	 * Baidu LBS callback
	 */
//	private BDLocationListener mLocListener = new BDLocationListener() {
//
//		@Override
//		public void onReceiveLocation(BDLocation location) {
//			if (null != location && location.getLocType() != BDLocation.TypeServerError) {
//				mLastLocTime = System.currentTimeMillis();
//				double longtitude = location.getLongitude();
//				double latitude = location.getLatitude();
//				String poi_str = "";
//				if (location.getPoiList() != null && !location.getPoiList().isEmpty()) {
//					for (int i = 0; i < location.getPoiList().size(); i++) {
//						Poi poi = (Poi) location.getPoiList().get(i);
//						poi_str += poi.getName() + ";";
//					}
//				}
//				String loc_desc = location.getLocationDescribe();
//				Log.i(TAG, "longtitude: " + longtitude + " latitude:" + latitude);
//				Log.i(TAG, "poi: " + poi_str);
//				Log.i(TAG, "location description: " + loc_desc);
//				
//				boolean isSuccess = true;
//				int loc_type = -1;
//				if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
////					Log.d("river", "loc type: " + "GPS");
//					loc_type = M2MLocation.LOCTYPE_GPS;
//				}
//				else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
//					int network_type = M2MNetworkUtils.networkType(getApplicationContext());
//					if(M2MNetworkUtils.CELLULAR == network_type) {
////						Log.d("river", "loc type: " + "Cellular");
//						loc_type = M2MLocation.LOCTYPE_CELLULAR;
//					}
//					else {
////						Log.d("river", "loc type: " + "WiFi");
//						loc_type = M2MLocation.LOCTYPE_WIFI;
//					}
//				}
//				else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
////					Log.d("river", "loc type: " + "offline");
//					loc_type = M2MLocation.LOCTYPE_OFFLINE;
//				}
//				else {
//					isSuccess = false;
//				}
//				
//				if(isSuccess) {
//					mCurrLocation = new M2MLocation(longtitude, latitude, poi_str, loc_desc, loc_type);
//				}
//			}
//			
//			mLocationService.unregisterListener(mLocListener);
//			mLocationService.stop();
//		}
//		
//	};
	/**
	 * last location time
	 */
	private long mLastLocTime = 0;
//	public M2MLocation mCurrLocation = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
//		GlobalParameter.AppExited = false;
	}
	
	/**
	 * 微信API
	 */
//	private IWXAPI mWxApi = null;
	/**
	 * 微信App ID
	 */
	private String mWxAppId = "wx8325bdaac7bb9c07";
	/**
	 * 微博App Key
	 */
	private String mWbAppKey = "2896052841";
	public void init() {
		// initialize bug-report service
		// ACRA.init(this);
		
		if(null != mAppHandler) {
			mAppHandler.removeCallbacks(mCloseDB);
		}
		else {
			mAppHandler = new Handler();
		}

		// get application context
//		GlobalParameter.AppContext = this.getApplicationContext();
		// set global host name verifier for HTTPS
//		M2MSSLSocketFactory.init();
		// initialize the flag of indicating if App is in foreground
		mForeground = false;

		// initialize list to store devices IDs with fast-unlock enabled
		mFastUnlockList = new ArrayList<String>();

		// initialize Baidu LBS
//		mLocationService = new LocationService(getApplicationContext());
//		WriteLog.getInstance().init(); // 初始化日志
		
		// initialize database
		CBL.initCBL(getApplicationContext());
		// initialize user data
		// initialize remote controller
//		M2MRemoteController.init(getApplicationContext());
		
//		initBCP();
		
		// initialize wexin API
//    	mWxApi = WXAPIFactory.createWXAPI(this, mWxAppId, true);
//    	mWxApi.registerApp(mWxAppId);
//    	
//    	// 初始化UI状态
//    	initUIStates();
	}
	
	public void initBCP() {
		// initialize BCP, do not do this in Application's onCreate() method
//		PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY, "ug5314lQQpaGyXA2nWYDl9Uj");
//		BCPHandler.WaitingServer = true;
	}
	
//	public IWXAPI getWxApi() {
////		return mWxApi;
//	}
//	
	public String getWxAppId() {
		return mWxAppId;
	}
	
	public String getWbAppKey() {
		return mWbAppKey;
	}
	
	/**
	 * set App foreground/background flag
	 * @param foreground
	 */
	public void setForeground(boolean foreground) {
		mForeground = foreground;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isForeground() {
		return mForeground;
	}
	
	/**
	 * 
	 * @param dev_id
	 */
	public void addFastUnlockDevice(String dev_id) {
//		if(!M2MListUtils.objectInList(mFastUnlockList, dev_id)) {
//			mFastUnlockList.add(dev_id);
//		}
	}
	
	/**
	 * 
	 * @param dev_list
	 */
	public void setFastUnlockDevices(List<String> dev_list) {
		if(null == dev_list || dev_list.isEmpty()) {
			return;
		}
		mFastUnlockList = dev_list;
	}
	
	public boolean isFastUnlockDevice(String device_id) {
//		if(M2MListUtils.objectInList(mFastUnlockList, device_id)) {
//			return true;
//		}
//		else {
//			return false;
//		}
		return true;
	}
	
	/**
	 * 
	 * @param dev_id
	 */
	public void removeFastUnlockDevice(String dev_id) {
		int pos = -1;
		for(int i=0; i<mFastUnlockList.size(); i++) {
			if(mFastUnlockList.get(i).equals(dev_id)) {
				pos = i;
				break;
			}
		}
		if(-1 != pos) {
			mFastUnlockList.remove(pos);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public List<String> getFastUnlockDevices() {
		return mFastUnlockList;
	}
	
//	public M2MDeviceTrackerManager getDeviceTrackerManager() {
//		return mDeviceTrackerMgr;
//	}
	
//	public void locate() {
//		if(System.currentTimeMillis() - mLastLocTime > 60000) {
//			mLocationService.setLocationOption(mLocationService.getDefaultLocationClientOption());
//			mLocationService.registerListener(mLocListener);
//			mLocationService.start();
//		}
//		else {
////			Log.i(TAG, "too fast to start LBS");
//		}
//	}
	
//	private List<M2MLocationDecorator> mDevLocationList = new ArrayList<M2MLocationDecorator>();
//	public void addDeviceLocationDecorator(M2MBLEDevice device) {
//		if(!deviceInList(mDevLocationList, device.getAddress())) {
//			M2MLocationDecorator loc_decorator = new M2MLocationDecorator(device);
//			mDevLocationList.add(loc_decorator);
//		}
//	}
//	
//	public void removeDeviceLocationDecorator(String device_id) {
//		for(int i=0; i<mDevLocationList.size(); i++) {
//			if(mDevLocationList.get(i).getDeviceId().equals(device_id)) {
//				mDevLocationList.remove(i);
//				break;
//			}
//		}
//	}
	
//	public void updateDeviceLocationDecorator(M2MBLEScanner scanner) {
//		for(M2MLocationDecorator loc : mDevLocationList) {
//			if(null != scanner && scanner.isDeviceDiscovered(loc.getDeviceId())) {
//				if(mCurrLocation != null) {
//					Log.d(TAG, "get device location");
//					loc.setLocation(mCurrLocation);
//					loc.saveLocation();
//				}
//			}
//		}
//	}
	
//	public boolean hasFastUnlockDeviceInBigRange() {
//		// just select the devices with fast-unlock enabled
//		List<M2MLocationDecorator> tmp_list  = new ArrayList<M2MLocationDecorator>();
//		for(String dev_id : mFastUnlockList) {
//			for(M2MLocationDecorator loc : mDevLocationList) {
//				if(loc.getDeviceId().equals(dev_id)) {
//					tmp_list.add(loc);
//				}
//			}
//		}
//		return mLocationService.hasDeviceInBigRange(tmp_list, mCurrLocation);
//	}
	
//	public boolean isDeviceInBigRange(String device_id) {
//		List<M2MLocationDecorator> tmp_list  = new ArrayList<M2MLocationDecorator>();
//		for(M2MLocationDecorator loc : mDevLocationList) {
//			if(loc.getDeviceId().equals(device_id)) {
//				tmp_list.add(loc);
//			}
//		}
//		return mLocationService.hasDeviceInBigRange(tmp_list, mCurrLocation);
//	}
	
	/**
	 * 更新设备不在范围的标识。注意，此方法只检查设备是否不在范围内，若设备在范围内时，并不设置标识，
	 * 只有当用户点击快捷开锁界面的开锁/取消按钮时才会设置
	 */
//	public void updateFastUnlockDeviceInRangeStates() {
//		for(M2MLocationDecorator loc : mDevLocationList) {
//			if(M2MListUtils.objectInList(mFastUnlockList, loc.getDeviceId())
//					&& !isDeviceInBigRange(loc.getDeviceId())) {
//				loc.setOutOfRange(true);
//			}
//		}
//	}
//	
//	public M2MLocationDecorator getDeviceLocation(String device_id) {
//		M2MLocationDecorator ret = null;
//		for(M2MLocationDecorator loc : mDevLocationList) {
//			if(loc.getDeviceId().equals(device_id)) {
//				ret = loc;
//				break;
//			}
//		}
//		return ret;
//	}
//	
//	public M2MBLEDevice getNearestDeviceViaLBS() {
//		// just select the devices with fast-unlock enabled
//		List<M2MLocationDecorator> tmp_list  = new ArrayList<M2MLocationDecorator>();
//		for(String dev_id : mFastUnlockList) {
//			for(M2MLocationDecorator loc : mDevLocationList) {
//				if(loc.getDeviceId().equals(dev_id)) {
//					tmp_list.add(loc);
//				}
//			}
//		}
//		M2MLocationDecorator nearest = mLocationService.getNearestDevice(tmp_list, mCurrLocation);
//		if(null == nearest) {
//			return null;
//		}
//		else {
//			return nearest.getDevice();
//		}
//	}
	
	private boolean deviceInList(List<M2MLocationDecorator> list, String device_id) {
		if(null == list) {
			return false;
		}
		boolean flag = false;
		for(M2MLocationDecorator tmp : list) {
			if(tmp.getDeviceId().equals(device_id)) {
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	
}
