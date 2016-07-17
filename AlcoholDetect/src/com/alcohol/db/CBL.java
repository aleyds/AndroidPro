package com.alcohol.db;

import java.io.IOException;
import java.util.Map;

import com.alcohol.tools.Tcrypt;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;

import android.content.Context;
import android.util.Log;

public class CBL{
	public final static String TAG = "DB-CBL";
	
	public final static String MKEY_UNUSED_COMMENT = "unused";
	public final static String CBL_MSG_BACKUP_OK = "backup_ok";
	public final static String CBL_MSG_RECOVERY_OK = "recovery_ok";
	public final static String CBL_MSG_REPLICATION_ERROR = "replication_error";
	public final static String CBL_MSG_REPLICATION_OK = "replication_ok";
	public final static String CBL_MSG_AUTO_REPLICATION_OK = "auto_replication_ok";
	public final static String CBL_MSG_AUTO_REPLICATION_ERROR = "auto_replication_error";
	public final static String CBL_MSG_REPLICATION_PROGRESS = "replication_progress";
	public final static String CBL_MSG_REPLICATION_PROGRESS_DATA = "replication_progress_data";
	
	public final static String PUSH_FILTER = "user_doc_filter";
	
	private static Context sAppContext = null;
	private static CBL sInstance = null;
	
	private Manager mManager = null;
	private Database mDatabase = null;
	private String mDBName = "";
	private String mDBCookie = "";
	private byte[] mDBKey = null;
	
	/**
	 * 初始化CBL数据库接口，数据库接口采用单件模式实现
	 * @param app_context
	 */
	public static void initCBL(Context app_context) {
		sAppContext = app_context;
		if(null != sInstance) { // force to create a new instance
			sInstance = null;
		}
		getInstance();
	}
	
	/**
	 * 获取CBL数据库接口实例，使用该方法之前，必须先调用{@code initCBL()}方法，否则该方法会返回{@code null}
	 * @return
	 * 数据库接口实例，或null
	 */
	public static CBL getInstance() {
		if(null == sAppContext) {
			sInstance = null;
		}
		else {
			if(null == sInstance) {
				sInstance = new CBL();
			}
		}
		return sInstance;
	}
	
