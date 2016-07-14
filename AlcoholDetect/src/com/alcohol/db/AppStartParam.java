package com.alcohol.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class AppStartParam {
	public static final String PRE_FILE_NAME = "startxml";
	public static final String FIRST_START_KEY = "fristStart";
	
	private Context mContext;
	
	public AppStartParam(Context mcontext){
		mContext = mcontext;
	}
	
	public boolean isFirstStart(){
		SharedPreferences mShared = mContext.getSharedPreferences(PRE_FILE_NAME, Context.MODE_APPEND);
		return mShared.getBoolean(FIRST_START_KEY, true);
	}
	
	public void saveFirstStart(boolean isFirst){
		SharedPreferences mShared = mContext.getSharedPreferences(PRE_FILE_NAME, Context.MODE_APPEND);
		Editor medit = mShared.edit();
		medit.putBoolean(FIRST_START_KEY, isFirst);
		medit.commit();
	}
}
