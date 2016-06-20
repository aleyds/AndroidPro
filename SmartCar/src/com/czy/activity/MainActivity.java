package com.czy.activity;

import com.czy.ControlCommand;
import com.czy.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity{
	public final static String SERVER_KEY = "ServerKey";
	public final static String SAVEFILENAME="SaveFile";
	public final static String SERVER_ADDRESS_KEY = "SERVER_KEY";
	
	private ImageButton mSettingImage;
	private ImageButton mStartImage;
	
	private Context mContext = null;
	
	private ControlCommand mControlCommand;
	
	private void WedgitInit(){
		mSettingImage = (ImageButton)findViewById(R.id.SettingImage);
		mStartImage = (ImageButton) findViewById(R.id.StartImage);
		mSettingImage.setOnClickListener(mOnClickListener);
		mStartImage.setOnClickListener(mOnClickListener);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mContext = this;
        WedgitInit();
        ServerAddressLoad();
        setTitle(R.string.cn_string_menu_title); 
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
	}
	
	private void ServerAddressLoad(){
		mControlCommand = new ControlCommand(this);
		mControlCommand.LoadCommand();
	}

	private void ServerAddressSave(){
		if(mControlCommand!=null){
			mControlCommand.CommandSave();
		}
	}

	private View.OnClickListener mOnClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId()){
			case R.id.SettingImage:
				Setting();
				break;
			case R.id.StartImage:
				EnterControl();
				break;
				default:
					break;
			}
		}
	};
	
	private void EnterControl(){
		Intent mintent = new Intent();
		mintent.setClass(this, ControlActivity.class);
		startActivity(mintent);
	}
	
	private void SettingInit(View mView){
		EditText up = (EditText)mView.findViewById(R.id.UPControlSet);
		EditText down = (EditText)mView.findViewById(R.id.DownControlSet);
		EditText left = (EditText)mView.findViewById(R.id.LeftControlSet);
		EditText right = (EditText)mView.findViewById(R.id.RightControlSet);
		EditText stop = (EditText)mView.findViewById(R.id.StopControlSet);
		EditText address = (EditText)mView.findViewById(R.id.ControlAddress);
		EditText port = (EditText)mView.findViewById(R.id.PortControlSet);
		EditText vedioAddr = (EditText)mView.findViewById(R.id.connectionurl);
		
		up.setText(mControlCommand.mUpCommand);
		down.setText(mControlCommand.mDownCommand);
		left.setText(mControlCommand.mLeftCommand);
		right.setText(mControlCommand.mRightCommand);
		stop.setText(mControlCommand.mStopCommand);
		address.setText(mControlCommand.mControlAddr);
		port.setText(mControlCommand.mControlPort);
		vedioAddr.setText(mControlCommand.mVideoAddr);
	}
	
	private void SettingSave(View mView){
		EditText up = (EditText)mView.findViewById(R.id.UPControlSet);
		EditText down = (EditText)mView.findViewById(R.id.DownControlSet);
		EditText left = (EditText)mView.findViewById(R.id.LeftControlSet);
		EditText right = (EditText)mView.findViewById(R.id.RightControlSet);
		EditText stop = (EditText)mView.findViewById(R.id.StopControlSet);
		EditText address = (EditText)mView.findViewById(R.id.ControlAddress);
		EditText port = (EditText)mView.findViewById(R.id.PortControlSet);
		EditText vedioAddr = (EditText)mView.findViewById(R.id.connectionurl);
		
		mControlCommand.mUpCommand = up.getText().toString();
		mControlCommand.mDownCommand = down.getText().toString();
		mControlCommand.mLeftCommand = left.getText().toString();
		mControlCommand.mRightCommand = right.getText().toString();
		mControlCommand.mStopCommand = stop.getText().toString();
		mControlCommand.mControlAddr = address.getText().toString();
		mControlCommand.mControlPort = port.getText().toString();
		mControlCommand.mVideoAddr = vedioAddr.getText().toString();
		ServerAddressSave();
	}
	
	private void Setting() {
		LayoutInflater factory=LayoutInflater.from(mContext);
		final View v1=factory.inflate(R.layout.setting,null);
		AlertDialog.Builder dialog=new AlertDialog.Builder(mContext);
		dialog.setView(v1);
		SettingInit(v1);
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	EditText qet = (EditText)v1.findViewById(R.id.connectionurl);
            	SettingSave(v1);
            	Toast.makeText(mContext, "设置成功!", Toast.LENGTH_SHORT).show(); 
            }
        });
        dialog.setNegativeButton("取消",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
        dialog.show();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		//检测返回按键
		if(keyCode == KeyEvent.KEYCODE_BACK){
			new AlertDialog.Builder(this)
			.setTitle(R.string.cn_string_exit_title)
			.setMessage(R.string.cn_string_exit_content)
			.setNegativeButton(R.string.cn_string_exit_no,new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			})
			.setPositiveButton(R.string.cn_string_exit_yes,new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					finish();
				}
			}).show();
		}
		return super.onKeyDown(keyCode, event);
	}

	
	
	
}