	/**
	 * 初始化数据库接口
	 */
	private CBL() {
		mManager = null;
		try {
			mManager = new Manager(new AndroidContext(sAppContext), Manager.DEFAULT_OPTIONS);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mDatabase = null;
		mDBName = "";
	}
	
	/**
	 * 获取当前的用户数据库名称
	 * @return
	 * 用户数据库名称，或{@code null}如果未设置用户数据库
	 */
	public String getUserDBName(){
		if(mDBName.equals("")){
			return null;
		}else{
			return mDBName;
		}
	}
	
	/**
	 * 获取用户数据库cookie
	 * @return
	 * 用户数据库cookie，或{@code null}如果未设置cookie
	 */
	public String getUserDBCookie(){
		if(mDBCookie.equals("")){
			return null;
		}else{
			return mDBCookie;
		}
	}
	
	/**
	 * 启动用户数据库
	 * @param userid - 用户账户ID
	 * @param cookie - 用户登录的token
	 * @return
	 * {@code true}数据库启动成功，否则{@code false}
	 */
	public boolean setupUserDB(String userid, String user_token) {
		boolean ok = false;
		
		closeUserDB();
		String db_name = genUserDBName(userid);
		mDBKey = Tcrypt.PBKDF2Encrypt(db_name, 1000);//M2MBLEMessage.getInstance().DKGen(db_name); // 根据数据库名称，生成数据库加密密钥
		mDBName = db_name;
		mDBCookie = user_token; // 数据库cookie与用户token一致
		mDatabase = null;
		try {
			if(null != mManager) {
				// create a name for the database and make sure the name is legal
				if (!Manager.isValidDatabaseName(mDBName)) {
				}
				else {
					// get existed database
					mDatabase = mManager.getExistingDatabase(mDBName);
					if(null == mDatabase) {
						// create a new database
						mDatabase = mManager.getDatabase(mDBName);
					}
					if(null != mDatabase) {
						
						mDatabase.open();
						initViews();
						mDatabase.setMaxRevTreeDepth(3);
						
//						PersistentCookieStore cookieStore = mDatabase.getPersistentCookieStore();
//						BasicClientCookie baseCookie = new BasicClientCookie("AuthSession", mDBCookie);
//						baseCookie.setDomain(GlobalParameter.getCouchDBHost());
//						cookieStore.addCookie(baseCookie);
//						CouchbaseLiteHttpClientFactory cblHttpClientfactory = new CouchbaseLiteHttpClientFactory(cookieStore);
						
//						KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
//						trustStore.load(null);
						
//						InputStream fis3 = sAppContext.getResources().openRawResource(R.raw.rootcrt);
//						BufferedInputStream bis3 = new BufferedInputStream(fis3);
//						CertificateFactory certificateFactory3 = CertificateFactory.getInstance("X.509");
//						X509Certificate cert3 = (X509Certificate) certificateFactory3.generateCertificate(bis3);
//						String alias3 = cert3.getSubjectX500Principal().getName();
//						trustStore.setCertificateEntry(alias3, cert3);

//						CBLTLSSocketFactory tmp = new CBLTLSSocketFactory(trustStore);
//						cblHttpClientfactory.setSSLSocketFactory(tmp);
//						mManager.setDefaultHttpClientFactory(cblHttpClientfactory);
//						// set push filter
//						mDatabase.setFilter(PUSH_FILTER, new ReplicationFilter(){
//							@Override
//							public boolean filter(SavedRevision arg0, Map<String, Object> arg1) {
//								String doc_type = (String)arg0.getProperty("TYPE");
//								if(null != doc_type && (doc_type.equals("user") || doc_type.equals("message"))) {
//									return false;
//								}
//								else {
//									return true;
//								}
//							}
//						});
//						ok = true;
					}
				}
			}
		}
		catch (CouchbaseLiteException e) {
			mDatabase = null;
			e.printStackTrace();
		}
		return ok;
	}
	
	public static final String GUEST_DBNAME = "guest";
	/**
	 * 根据用户ID，生成用户数据库名称
	 * @param user_id
	 * @return
	 * 用户数据库名称，或{@code null}如果用户ID无效
	 */
	private String genUserDBName(String user_id) {
		if(null == user_id || user_id.isEmpty()) {
			return null;
		}
		String db_name = "";
		if(GUEST_DBNAME.equals(user_id)) {
			db_name = GUEST_DBNAME;
		}
		else {
			db_name ="db" + user_id; 
		}
		return db_name;
	}
	
	/**
	 * 关闭当前的用户数据库
	 */
	public void closeUserDB()
	{
		if(null != mDatabase){
//			stopReplication();
			mDatabase.close();
			mDatabase = null;
		}
		mDBName = null;
		mDBCookie = null;
		
	}
	
	/**
	 * 释放CBL数据库接口
	 */
	public void releaseCBL() {
		closeUserDB();
		if(null != mManager) {
			mManager.close();
			mManager = null;
		}
		sAppContext = null;
		sInstance = null;
		Log.i(TAG, "CBL released");
	}
	
	/**
	 * 初始化数据库查询试图
	 */
	private void initViews() {
		// Create lock view
		// [lock_id: {encrypted lock info}]
		View lock_view = mDatabase.getView("LOCK_VIEW");
		lock_view.setMap(new Mapper() {
		    @Override
		    public void map(Map<String, Object> document, Emitter emitter) {
		    	if(document.containsKey("LOCK")) {
		    		emitter.emit(document.get("_id"), document.get("LOCK"));
		    	}
		    }
		}, "1");
		// Create mDBKey view
		// [lock_id: {encrypted mDBKey list}]
		View mkey_view = mDatabase.getView("MKEY_VIEW");
		mkey_view.setMap(new Mapper() {
			@Override
			public void map(Map<String, Object> document, Emitter emitter) {
				if(document.containsKey("MKEY_LIST")) {
					emitter.emit(document.get("_id"), document.get("MKEY_LIST"));
				}
			}
			
		}, "1");
		
		// Create sKey view
		// [lock_id: {encrypted sKey list}]
		View skey_view = mDatabase.getView("SKEY_VIEW");
		skey_view.setMap(new Mapper() {
			@Override
			public void map(Map<String, Object> document, Emitter emitter) {
				if(document.containsKey("SKEY_LIST")) {
					emitter.emit(document.get("_id"), document.get("SKEY_LIST"));
				}
			}
		}, "1");
		
		// Create Log view
		// [lock_id: {encrypted log info}]
		View log_view = mDatabase.getView("LOG_VIEW");
		log_view.setMap(new Mapper() {
			@Override
			public void map(Map<String, Object> document, Emitter emitter) {
				if(document.containsKey("LOG")) {
					String doc_id = (String)document.get("_id");
					String [] tmp = doc_id.split("_");
					
					emitter.emit(tmp[0], document.get("LOG"));
				}
			}
			
		}, "1");
		
		View msg_view = mDatabase.getView("MSG_VIEW");
		msg_view.setMap(new Mapper() {
			@Override
			public void map(Map<String, Object> document, Emitter emitter) {
				if(document.containsKey("MESSAGE_DATA")) {
					String doc_id = (String)document.get("_id");
					String [] tmp = doc_id.split("_");
					emitter.emit(tmp[0], document.get("MESSAGE_DATA"));
				}
			}
			
		}, "1");
		
		View fingerprint_view = mDatabase.getView("FINGERPRINT_VIEW");
		fingerprint_view.setMap(new Mapper() {

			@Override
			public void map(Map<String, Object> document, Emitter emitter) {
				if(document.containsKey("FINGERPRINT_LIST")) {
					emitter.emit(document.get("_id"), document.get("FINGERPRINT_LIST"));
				}
			}
			
		}, "1");
	}
	
	
//	
//	public boolean addLock(String lock_id, int lock_type, int user_mkey, String lock_comment,
//								byte[] ltk, long auth_start, long auth_end, String mkey_comment) {
//		if(null == mDatabase) {
//			Log.e(TAG, "database is null");
//			return false;
//		}
//		else if(null != mDatabase.getExistingDocument(lock_id)) {
//			Log.e(TAG, "the lock already exists");
//			return false;
//		}
//		else {
//			LockDoc lock = new LockDoc(mDatabase, lock_id);
//			if(!lock.open(true)) {
//				Log.e(TAG, "failed to open docum ent");
//				return false;
//			}
//			lock.setLockId(lock_id);
//			lock.setLockType(lock_type);
//			lock.setUsermKeyId(user_mkey);
//			lock.setLockComment(lock_comment);
//			lock.setUsermKeyLTK(ltk);
//			lock.addmKey(user_mkey, ltk, false, mkey_comment, auth_start, auth_end);
//			boolean ok = lock.commit();
//			lock.close();
//			return ok;
//		}
//	}
//	
//	public boolean setLockAppVersion(String lock_id, int version) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		String ver_str = String.format("0.%d", version);
//		lock.setLockAppVersion(ver_str);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public int getLockAppVersion(String lock_id) {
//		String ver = null;
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		ver = lock.getLockAppVersion();
//		lock.close();
//		if(null == ver) {
//			return 0;
//		}
//		else {
//			return Integer.valueOf(ver.substring(2), 10);
//		}
//	}
//	
//	public boolean setLockHwVersion(String lock_id, int version) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.setLockHwVersion(version);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public int getLockHwVersion(String lock_id) {
//		int ver = 0;
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		ver = lock.getLockHwVersion();
//		lock.close();
//		return ver;
//	}
//	
//	public boolean deleteLock(String lock_id, boolean force) {
//		if(null == mDatabase || (!force && this.hasAdminData(lock_id))) {
//			return false;
//		}
//		Document del_lock = mDatabase.getExistingDocument(lock_id);
//		if(null == del_lock) {
//			return false;
//		}
//		boolean removed = false;
//		try {
//			removed = del_lock.delete();
//			removed = true;
//			DeleteLockLog(lock_id);
//			deleteDeviceLocation(lock_id);
//			// delete lock from ShakeConfigDoc
//			List<String> shakeLockList = getFastUnlockDevices();
//			if(null != shakeLockList && M2MListUtils.objectInList(shakeLockList, lock_id)){
//				shakeLockList.remove(lock_id);
//				setFastUnlockDevices(shakeLockList);
//			}
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//		}
//		return removed;
//	}
//
//	/**
//	 * 设置锁的备注名称
//	 * @param lock_id
//	 * @param comment
//	 * @return
//	 */
//	public boolean setLockComment(String lock_id, final String comment) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.setLockComment(comment);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	/**
//	 * get basic information of all locks in database
//	 * @return
//	 * array list of SLock objects, each object contains lock ID(MAC address), lock type, PIN number and friendly name  
//	 */
//	public List<SLock> getLockList() {
//		List<SLock> lock_list_return = new ArrayList<SLock>();
//		if(null == mDatabase) {
//			return lock_list_return;
//		}
//		View lock_view = mDatabase.getExistingView("LOCK_VIEW");
//		if(null == lock_view) {
//			return lock_list_return;
//		}
//		Query query = lock_view.createQuery();
//		QueryEnumerator result;
//		try {
//			result = query.run();
//			for (Iterator<QueryRow> it = result; it.hasNext(); ) {
//			    QueryRow row = it.next();
//			    String lock_info_encrypted = (String)row.getValue();
//			    String tmp = decryptUserData(lock_info_encrypted);
//			    if(null == tmp) {
//			    	continue;
//			    }
//			    JSONObject lock_info = new JSONObject(tmp);
//			    SLock lock = new SLock(lock_info.getString(LockDoc.KEY_LOCK_ID),
//			    						lock_info.getInt(LockDoc.KEY_LOCK_TYPE),
//			    						lock_info.getInt(LockDoc.KEY_USER_MKEY),
//			    						M2MStringUtils.hexStr2Utf8Str(lock_info.getString(LockDoc.KEY_COMMENT)));
//			    lock_list_return.add(lock);
//			}
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		
//		return lock_list_return;
//	}
//	
//	public List<M2MLock> getDeviceList() {
//		List<M2MLock> lock_list = new ArrayList<M2MLock>();
//		
//		if(null == mDatabase) {
//			return lock_list;
//		}
//		View lock_view = mDatabase.getExistingView("LOCK_VIEW");
//		if(null == lock_view) {
//			return lock_list;
//		}
//		Query query = lock_view.createQuery();
//		QueryEnumerator result;
//		try {
//			result = query.run();
//			for (Iterator<QueryRow> it = result; it.hasNext(); ) {
//			    QueryRow row = it.next();
//			    String lock_info_encrypted = (String)row.getValue();
//			    String tmp = decryptUserData(lock_info_encrypted);
//			    if(null == tmp) {
//			    	continue;
//			    }
//			    JSONObject lock_info = new JSONObject(tmp);
//			    M2MLock lock = new M2MLock(lock_info.getString(LockDoc.KEY_LOCK_ID),
//			    							M2MStringUtils.hexStr2Utf8Str(lock_info.getString(LockDoc.KEY_COMMENT)),
//			    							lock_info.getInt(LockDoc.KEY_LOCK_TYPE),
//			    							lock_info.getInt(LockDoc.KEY_USER_MKEY),
//			    							M2MStringUtils.hexStringToByteArray(lock_info.getString(LockDoc.KEY_LTK)));
//			    if(lock_info.has(LockDoc.KEY_LOCK_APP_VERSION)) {
//			    	lock.setLockAppVersion(lock_info.getString(LockDoc.KEY_LOCK_APP_VERSION));
//			    }
//			    if(lock_info.has(LockDoc.KEY_LOCK_HW_VERSION)) {
//			    	lock.setLockHwVersion(lock_info.getInt(LockDoc.KEY_LOCK_HW_VERSION));
//			    }
//			    lock_list.add(lock);
//			}
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		
//		return lock_list;
//	}
//	
//	/**
//	 * check if an admin lock has mKey or sKey data
//	 * @param lock_id - Bluetooth MAC address of the lock
//	 * @return
//	 * true if there is mKey or sKey data, otherwise return false
//	 */
//	public boolean hasAdminData(String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		boolean has_admin_data = false;
//		if(M2MLock.ADMIN_MKEY_ID != lock.getUsermKeyId()) {
//			has_admin_data = false;
//		}
//		else {
//			if(lock.getmKeyList().length() > 1
//					|| (null != lock.getsKeyList() && lock.getsKeyList().length() != 0)
//					|| (null != lock.getFingerprintList() && lock.getFingerprintList().length() !=0)
//					|| (null != lock.getCombinationList() && lock.getCombinationList().length() != 0)) {
//				has_admin_data = true;
//			}
//			else {
//				has_admin_data = false;
//			}
//		}
//		lock.close();
//		return has_admin_data;
//	}
//	
//	/**
//	 * 获取当前用户的mKey ID
//	 * @param lock_id
//	 * @return
//	 */
//	public int getLockPIN(String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		int pin = lock.getUsermKeyId();
//		lock.close();
//		return pin;
//	}
//	
//	/**
//	 * 获取锁的备注名称
//	 * @param lock_id
//	 * @return
//	 */
//	public String getLockComment(String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		String comment = lock.getLockComment();
//		lock.close();
//		return comment;
//	}
//	
//	public int getLockType(String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		int type = lock.getLockType();
//		lock.close();
//		return type;
//	}
//
//	public boolean lockExists(String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		Document lock = mDatabase.getExistingDocument(lock_id);
//		if(null == lock) {
//			return false;
//		}
//		else {
//			return true;
//		}
//	}
//	
//	/**
//	 * 检查锁体是否配置过
//	 * @param lock_id
//	 * @return
//	 * 已配置过返回{@code true}，否则返回{@code false}
//	 */
//	public boolean hasLockBodyConfigured(String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		boolean configured = lock.hasLockBody();
//		lock.close();
//		return configured;
//	}
//	
//	/**
//	 * 获取锁体类型
//	 * @param lock_id
//	 * @return
//	 * 机械锁体返回0，电子锁体返回1
//	 */
//	public int getLockBody(String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		int lock_body = lock.getLockBody();
//		lock.close();
//		return lock_body;
//	}
//	
//	/**
//	 * 设置锁体类型
//	 * @param lock_id
//	 * @param lock_body - 0为机械锁体，1为电子锁体
//	 * @return
//	 */
//	public boolean setLockBody(String lock_id, int lock_body) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.setLockBody(lock_body);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	/*-------------------mKey related methods-------------------*/
//	
//	/**
//	 * 增加普通用户mKey
//	 * @param pin
//	 * @param ltk
//	 * @param lock_id
//	 * @param auth_end - 授权结束日期的时间戳，单位s
//	 * @param comment
//	 * @return
//	 * 添加成功返回{@code true}，否则返回{@code false}
//	 */
//	public boolean addRegularMKey(final int pin, final byte[] ltk, String lock_id, final long auth_end,
//									final String comment) {
//		if(null == mDatabase || mKeyExists(pin, lock_id)) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.addmKey(pin, ltk, true, comment, 0, auth_end);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	/**
//	 * 删除普通用户mKey
//	 * @param key_id
//	 * @param lock_id
//	 * @return
//	 * 删除成功返回{@code true}，否则返回{@code false}
//	 */
//	public boolean deleteRegularmKey(final int key_id, String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.removemKey(key_id);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public boolean mKeyReset(String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		List<Integer> pin_list = new ArrayList<Integer>();
//		lock.open(false);
//		JSONArray mkey_list = lock.getmKeyList();
//		if(null == mkey_list) {
//			Log.e(TAG, "mKey list is null");
//			lock.close();
//			return false;
//		}
//		for(int i=0; i<mkey_list.length(); i++) {
//			try {
//				int pin = mkey_list.getJSONObject(i).getInt(LockDoc.KEY_PIN);
//				if(pin != M2MLock.ADMIN_MKEY_ID) {
//					pin_list.add(pin);
//				}
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//		for(int pin : pin_list) {
//			lock.removemKey(pin);
//		}
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	/**
//	 * 设置管理员mKey的密钥（LTK）
//	 * @param lock_id
//	 * @param new_ltk
//	 * @return
//	 */
//	public boolean setAdminLTK(String lock_id, byte [] new_ltk) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.removemKey(M2MLock.ADMIN_MKEY_ID);
//		lock.addmKey(M2MLock.ADMIN_MKEY_ID, new_ltk, false, "", 0, M2MLock.MKEY_NOEXPIRY);
//		lock.setUsermKeyLTK(new_ltk);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public boolean setmKeyComment(final int key_id, String lock_id, final String comment) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONObject old_mkey = lock.getmKey(key_id);
//		if(null == old_mkey) {
//			Log.w(TAG, "the mKey does not exist");
//			lock.close();
//			return false;
//		}
//		boolean ok = true;
//		try {
//			String hex_ltk = old_mkey.getString(LockDoc.KEY_LTK);
//			boolean usable = old_mkey.getBoolean(LockDoc.KEY_USABLE);
//			long auth_start = old_mkey.getLong(LockDoc.KEY_AUTH_START);
//			long auth_end = old_mkey.getLong(LockDoc.KEY_AUTH_END);
//			lock.removemKey(key_id);
//			lock.addmKey(key_id, M2MStringUtils.hexStringToByteArray(hex_ltk), usable, comment, auth_start, auth_end);
//			ok = lock.commit();
//		} catch (JSONException e) {
//			ok = false;
//			e.printStackTrace();
//		}
//		lock.close();
//		return ok;
//	}
//	
//	/**
//	 * 根据mKey的ID（PIN#）获取密钥（LTK）
//	 * @param key_id
//	 * @param lock_id
//	 * @return
//	 */
//	public byte[] getLtkByPIN(int key_id, String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONObject mkey = lock.getmKey(key_id);
//		byte[] ltk = null;
//		if(null != mkey) {
//			try {
//				String hex_ltk = mkey.getString(LockDoc.KEY_LTK);
//				ltk = M2MStringUtils.hexStringToByteArray(hex_ltk);
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//		lock.close();
//		return ltk;
//	}
//	
//	public String getmKeyComment(int key_id, String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONObject mkey = lock.getmKey(key_id);
//		String comment = null;
//		if(null != mkey) {
//			try {
//				String hex_comment = mkey.getString(LockDoc.KEY_COMMENT);
//				comment = M2MStringUtils.hexStr2Utf8Str(hex_comment);
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//		lock.close();
//		return comment;
//	}
//	
//	/**
//	 * 获取mKey授权结束日期的时间戳
//	 * @param key_id
//	 * @param lock_id
//	 * @return
//	 * 获取成功返回有效的时间戳，否则返回-1
//	 */
//	public long getmKeyAuthEnd(int key_id, String lock_id) {
//		long auth_end = -1;
//		if(null == mDatabase) {
//			return auth_end;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONObject mkey = lock.getmKey(key_id);
//		if(null != mkey) {
//			try {
//				auth_end = mkey.getLong(LockDoc.KEY_AUTH_END);
//			} catch (JSONException e) {
//				auth_end = -1;
//				e.printStackTrace();
//			}
//		}
//		lock.close();
//		return auth_end;
//	}
//	
//	public boolean mKeyExists(int key_id, String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		boolean exists = false;
//		if(null != lock.getmKey(key_id)) {
//			exists = true;
//		}
//		lock.close();
//		return exists;
//	}
//	
//	/**
//	 * 检查mKey是否已分配
//	 * @param key_id
//	 * @param lock_id
//	 * @return
//	 * 已分配返回{@code true}，否则返回{@code false}
//	 */
//	public boolean getmKeyAssigned(int key_id, String lock_id) {
//		boolean used = true;
//		if(null == mDatabase) {
//			return used;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONObject mkey = lock.getmKey(key_id);
//		if(null != mkey) {
//			try {
//				used = !mkey.getBoolean(LockDoc.KEY_USABLE);
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//		lock.close();
//		return used;
//	}
//	
//	/**
//	 * 设置mKey的标识为已分配
//	 * @param key_id
//	 * @param lock_id
//	 * @return
//	 */
//	public boolean setmKeyAssigned(final int key_id, String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONObject mkey = lock.getmKey(key_id);
//		boolean ok = true; 
//		if(null != mkey) {
//			try {
//				String hex_ltk = mkey.getString(LockDoc.KEY_LTK);
//				String hex_comment = mkey.getString(LockDoc.KEY_COMMENT);
//				long auth_start = mkey.getLong(LockDoc.KEY_AUTH_START);
//				long auth_end = mkey.getLong(LockDoc.KEY_AUTH_END);
//				lock.removemKey(key_id);
//				lock.addmKey(key_id, M2MStringUtils.hexStringToByteArray(hex_ltk), false,
//								M2MStringUtils.hexStr2Utf8Str(hex_comment), auth_start, auth_end);
//				ok = lock.commit();
//			} catch (JSONException e) {
//				ok = false;
//				e.printStackTrace();
//			}
//		}
//		lock.close();
//		return ok;
//	}
//	
//	/**
//	 * 获取普通用户权限mKey列表，即PIN#不为1（管理员权限mKey的PIN#）的mKey列表
//	 * @param lock_id
//	 * @return
//	 * 长度大于或等于0的列表
//	 */
//	public List<SKey> getRegularmKeyList(String lock_id) {
//		List<SKey> regular_mkey_list = this.getmKeyList(lock_id);
//		for(int i=0; i<regular_mkey_list.size(); i++) {
//			SKey mkey = regular_mkey_list.get(i);
//			if(M2MBLEDevice.ADMIN_MKEY_ID == mkey.mKeyId) { // remove admin mKey
//				regular_mkey_list.remove(i);
//			}
//		}
//		return regular_mkey_list;
//	}
//	
//	/**
//	 * 获取锁的mKey列表
//	 * @param lock_id
//	 * @return
//	 * 返回mKey列表，有效的锁数据mKey列表的长度必须大于或等于1，因为至少有一个用户当前控制锁使用的mKey
//	 */
//	public List<SKey> getmKeyList(String lock_id) {
//		List<SKey> mkey_list_return = new ArrayList<SKey>();
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONArray mkey_list = lock.getmKeyList();
//		if(null != mkey_list) {
//			for(int i=0; i<mkey_list.length(); i++) {
//				try {
//					JSONObject mkey = mkey_list.getJSONObject(i);
//					int pin = mkey.getInt(LockDoc.KEY_PIN);
//					String hex_ltk = mkey.getString(LockDoc.KEY_LTK);
//					String hex_comment = mkey.getString(LockDoc.KEY_COMMENT);
//					SKey tmp = new SKey(pin, hex_ltk, M2MStringUtils.hexStr2Utf8Str(hex_comment));
//					if(mkey.getBoolean(LockDoc.KEY_USABLE)) { // mKey还未分配
//						tmp.mComment = sAppContext.getString(R.string.cn_mla_unsend) + "mKey";
//					}
//					mkey_list_return.add(tmp);
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		lock.close();
//		return mkey_list_return;
//	}
//	
//	/*-------------------sKey related methods-------------------*/
//	public boolean addsKey(final String romid, final int key_id, String lock_id, final String comment) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.addsKey(romid, comment, key_id);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public boolean deletesKey(final String romid, String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.removesKey(romid);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public boolean sKeyReset(String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.removesKeyList();
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public boolean UpdateIKeyComment(final String romid, String lock_id, final String comment) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONObject skey = lock.getsKey(romid);
//		boolean ok = false;
//		if(null != skey) {
//			try {
//				int key_id = skey.getInt(LockDoc.KEY_SKEY_ID);
//				lock.removesKey(key_id);
//				lock.addsKey(romid, comment, key_id);
//				ok = lock.commit();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//		lock.close();
//		return ok;
//	}
//	
//	public SKey GetIKey(String romid, String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		SKey skey = null;
//		JSONObject skey_json = lock.getsKey(romid);
//		if(null != skey_json) {
//			try {
//				int key_id = skey_json.getInt(LockDoc.KEY_SKEY_ID);
//				String hex_comment = skey_json.getString(LockDoc.KEY_COMMENT);
//				skey = new SKey(key_id, romid, M2MStringUtils.hexStr2Utf8Str(hex_comment));
//			} catch (JSONException e) {
//				skey = null;
//				e.printStackTrace();
//			}
//		}
//		lock.close();
//		return skey;
//	}
//	
//	/**
//	 * 获取sKey的ROM ID
//	 * @param key_id
//	 * @param lock_id
//	 * @return
//	 * 获取成功返回有效的ROM ID，否则返回{@code null}
//	 */
//	public String getRomId(int key_id, String lock_id) {
//		String romid = null;
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONObject skey = lock.getsKey(key_id);
//		if(null != skey) {
//			try {
//				romid = skey.getString(LockDoc.KEY_ROMID);
//			} catch (JSONException e) {
//				e.printStackTrace();
//				romid = null;
//			}
//		}
//		lock.close();
//		return romid;
//	}
//	
//	public String getsKeyComment(int key_id, String lock_id) {
//		String comment = null;
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONObject skey = lock.getsKey(key_id);
//		if(null != skey) {
//			try {
//				comment = M2MStringUtils.hexStr2Utf8Str(skey.getString(LockDoc.KEY_COMMENT));
//			} catch (JSONException e) {
//				e.printStackTrace();
//				comment = null;
//			}
//		}
//		lock.close();
//		return comment;
//	}
//	
//	public boolean sKeyExists(String romid, String lock_id) {
//		boolean existed = false;
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		if(null != lock.getsKey(romid)) {
//			existed = true;
//		}
//		lock.close();
//		return existed;
//	}
//	
//	public List<SKey> GetIKeyList(String lock_id) {
//		List<SKey> skey_list_return = new ArrayList<SKey>();
//		if(null == mDatabase) {
//			return skey_list_return;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONArray skey_list = lock.getsKeyList();
//		if(null != skey_list) {
//			for(int i=0; i<skey_list.length(); i++) {
//				try {
//					JSONObject skey_json = skey_list.getJSONObject(i);
//					SKey skey = new SKey(skey_json.getInt(LockDoc.KEY_SKEY_ID),
//											skey_json.getString(LockDoc.KEY_ROMID),
//											M2MStringUtils.hexStr2Utf8Str(skey_json.getString(LockDoc.KEY_COMMENT)));
//					skey_list_return.add(skey);
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		lock.close();
//		return skey_list_return;
//	}
//	
//	/*-------------------fingerprint related methods-------------------*/
//	public boolean addFingerprint(final int fingerprint_id, final String comment, String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.addFingerprint(fingerprint_id, comment);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public boolean deleteFingerprint(final int del_fingerprint_id, String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.removeFingerprint(del_fingerprint_id);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public boolean resetFingerprint(String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.removeFingerprintList();
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public boolean FingerExistDB(int fingerprint_id, String lock_id){
//		boolean existed = false;
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		if(null != lock.getFingerprint(fingerprint_id)) {
//			existed = true;
//		}
//		lock.close();
//		return existed;
//	}
//	
//	public String GetFingerComment(int fingerprint_id, String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		String comment = null;
//		JSONObject fingerprint = lock.getFingerprint(fingerprint_id);
//		if(null != fingerprint) {
//			try {
//				comment = M2MStringUtils.hexStr2Utf8Str(fingerprint.getString(LockDoc.KEY_COMMENT));
//			} catch (JSONException e) {
//				e.printStackTrace();
//				comment = null;
//			}
//		}
//		lock.close();
//		return comment;
//	}
//	
//	public boolean updateFingerprintComment(final int fingerprint_id, final String comment, String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.removeFingerprint(fingerprint_id);
//		lock.addFingerprint(fingerprint_id, comment);
//		boolean ok = lock.commit();
//		lock.close();
//		return ok;
//	}
//	
//	public List<SKey> getFingerprintList(String lock_id) {
//		List<SKey> fingerprint_list_return = new ArrayList<SKey>();
//		if(null == mDatabase) {
//			return fingerprint_list_return;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONArray finger_list = lock.getFingerprintList();
//		if(null != finger_list) {
//			for(int i=0; i<finger_list.length(); i++) {
//				try {
//					JSONObject finger = finger_list.getJSONObject(i);
//					SKey fingerprint = new SKey(finger.getInt(LockDoc.KEY_ID), "", finger.getString(LockDoc.KEY_COMMENT));
//					fingerprint_list_return.add(fingerprint);
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		lock.close();
//		return fingerprint_list_return;
//	}
//	
//	/*-------------------combination related methods-------------------*/
//	public String getPermanentCombination(String lock_id) {
//		String combination = null;
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		combination = lock.getCombination(1);
//		lock.close();
//		
//		return combination;
//	}
//	
//	public void setPermanentCombination(String lock_id, String comb) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.updateCombination(1, comb);
//		lock.commit();
//		lock.close();
//	}
//	
//	public List<Combination> getOneTimeCombinationList(String lock_id) {
//		JSONArray list = new JSONArray();
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		JSONArray tmp = lock.getCombinationList();
//		if(null != tmp) {
//			list = tmp;
//		}
//		lock.close();
//		List<Combination> comb_list = new ArrayList<Combination>();
//		for(int i=0; i<list.length(); i++) {
//			try {
//				JSONObject obj = list.getJSONObject(i);
//				if(1 != obj.getInt(LockDoc.KEY_ID)) {
//					Combination comb = new Combination();
//					comb.mId = obj.getInt(LockDoc.KEY_ID);
//					comb.mPwd = obj.getString(LockDoc.KEY_PWD);
//					comb_list.add(comb);
//				}
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//		return comb_list;
//	}
//	
//	public void setOneTimeCombinationList(String lock_id, List<Combination> comb_list) {
//		if(null == comb_list || comb_list.isEmpty()) {
//			return;
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		for(int i=0; i<comb_list.size(); i++) {
//			Combination comb = comb_list.get(i);
//			lock.addCombination(comb.mId, comb.mPwd);
//		}
//		lock.commit();
//		lock.close();
//	}
//	
//	public void deleteOneTimeCombination(String lock_id, int pwd_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.removeCombination(pwd_id);
//		lock.commit();
//		lock.close();
//	}
//	
//	public void clearOneTimeCombinationList(String lock_id) {
//		String permenant_pwd = getPermanentCombination(lock_id);
//		if(null == permenant_pwd) {
//			permenant_pwd = "";
//		}
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		lock.removeCombinationList();
//		lock.addCombination(1, permenant_pwd);
//		lock.commit();
//		lock.close();
//	}
//	
//	/*-------------------log related methods-------------------*/
//	private boolean addLog(int logid, int cmd_type, long time, int keyid, String key_uuid,
//							String key_desc, byte[] orig_log, String lock_id) {
//		LogDoc log = new LogDoc(mDatabase, genLogId(lock_id, logid));
//		log.open(true);
//		log.setLogId(logid);
//		log.setCmdType(cmd_type);
//		log.setTimeStamp(time);
//		log.setKeyId(keyid);
//		if(M2MLock.ADMIN_MKEY_ID == keyid) {
//			log.setKeyUuid("00000000000000000000000000000000");
//		}
//		else {
//			log.setKeyUuid(key_uuid);
//		}
//		log.setKeyComment(key_desc);
//		log.setOrigLog(orig_log);
//		log.setLockId(lock_id);
//		boolean ok = log.commit();
//		log.close();
//		return ok;
//	}
//	
//	private String genLogId(String lock_id, int log_id) {
//		return lock_id + "_" + log_id;
//	}
//	
//	public boolean addLog(M2MLog log, String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		return addLog(log.mLogId, log.getCmdID(), log.mTimeStp, log.mKeyId, log.mKeyUuid, log.mKeyComment, log.mOrigLog, lock_id);
//	}
//	
////	public boolean addLog(byte[] content, String lock_id) {
////		if(null == mDatabase) {
////			return false;
////		}
////		M2MLog log = new M2MLog(content);
////		
////		if(LogExists(log.mLogId, lock_id)) {
////			return false;
////		}
////		
////		return addLog(log.mLogId, log.getCmdID(), log.mTimeStp, log.mKeyId, log.mKeyUuid, log.mKeyComment, content, lock_id);
////	}
//	
//	public boolean DeleteLockLog(String lock_id) {
//		if(null == mDatabase) {
//			return false;
//		}
//		View log_view = mDatabase.getExistingView("LOG_VIEW");
//		if(null == log_view) {
//			return false;
//		}
//		Query query = log_view.createQuery();
//		query.setStartKey(lock_id);
//		query.setEndKey(lock_id);
//		QueryEnumerator result;
//		try {
//			result = query.run();
//			for (Iterator<QueryRow> it = result; it.hasNext(); ) {
//			    QueryRow row = it.next();
//			    String log_info = (String)row.getValue();
//			    String tmp = decryptUserData(log_info);
//			    if(null == tmp) {
//			    	continue;
//			    }
//			    JSONObject log = null;
//			    try {
//					log = new JSONObject(tmp);
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//			    if(null != log) {
//			    	String doc_id;
//					try {
//						doc_id = lock_id + "_" + (Integer)log.get("LOG_ID");
//						Document log_doc = mDatabase.getExistingDocument(doc_id);
//						if(null != log_doc) {
//							log_doc.delete();
//						}
//					} catch (JSONException e) {
//						e.printStackTrace();
//					}
//			    }
//			}
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//			return false;
//		}
//		return true;
//	}
//	
//	public int GetLatestLogID(String lock_id, int lock_type) {
//		int logid = 0;
//		List<M2MLog> log_list = this.getLogList(lock_id, lock_type);
//		for(int i=0; i<log_list.size(); i++) {
//			int tmp = log_list.get(i).mLogId;
//			if(tmp > logid) {
//				logid = tmp;
//			}
//		}
//		return logid;
//	}
//	
//	public List<M2MLog> getLogList(String lock_id, int lock_type) {
//		List<M2MLog> log_list_return = new ArrayList<M2MLog>();
//		if(null == mDatabase) {
//			return log_list_return;
//		}
//		View log_view = mDatabase.getExistingView("LOG_VIEW");
//		if(null == log_view) {
//			return log_list_return;
//		}
//		Query query = log_view.createQuery();
//		query.setStartKey(lock_id);
//		query.setEndKey(lock_id);
//		QueryEnumerator result;
//		try {
//			result = query.run();
//			for (Iterator<QueryRow> it = result; it.hasNext(); ) {
//			    QueryRow row = it.next();
//			    	String log_encrypted = (String)row.getValue();
//			    	JSONObject json_log = new JSONObject(this.decryptUserData(log_encrypted));
////			    	byte [] orig_log = M2MStringUtils.hexStringToByteArray(json_log.getString("ORIG_LOG"));
//			    	int log_id = json_log.getInt(LogDoc.KEY_LOG_ID);
//			    	long time = json_log.getLong(LogDoc.KEY_TIME_STAMP);
//			    	int key_id = json_log.getInt(LogDoc.KEY_KEY_ID);
//			    	String key_comment = M2MStringUtils.hexStr2Utf8Str(json_log.getString(LogDoc.KEY_KEY_DESC));
//			    	int cmd_id = json_log.getInt(LogDoc.KEY_CMD_TYPE);
//			    	String key_uuid = null;
//			    	if(json_log.has(LogDoc.KEY_KEY_UUID)) {
//			    		key_uuid = json_log.getString(LogDoc.KEY_KEY_UUID);
//			    	}
//			    	else {
//			    		key_uuid = null;
//			    	}
//			    	M2MLog log = new M2MLog(log_id, time, key_id, key_uuid, key_comment, cmd_id);
//			    	log_list_return.add(log);
//			}
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		return log_list_return;
//	}
//	
//	public boolean LogExists(int log_id, String lock_id) {
//		return documentExists(lock_id + "_" + log_id);
//	}
//	
//	public boolean addUserInfo(String user_id,String user_token,long login_time,String pattern){
//		if(mDatabase==null){
//			return false;
//		}
//		Map<String,Object> userMap=new HashMap<String,Object>();
//		userMap.put("USER_ID", encryptUserData(user_id));
//		//userMap.put("USER_TOKEN", user_token);
//		userMap.put("LOGIN_TIME", login_time);
//		userMap.put("PATTERN", encryptUserData(pattern));
//		userMap.put("TYPE", "user");
//		Document userDoc=mDatabase.getDocument(user_id);
//		try {
//			userDoc.putProperties(userMap);
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//		}
//		return true;
//	}
//	public boolean UserExist(String user_id){
//		if(mDatabase==null){
//			return false;
//		}
//		Document userDoc=mDatabase.getExistingDocument(user_id);
//		if(userDoc!=null){
//			return true;
//		}
//		else{
//			return false;
//		}
//	}
//	
//	/**
//	 * get lock pattern string
//	 * @param user_id - user account ID
//	 * @return
//	 * lock pattern string, or empty string if no pattern set, or null if failed to obtain the string
//	 */
//	public String getUserPattern(String user_id){
//		String userPattern=null;
//		if(mDatabase==null){
//			return null;
//		}
//		Document userDoc=mDatabase.getExistingDocument(user_id);
//		if(userDoc!=null){
//			userPattern=(String) userDoc.getProperty("PATTERN");
//		}
//		String lock_pattern =  decryptUserData(userPattern);
//		if(null == lock_pattern) {
//			lock_pattern = null;
//		}
//		else if(lock_pattern.equals("") || lock_pattern.equals("9")) {
//			lock_pattern = "";
//		}
//		return lock_pattern;
//	}
//	
//	public boolean updateUserPattern(final String user_id,final String user_pattern){
//		if(mDatabase==null){
//			return false;
//		}
//		Document userDoc=mDatabase.getExistingDocument(user_id);
//		if(userDoc==null){
//			return false;
//		}
//		try {
//			userDoc.update(new Document.DocumentUpdater() {
//				@Override
//				public boolean update(UnsavedRevision newRevision) {
//					Map<String,Object> userMap=newRevision.getUserProperties();
//					String userId=(String) userMap.get("USER_ID");
//					if(decryptUserData(userId).equals(user_id)){
//						userMap.put("PATTERN", encryptUserData(user_pattern));
//						newRevision.setUserProperties(userMap);
//						return true;
//					}
//					else{
//						return false;
//					}
//				}
//			});
//			return true;
//		} 
//		catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//			return false;
//		}
//	}
//	
//	public boolean updateLoginTime(final String user_id,final long login_time){
//		if(mDatabase==null){
//			return false;
//		}
//		Document userDoc=mDatabase.getExistingDocument(user_id);
//		if(userDoc==null){
//			return false;
//		}
//		try {
//			userDoc.update(new Document.DocumentUpdater() {
//				
//				@Override
//				public boolean update(UnsavedRevision newRevision) {
//					Map<String,Object> userMap=newRevision.getUserProperties();
//					String userId=(String) userMap.get("USER_ID");
//					if(decryptUserData(userId).equals(user_id)){
//						userMap.put("LOGIN_TIME", login_time);
//						newRevision.setUserProperties(userMap);
//						return true;
//					}
//					else{
//						return false;
//					}
//				}
//			});
//			return true;
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//			return false;
//		}
//	}
//	
//	public static final int AUTO_SYNC = 0;
//	public static final int MANUAL_SYNC = 1;
//	private int mIntentMsgType;
//	
//	/**
//	 * database replication, the result is broadcasted by intent
//	 * @param initent_msg_type
//	 * @return
//	 */
//	public boolean replicate_bc(int initent_msg_type) {
//		mIntentMsgType = initent_msg_type;
//		CBLReplicator replicator = CBLReplicator.getReplicator();
//		if(!replicator.isRunning()) {
//			replicator.setDatabase(mDatabase);
//			replicator.setReplicationURL(GlobalParameter.getCouchDBHttpsUrl() + "/" + mDBName + "/");
//			replicator.setCallback(new CBLReplicator.CBLReplicationCallback() {
//				private long mLastUpdateProgressTime = 0;
//				@Override
//				public void onReplicationTimeout() {
//					Intent intent = new Intent();
//					if(AUTO_SYNC == mIntentMsgType){
//						intent.setAction(CBL.CBL_MSG_AUTO_REPLICATION_ERROR);
//					}else{
//						intent.setAction(CBL.CBL_MSG_REPLICATION_ERROR);
//					}
//					sAppContext.sendBroadcast(intent);
//				}
//				
//				@Override
//				public void onReplicationFinished() {
//					Intent intent = new Intent();
//					Log.d("progress", "finish");
//					if(AUTO_SYNC == mIntentMsgType) {
//						intent.setAction(CBL.CBL_MSG_AUTO_REPLICATION_OK);
//					}
//					else {
//						intent.setAction(CBL.CBL_MSG_REPLICATION_OK);
//					}
//					sAppContext.sendBroadcast(intent);
//				}
//	
//				@Override
//				public void onReplicationFailed() {
//					Intent intent = new Intent();
//					if(AUTO_SYNC == mIntentMsgType){
//						intent.setAction(CBL.CBL_MSG_AUTO_REPLICATION_ERROR);
//					}else{
//						intent.setAction(CBL.CBL_MSG_REPLICATION_ERROR);
//					}
//					sAppContext.sendBroadcast(intent);
//				}
//
//				@Override
//				public void onProgress(int progress) {
//					if(System.currentTimeMillis() - mLastUpdateProgressTime > 1000) {
//						Intent intent = new Intent();
//						intent.putExtra(CBL.CBL_MSG_REPLICATION_PROGRESS_DATA, progress);
//						intent.setAction(CBL.CBL_MSG_REPLICATION_PROGRESS);
//						sAppContext.sendBroadcast(intent);
//						mLastUpdateProgressTime = System.currentTimeMillis();
//					}
//				}
//			});
//			replicator.oneShotSyncStart();
//			return true;
//		}
//		else {
//			return false;
//		}
//	}
//	
//	public boolean AddMessage(String user_id, String lock_id, int msg_type,int key_id,long time)
//	{
//		if(mDatabase==null){
//			return false;
//		}
//		String doc_id = user_id + "_"+time;
//		Document cmdDoc=mDatabase.getExistingDocument(doc_id);
//		if(cmdDoc != null)
//		{
//			return true;
//		}
//		JSONObject cmd_json = new JSONObject();
//		try {
//			cmd_json.put("LOCK_ID", lock_id);
//			cmd_json.put("KEY_ID", key_id);
//			cmd_json.put("STATE_TYPE", msg_type);
//			cmd_json.put("TIME", time);
//		} catch (JSONException e1) {
//			e1.printStackTrace();
//		}
//		String cmd_encrypted = this.encryptUserData(cmd_json.toString());
//		Map<String, Object> cmd_info = new HashMap<String, Object>();
//		cmd_info.put("MESSAGE_DATA", cmd_encrypted);
//		cmd_info.put("TYPE", "message");
//		Document CMDDoc=mDatabase.getDocument(doc_id);
//		try {
//			CMDDoc.putProperties(cmd_info);
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//		}
//		// create database document
//		return false;
//	}
//	
//	static public class MessageData {
//		public String mLockID;
//		public int mType;
//		public int mKeyID;
//		public long mTime;
//		public MessageData(String lock_id, int type, int key_id, long time) {
//			mLockID = lock_id;
//			mType = type;
//			mKeyID = key_id;
//			mTime = time;
//		}
//	}
//	
//	public List<MessageData> GetMessageList() {
//		List<MessageData> msg_list_return = new ArrayList<MessageData>();
//		if(null == mDatabase) {
//			return msg_list_return;
//		}
//		View msg_view = mDatabase.getExistingView("MSG_VIEW");
//		if(null == msg_view) {
//			return msg_list_return;
//		}
//		Query query = msg_view.createQuery();
//		QueryEnumerator result;
//		try {
//			result = query.run();
//			for (Iterator<QueryRow> it = result; it.hasNext(); ) {
//			    QueryRow row = it.next();
//			    String msg_info_encrypted = (String)row.getValue();
//			    JSONObject msg_info = new JSONObject(this.decryptUserData(msg_info_encrypted));
//			    MessageData msg = new MessageData(msg_info.optString("LOCK_ID"),
//			    						  msg_info.optInt("STATE_TYPE"),
//			    						  msg_info.optInt("KEY_ID"),
//			    						  msg_info.optLong("TIME")
//			    						);
//			    msg_list_return.add(msg);
//			}
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		
//		return msg_list_return;
//	}
//	
//	
//	public boolean deleteAllMessage(String user_id)
//	{
//		View lock_view = mDatabase.getExistingView("MSG_VIEW");
//		if(null == lock_view) {
//			return false;
//		}
//		
//		Query query = lock_view.createQuery();
//		QueryEnumerator result;
//		try {
//			result = query.run();
//			for (Iterator<QueryRow> it = result; it.hasNext(); ) {
//			    QueryRow row = it.next();
//			    Document msg_doc = row.getDocument();
//			    msg_doc.delete();
//			}
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//		}
//		
//		return true;
//	}
//	
//	private boolean documentExists(String DocID)
//	{
//		if(mDatabase==null){
//			return false;
//		}
//		Document Doc=mDatabase.getExistingDocument(DocID);
//		if(Doc==null){
//			return false;
//		}
//		else {
//			return true;
//		}
//	}
//	
//	@SuppressLint("TrulyRandom")
//	/**
//	 * 加密用户信息
//	 * @param input
//	 * @return
//	 */
//	public String encryptUserData(String input) {
//		SecretKeySpec sks = new SecretKeySpec(this.mDBKey, "AES");
//		Cipher cipher;
//		byte[] encrypted = null;
//		try {
//			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//			cipher.init(Cipher.ENCRYPT_MODE, sks);
//			encrypted = cipher.doFinal(input.getBytes("UTF-8"));
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		} catch (IllegalBlockSizeException e) {
//			e.printStackTrace();
//		} catch (BadPaddingException e) {
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		} catch (InvalidKeyException e) {
//			e.printStackTrace();
//		}
//		if(null == encrypted) {
//			return null;
//		}
//		else {
//			return M2MStringUtils.byteArrayToHexString(encrypted);
//		}
//	}
//	
//	/**
//	 * 解密用户信息
//	 * @param input
//	 * @return
//	 */
//	public String decryptUserData(String input) {
//		SecretKeySpec sks = new SecretKeySpec(this.mDBKey, "AES");
//		Cipher cipher;
//		byte[] decrypted = null;
//		try {
//			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//			cipher.init(Cipher.DECRYPT_MODE, sks);
//			decrypted = cipher.doFinal(M2MStringUtils.hexStringToByteArray(input));
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		} catch (IllegalBlockSizeException e) {
//			e.printStackTrace();
//		} catch (BadPaddingException e) {
//			e.printStackTrace();
//		} catch (InvalidKeyException e) {
//			e.printStackTrace();
//		}
//		if(null == decrypted) {
//			return null;
//		}
//		else {
//			String json_str = null;
//			try {
//				json_str = new String(decrypted, "UTF8");
//			} catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//			}
//			return json_str;
//		}
//	}
//	
//	private static final String CONFIG_DOC_ID = "configuration";
//	
//	public void setFastUnlockConfig(boolean enabled, boolean keyguard) {
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(true);
//		if(enabled != config.getFastUnlockEnabled() || keyguard != config.getKeyguardEnabled()) {
//			config.setFastUnlockEnabled(enabled);
//			config.setKeyguardEnabled(keyguard);
//			if(!config.commit()) {
//				Log.e(TAG, "setFastUnlockConfig error");
//			}
//		}
//		config.close();
//	}
//	
//	public void setFastUnlockConfig(boolean enabled, boolean keyguard, List<String> locks) {
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(true);
//		List<String> exist_locks = config.getFastUnlockList();
//		boolean same_locks = true;
//		if(null != exist_locks && exist_locks.size() == locks.size()) {
//			for(String lock : locks) {
//				if(!M2MListUtils.objectInList(exist_locks, lock)) {
//					same_locks = false;
//					break;
//				}
//			}
//		}
//		else {
//			same_locks = false;
//		}
//		if(enabled != config.getFastUnlockEnabled() || keyguard != config.getKeyguardEnabled()
//				|| !same_locks) {
//			config.setFastUnlockEnabled(enabled);
//			config.setKeyguardEnabled(keyguard);
//			config.setFastUnlockList(locks);
//			config.commit();
//		}
//		config.close();
//	}
//	
//	public void setAutoSync(final boolean auto_sync, final boolean sync_via_wifi) {
//		if(null != mDatabase) {
//			ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//			config.open(true);
//			config.setAutoSyncEnabled(auto_sync);
//			config.setAutoSyncWithWiFi(sync_via_wifi);
//			config.commit();
//			config.close();
//		}
//	}
//	
//	/**
//	 * get auto-sync config
//	 * @return
//	 * Boolean object, null if no configuration document found,
//	 */
//	public boolean autoSyncEnabled() {
//		if(null == mDatabase) {
//			return false;
//		}
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(true);
//		boolean auto_sync_enabled = config.getAutoSyncEnabled();
//		config.close();
//		return auto_sync_enabled;
//	}
//	
//	/**
//	 * 检查用户是否已配置自动同步
//	 * @return
//	 */
//	public boolean hasAutoSyncConfigured() {
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(true);
//		boolean auto_sync_configured = config.getAutoSyncConfigured();
//		config.close();
//		return auto_sync_configured;
//	}
//	
//	public boolean onlySyncViaWifi() {
//		if(null == mDatabase) {
//			return false;
//		}
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(true);
//		boolean only_sync_with_wifi = config.getAutoSyncWithWiFi();
//		config.close();
//		return only_sync_with_wifi;
//	}
//	
//	public List<String> getFastUnlockDevices() {
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(false);
//		List<String> list = config.getFastUnlockList();
//		config.close();
//		return list;
//	}
//	
//	public void setFastUnlockDevices(final List<String> dev_list) {
//		if(null == mDatabase) {
//			return;
//		}
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(false);
//		config.setFastUnlockList(dev_list);
//		config.commit();
//		config.close();
//	}
//	
//	public boolean getFastUnlockEnabled() {
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(false);
//		boolean enabled = config.getFastUnlockEnabled();
//		config.close();
//		return enabled;
//	}
//	
//	public void setFastUnlockEnabled(final boolean fast_unlock_enabled) {
//		if(null == mDatabase) {
//			return;
//		}
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(true);
//		config.setFastUnlockEnabled(fast_unlock_enabled);
//		config.commit();
//		config.close();
//	}
//	
//	public boolean getKeyguardEnabled() {
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(false);
//		boolean enabled = config.getKeyguardEnabled();
//		config.close();
//		return enabled;
//	}
//	
//	public void setKeyguardEnabled(final boolean keyguard_enabled) {
//		if(null == mDatabase) {
//			return;
//		}
//		ConfigurationDoc config = new ConfigurationDoc(mDatabase, CONFIG_DOC_ID);
//		config.open(true);
//		config.setKeyguardEnabled(keyguard_enabled);
//		config.commit();
//		config.close();
//	}
//	
//	private static final String LOCATION_DOC_ID_BASE = "location_";
//	private static final String LOCATION_DOC_KEY_CELLULAR = "cellular";
//	private static final String LOCATION_DOC_KEY_WIFI = "wifi";
//	private static final String LOCATION_DOC_KEY_GPS = "gps";
//	private static final String LOCATION_DOC_KEY_OFFLINE = "offline";
//	private static final String LOCATION_DOC_KEY_LONGTITUDE = "longtitude";
//	private static final String LOCATION_DOC_KEY_LATITUDE = "latitude";
//	private static final String LOCATION_DOC_KEY_POI = "poi";
//	private static final String LOCATION_DOC_KEY_DESC = "description";
//	
//
//	/**
//	 * set device locations
//	 * @param device_id
//	 * @param loc_list
//	 * @return true if data saved, otherwise false
//	 */
//	public boolean setDeviceLocation(final String device_id, final List<M2MLocation> loc_list) {
//		if(null == mDatabase || null == device_id || null == loc_list || loc_list.isEmpty()) {
//			return false;
//		}
//		String doc_id = LOCATION_DOC_ID_BASE + device_id;
//		Document location_doc = mDatabase.getExistingDocument(doc_id);
//		if(null == location_doc) { // create a location document
//			location_doc = mDatabase.getDocument(doc_id);
//		}
//		// update the location document
//		try {
//			location_doc.update(new Document.DocumentUpdater() {
//
//				@Override
//				public boolean update(UnsavedRevision newRevision) {
//					Map<String, Object> properties = newRevision.getUserProperties();
//					boolean has_update = false;
//					for(M2MLocation loc : loc_list) {
//						String type_str = null;
//						switch(loc.getLocationType()) {
//						case M2MLocation.LOCTYPE_CELLULAR:
//							type_str = LOCATION_DOC_KEY_CELLULAR;
//							break;
//						case M2MLocation.LOCTYPE_GPS:
//							type_str = LOCATION_DOC_KEY_GPS;
//							break;
//						case M2MLocation.LOCTYPE_OFFLINE:
//							type_str = LOCATION_DOC_KEY_OFFLINE;
//							break;
//						case M2MLocation.LOCTYPE_WIFI:
//							type_str = LOCATION_DOC_KEY_WIFI;
//							break;
//						default:
//							break;
//						}
//						String str_prev_loc_info = (String) properties.get(type_str);
//						M2MLocation prev_loc = loadJsonLocData(str_prev_loc_info, loc.getLocationType());
//						if(null != prev_loc && LocationService.computeDist(prev_loc, loc) < 50) {
//							continue;
//						}
//						has_update  = true;
//						JSONObject loc_info = new JSONObject();
//						try {
//							loc_info.put(LOCATION_DOC_KEY_LONGTITUDE, loc.getLongitude());
//							loc_info.put(LOCATION_DOC_KEY_LATITUDE, loc.getLatitude());
//							loc_info.put(LOCATION_DOC_KEY_POI, M2MStringUtils.utf8Str2HexStr(loc.getPoi()));
//							loc_info.put(LOCATION_DOC_KEY_DESC, M2MStringUtils.utf8Str2HexStr(loc.getLocDesc()));
//							properties.put(type_str, loc_info.toString());
//						} catch (JSONException e) {
//							e.printStackTrace();
//						}
//					}
//					if(has_update) {
//						newRevision.setUserProperties(properties);
//						return true;
//					}
//					else {
//						return false;
//					}
//				}
//
//			});
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//			return false;
//		}
//		return true;
//	}
//	
//	/**
//	 * get device locations
//	 * @param device_id
//	 * @return list of different types of location data
//	 */
//	public List<M2MLocation> getDeviceLocation(String device_id) {
//		List<M2MLocation> loc_list = new ArrayList<M2MLocation>();
//		
//		if(null == mDatabase) {
//			return loc_list;
//		}
//		String loc_doc_id = LOCATION_DOC_ID_BASE + device_id;
//		Document location_doc = mDatabase.getExistingDocument(loc_doc_id);
//		if(null == location_doc) {
//			return loc_list;
//		}
//		// read cellular location
//		String tmp = (String)location_doc.getProperty(LOCATION_DOC_KEY_CELLULAR); // read json string
//		M2MLocation cellular_loc = loadJsonLocData(tmp, M2MLocation.LOCTYPE_CELLULAR);
//		if(null != cellular_loc) {
//			loc_list.add(cellular_loc);
//		}
//		// read WiFi location
//		tmp = (String)location_doc.getProperty(LOCATION_DOC_KEY_WIFI); // read json string
//		M2MLocation wifi_loc = loadJsonLocData(tmp, M2MLocation.LOCTYPE_WIFI);
//		if(null != wifi_loc) {
//			loc_list.add(wifi_loc);
//		}
//		// read GPS location
//		tmp = (String)location_doc.getProperty(LOCATION_DOC_KEY_GPS); // read json string
//		M2MLocation gps_loc = loadJsonLocData(tmp, M2MLocation.LOCTYPE_GPS);
//		if(null != gps_loc) {
//			loc_list.add(gps_loc);
//		}
//		// read offline location
//		tmp = (String)location_doc.getProperty(LOCATION_DOC_KEY_OFFLINE); // read json string
//		M2MLocation offline_loc = loadJsonLocData(tmp, M2MLocation.LOCTYPE_OFFLINE);
//		if(null != offline_loc) {
//			loc_list.add(offline_loc);
//		}
//		return loc_list;
//	}
//	
//	private void deleteDeviceLocation(String device_id) {
//		if(null == mDatabase) {
//			return;
//		}
//		String loc_doc_id = LOCATION_DOC_ID_BASE + device_id;
//		Document location_doc = mDatabase.getExistingDocument(loc_doc_id);
//		if(null != location_doc) {
//			try {
//				location_doc.delete();
//			} catch (CouchbaseLiteException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//	
//	private M2MLocation loadJsonLocData(String json_loc_string, int loc_type) {
//		JSONObject loc_info = null;
//		M2MLocation loc = null;
//		if(null == json_loc_string) {
//			return loc;
//		}
//		try {
//			loc_info = new JSONObject(json_loc_string);
//			double longtitude = loc_info.getDouble(LOCATION_DOC_KEY_LONGTITUDE);
//			double latitude = loc_info.getDouble(LOCATION_DOC_KEY_LATITUDE);
//			String poi_hex = loc_info.getString(LOCATION_DOC_KEY_POI);
//			String desc_hex = loc_info.getString(LOCATION_DOC_KEY_DESC);
//			loc = new M2MLocation(longtitude, latitude, M2MStringUtils.hexStr2Utf8Str(poi_hex),
//									M2MStringUtils.hexStr2Utf8Str(desc_hex), loc_type);
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		
//		return loc;
//	}
//	
//	// auto-sync related code
//	private boolean mDaemonRunning = false; // mark if daemon is running
//	/**
//	 * start auto-sync daemon
//	 */
//	public void startOneShotSync() {
//		CBL.getInstance().replicate_bc(CBL.AUTO_SYNC);
//	}
//	
//	public void autoSyncStart(long delay) {
//		if(!mDaemonRunning) {
//			ResetDelayAutoSync(delay);
//			mDaemonRunning = true;
//		}
//	}
//	
//	private void continueSyncStart() {
//		CBLReplicator replicator = CBLReplicator.getReplicator();
//		if(null == replicator) {
//			Log.e(TAG, "Replicator is null");
//			return;
//		}
//		if(!replicator.isRunning()) {
//			replicator.setDatabase(mDatabase);
//			replicator.setReplicationURL(GlobalParameter.getCouchDBHttpsUrl() + "/" + mDBName + "/");
////			replicator.setCallback(new CBLReplicator.CBLReplicationCallback() {
////
////				@Override
////				public void onReplicationFinished() {
////				}
////
////				@Override
////				public void onReplicationTimeout() {
////				}
////
////				@Override
////				public void onReplicationFailed() {
////				}
////
////				@Override
////				public void onProgress(int progress) {
////				}
////			});
//			replicator.startContinuousSync();
//		}
//		else {
//			Log.e(TAG, "Replication is running");
//		}
//	}
//	
//	/**
//	 * get auto-sync daemon state
//	 * @return
//	 * true if the daemon is running, false otherwise
//	 */
//	public boolean autoSyncRunning() {
//		return mDaemonRunning;
//	}
//	
//	/**
//	 * stop auto-sync daemon
//	 */
//	public void stopReplication() {
//		if(CBLReplicator.getReplicator().isRunning()) {
//			CBLReplicator.getReplicator().terminate();
//		}
//		StopDelayAutoSync();
//		mDaemonRunning = false;
//	}
	
//	private DelayAutoSyncHandler mDelayAutoSyncHandler = new DelayAutoSyncHandler(this);
//	static private class DelayAutoSyncHandler extends Handler {
//		WeakReference<CBL> mRef;
//		
//		public DelayAutoSyncHandler(CBL cbl) {
//			mRef = new WeakReference<CBL>(cbl);
//		}
//		
//		@Override
//		public void handleMessage(Message msg) {
//			if(null != mRef.get()) {
//				mRef.get().continueSyncStart();
//			}
//		}
//	}
//	
//	private Runnable mDelayAutoSyncCallback = new Runnable() {
//
//		@Override
//		public void run() {
//			mDelayAutoSyncHandler.sendEmptyMessage(0);
//		}
//		
//	};
//	
//	private void ResetDelayAutoSync(long delay) {
//		mDelayAutoSyncHandler.removeCallbacks(mDelayAutoSyncCallback);
//		mDelayAutoSyncHandler.postDelayed(mDelayAutoSyncCallback, delay);
//	}
//	
//	private void StopDelayAutoSync() {
//		mDelayAutoSyncHandler.removeCallbacks(mDelayAutoSyncCallback);
//	}
//	
//	private static final String POWER_SAVE_SETTINGS_DOCID = "power_save_settings";
//	public void savePowerSaveSettings(String lock_id, int [] time_mask) {
//		PowerSaveSettingsDoc doc = new PowerSaveSettingsDoc(mDatabase, POWER_SAVE_SETTINGS_DOCID);
//		doc.setWiFiOffTimePeriod(lock_id, time_mask);
//		doc.close();
//	}
//	
//	public int [] loadPowerSaveSettings(String lock_id) {
//		PowerSaveSettingsDoc doc = new PowerSaveSettingsDoc(mDatabase, POWER_SAVE_SETTINGS_DOCID);
//		int[] time_mask = doc.getWiFiOffTimePeriod(lock_id);
//		doc.close();
//		return time_mask;
//	}
//	
//	public void showLockDoc(String tag, String lock_id) {
//		LockDoc lock = new LockDoc(mDatabase, lock_id);
//		lock.open(false);
//		Log.d("m2mkey_" + tag, lock.toString());
//		lock.close();
//	}
//	
//	public boolean deleteDatabase(String userid) {
//		String db_name = genUserDBName(userid);
//		boolean ok = true;
//		try {
//			Database db = mManager.getDatabase(db_name);
//			if(null != mDatabase && mDatabase.getName().equals(db_name)) {
//				mDatabase.close();
//			}
//			db.delete();
//		} catch (CouchbaseLiteException e) {
//			e.printStackTrace();
//			ok = false;
//		}
//		return ok;
//	}
}
