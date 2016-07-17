package com.alcohol.activity;

import com.alcohol.activety.R;
import com.alcohol.db.UserInfo;
import com.alcohol.subActivity.ViewGuidePage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity{

	private final String TAG = "UI-MainActivity";
	
	private final String ACTION_SHOW_USER = "com.alcohol.app.showUser";
	private final long EXIT_TIME_WAIT = 4000;
	
	private boolean isShowUserLogin = false;
	private long ActiiytyExitTime = 0;

	
	private void WidgetInit(){
		
	}
	
	private void IntentParam(){
		Intent intent  = getIntent();
		Bundle mbundle = intent.getExtras();
		if(mbundle == null){
			isShowUserLogin = false;
		}else{
			boolean mEnter = mbundle.getBoolean(ViewGuidePage.GUIRD_PAGE_ENTER);
			if(mEnter && !checkUser()){
				isShowUserLogin = true;
			}
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		RegisterReceiver();
		
		if(!checkUser()){
			Intent intent = new Intent();
			intent.setAction(ACTION_SHOW_USER);
			sendBroadcast(intent);
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		UnRegidterReceiver();
	}
	
	
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
//		super.onBackPressed();
		if((System.currentTimeMillis()-ActiiytyExitTime) > EXIT_TIME_WAIT){  
			Toast.makeText(this, R.string.str_mainKeydownAgain,Toast.LENGTH_SHORT).show();
			ActiiytyExitTime = System.currentTimeMillis();   
		} else {
			this.finish();
		}
	}

	private boolean checkUser(){
		UserInfo mUserInfo = new UserInfo(this);
		String UserName = mUserInfo.getUserName();
		String UserPwd = mUserInfo.getUserPwd();
		if((null == UserName) || (UserName.equals("")) ||
			(null == UserPwd) || (UserPwd.equals(""))){
			return false;
		}else{
			return true;
		}
	}
	
	private View.OnClickListener mUserDigClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId()){
			case R.id.btRegister:
				Intent intent  = new Intent();
				intent.setClass(MainActivity.this, UserRegisterActivity.class);
				startActivity(intent);
				break;
			}
		}
	};
	
	private void showLoginDialog(){
		
		
		
		LayoutInflater  inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.activity_userlogin,null);
		AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
//		AlertDialog dig =	new AlertDialog.Builder(this).create();
		mBuilder.setView(layout);
		mBuilder.setTitle(R.string.str_UserLoginTitle);
		
		Button ButRegister = (Button) layout.findViewById(R.id.btRegister);
		ButRegister.setOnClickListener(mUserDigClickListener);
		mBuilder.create().show();
	}
	
	private void RegisterReceiver(){
		IntentFilter mFilter = new IntentFilter();
		mFilter.addAction(ACTION_SHOW_USER);
		registerReceiver(mBroadcastReceiver, mFilter);
	}
	
	private void UnRegidterReceiver(){
		unregisterReceiver(mBroadcastReceiver);
	}
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String Action = intent.getAction();
			if(ACTION_SHOW_USER.equals(Action)){
				showLoginDialog();
			}
		}};
	
}
