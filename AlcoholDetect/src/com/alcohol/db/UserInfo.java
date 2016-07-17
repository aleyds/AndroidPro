package com.alcohol.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class UserInfo {
	private static final String PRE_FILE_NAME = "userxml";
	private static final String USER_NAME_KEY = "userName";
	private static final String USER_PWD_KEY  = "userPwd";
	
	private Context mContext;
	
	private String mUserName;
	private String mUserPwd;
	
	public UserInfo(Context mcontext){
		mContext = mcontext;
	}
	
	public String getUserName(){
		SharedPreferences mShared = mContext.getSharedPreferences(PRE_FILE_NAME, Context.MODE_APPEND);
		mUserName =  mShared.getString(USER_NAME_KEY, "");
		return mUserName;
	}
	
	public String getUserPwd(){
		SharedPreferences mShared = mContext.getSharedPreferences(PRE_FILE_NAME, Context.MODE_APPEND);
		mUserPwd =  mShared.getString(USER_PWD_KEY, "");
		return mUserPwd;
	}
	
	public void saveUserInfo(String UserName, String UserPwd){
		SharedPreferences mShared = mContext.getSharedPreferences(PRE_FILE_NAME, Context.MODE_APPEND);
		Editor medit = mShared.edit();
		medit.putString(USER_NAME_KEY, UserName);
		medit.putString(USER_PWD_KEY, UserPwd);
		medit.commit();
	}
}
