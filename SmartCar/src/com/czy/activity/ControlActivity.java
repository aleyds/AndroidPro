package com.czy.activity;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Hashtable;

import com.czy.ControlCommand;
import com.czy.HttpRequest;
import com.czy.R;
import com.czy.StreamSplit;
import com.czy.widget.MySeekBar;
import com.czy.widget.MySeekBar.OnSeekBarChangeListener;
import com.czy.widget.MyVedio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ControlActivity extends Activity{
	
	//Control Command
	public final static String RUNNING_STOP = "FF000000FF";
	public final static String RUNNING_UP = "FF000100FF";
	public final static String RUNNING_DOWN = "FF000200FF";
	public final static String RUNNING_LEFT = "FF000300FF";
	public final static String RUNNING_RIGHT = "FF000400FF";
	public final static String RUNNING_LEFT_UP = "FF000500FF";
	public final static String RUNNING_RIGHT_UP = "FF000600FF";
	public final static String RUNNING_LEFT_DOWN = "FF000700FF";
	public final static String RUNNING_RIGHT_DOWN = "FF000800FF";
	public final static String COMMAND_ALARM_ON = "FF030000FF";
	public final static String COMMAND_ALARM_OFF = "FF030100FF";
	public final static String COMMAND_LED_ON = "FF020000FF";
	public final static String COMMAND_LED_OFF = "FF020100FF";
	
	private final int DefaultRemotePort = 1376;
	private final int DefaultLocalPort = 8080;
	private final String DefaultAddress = "192.168.1.1";
	private static final int MENU_ITEM_COUNTER = Menu.FIRST; 
	
//	private MyVedio mMyVedio;

//	private ImageView mUpControlImage;
//	private ImageView mDownControlImage;
//	private ImageView mLeftControlImage;
//	private ImageView mRightControlImage;
	
	private ImageView VedioViewPic;
	
	
	private ImageButton mLeftConLeftUpImage;
	private ImageButton mLeftConUpImage;
	private ImageButton mLeftConRightUpImage;
	private ImageButton mLeftConLeftImage;
	private ImageButton mLeftConRightImage;
	private ImageButton mLeftConLeftDownImage;
	private ImageButton mLeftConDownImage;
	private ImageButton mLeftConRightDownImage;
	
	private ImageButton mRightConUpImage;
	private ImageButton mRightConLeftImage;
	private ImageButton mRightConRightImage;
	private ImageButton mRightConDownImage;
	
	private CheckBox mSteerBox;
	private CheckBox mGravityBox;
	private CheckBox mEffectBox;
	
	private ImageButton mAlarmImage;
	private ImageButton mRorchImage;
	private boolean mAlarmStart = false;
	private boolean mRorchStart = false;
	
	private LinearLayout mEffectLinear;
	private LinearLayout mSteerLinear;
	
	private MySeekBar mSteerSeekBar1;
	private MySeekBar mSteerSeekBar2;
	private MySeekBar mSteerSeekBar3;
	private MySeekBar mSteerSeekBar4;
	private MySeekBar mSteerSeekBar5;
	private MySeekBar mSteerSeekBar6;
	
	
	private Context mContext;
	
	private Boolean isStop = true;
	private Handler messageHandler =null;
	private String ServerAddress;
	private HttpRequest http = null;
	private boolean isConnectServer = false;
	private ControlCommand mControlCommand;
	
	private DataOutputStream  mDataOutputStream;
	private Socket socket = null;
	private Thread thread = null;//控制小车线程
	private boolean isConnect = false;
	
	private void WidgetInit(){
		
		VedioViewPic = (ImageView)findViewById(R.id.VedioViewPic);
//		mMyVedio = (MyVedio)findViewById(R.id.paintVedio);
		//SurfaceHolder holder = mMyVedio.getHolder(); 
		//holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		//mMyVedio.Draw();
		
		mLeftConLeftUpImage = (ImageButton)findViewById(R.id.LeftConLeftUp);
		mLeftConUpImage = (ImageButton)findViewById(R.id.LeftConUp);
		mLeftConRightUpImage = (ImageButton)findViewById(R.id.LeftConRightUp);
		mLeftConLeftImage = (ImageButton)findViewById(R.id.LeftConLeft);
		mLeftConRightImage = (ImageButton)findViewById(R.id.LeftConRight);
		mLeftConLeftDownImage = (ImageButton)findViewById(R.id.LeftConLeftDown);
		mLeftConDownImage = (ImageButton)findViewById(R.id.LeftConDown);
		mLeftConRightDownImage = (ImageButton)findViewById(R.id.LeftConRightDown);
		
		mRightConUpImage = (ImageButton)findViewById(R.id.RightConUp);
		mRightConLeftImage = (ImageButton)findViewById(R.id.RightConLeft);
		mRightConRightImage = (ImageButton)findViewById(R.id.RightConRight);
		mRightConDownImage = (ImageButton)findViewById(R.id.RightConDown);
		
		mSteerBox = (CheckBox)findViewById(R.id.SteerControl);
		mGravityBox = (CheckBox)findViewById(R.id.GravityControl);
		mEffectBox = (CheckBox)findViewById(R.id.EffectControl);
		
		mAlarmImage = (ImageButton)findViewById(R.id.AlarmImage);
		mRorchImage = (ImageButton)findViewById(R.id.RorchImage);
		
		mEffectLinear = (LinearLayout)findViewById(R.id.EffectLinear);
		mSteerLinear = (LinearLayout)findViewById(R.id.SteerLinear);
		
		mSteerSeekBar1 = (MySeekBar)findViewById(R.id.seekBar1);
		mSteerSeekBar2 = (MySeekBar)findViewById(R.id.seekBar2);
		mSteerSeekBar3 = (MySeekBar)findViewById(R.id.seekBar3);
		mSteerSeekBar4 = (MySeekBar)findViewById(R.id.seekBar4);
		mSteerSeekBar5 = (MySeekBar)findViewById(R.id.seekBar5);
		mSteerSeekBar6 = (MySeekBar)findViewById(R.id.seekBar6);
		
		mLeftConLeftUpImage.setOnTouchListener(mOnTouchListener);
		mLeftConUpImage.setOnTouchListener(mOnTouchListener);
		mLeftConRightUpImage.setOnTouchListener(mOnTouchListener);
		mLeftConLeftImage.setOnTouchListener(mOnTouchListener);
		mLeftConRightImage.setOnTouchListener(mOnTouchListener);
		mLeftConLeftDownImage.setOnTouchListener(mOnTouchListener);
		mLeftConDownImage.setOnTouchListener(mOnTouchListener);
		mLeftConRightDownImage.setOnTouchListener(mOnTouchListener);
		
//		mRightConUpImage.setOnTouchListener(mOnTouchListener);
//		mRightConLeftImage.setOnTouchListener(mOnTouchListener);
//		mRightConRightImage.setOnTouchListener(mOnTouchListener);
//		mRightConDownImage.setOnTouchListener(mOnTouchListener);
		
		mRightConUpImage.setOnClickListener(mOncClickListener);
		mRightConLeftImage.setOnClickListener(mOncClickListener);
		mRightConRightImage.setOnClickListener(mOncClickListener);
		mRightConDownImage.setOnClickListener(mOncClickListener);
		
		mAlarmImage.setOnClickListener(mOncClickListener);
		mRorchImage.setOnClickListener(mOncClickListener);
		mSteerSeekBar1.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		mSteerSeekBar2.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		mSteerSeekBar3.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		mSteerSeekBar4.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		mSteerSeekBar5.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		mSteerSeekBar6.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
//		mSteerSeekBar1.setMax(180);
//		mSteerSeekBar1.setProgress(90);
//		
//		mSteerSeekBar2.setMax(180);
//		mSteerSeekBar3.setMax(180);
//		mSteerSeekBar4.setMax(180);
//		mSteerSeekBar5.setMax(180);
//		mSteerSeekBar6.setMax(180);
		
		
		mSteerBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
		mGravityBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
		mEffectBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
		
		if(mEffectBox.isChecked()){
			mEffectLinear.setVisibility(View.VISIBLE);
		}else{
			mEffectLinear.setVisibility(View.GONE);
		}
		
		if(mSteerBox.isChecked()){
			mSteerLinear.setVisibility(View.VISIBLE);
		}else{
			mSteerLinear.setVisibility(View.GONE);
		}
		
	}
	
	private void ParamInit(){
		mControlCommand = new ControlCommand(this);
		mControlCommand.LoadCommand();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        mContext = this;
        WidgetInit();
        ParamInit();
        startControl();
        
		http = new HttpRequest();  
        Looper looper = Looper.myLooper();
        messageHandler = new MessageHandler(looper);
        setTitle(R.string.cn_string_menu_title); 
        ServerConnect();
//        mMyVedio.SetUrl(mControlCommand.mVideoAddr);
        //mMyVedio.Draw();
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

	private void socketClose()
	{
		if (isConnect) {
			isConnect = false;
			try {
				if (socket != null) {
					socket.close();
					socket = null;
//					printWriter.close();
//					printWriter = null;
					if(null != mDataOutputStream)
					{
						
						mDataOutputStream.close();
					}
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			thread.interrupt();
	}
		isStop = true;
}
		
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		//finish();
		socketClose();
		super.onDestroy();
		
	}
	
	
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// TODO Auto-generated method stub
//		menu.add(0, MENU_ITEM_COUNTER, 0, R.string.cn_string_connect);
//		return super.onCreateOptionsMenu(menu);
//	}
//
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// TODO Auto-generated method stub
//		switch(item.getItemId()){
//		case MENU_ITEM_COUNTER:
//			ServerConnect();
//			break;
//			default:
//				break;
//		}
//		return super.onOptionsItemSelected(item);
//	}




	private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			// TODO Auto-generated method stub
			switch(buttonView.getId())
			{
			case R.id.SteerControl:
				if(isChecked){
					mSteerLinear.setVisibility(View.VISIBLE);
				}else{
					mSteerLinear.setVisibility(View.GONE);
				}
				break;
			case R.id.GravityControl:
				break;
			case R.id.EffectControl:
				if(isChecked){
					mEffectLinear.setVisibility(View.VISIBLE);
				}else{
					mEffectLinear.setVisibility(View.GONE);
				}
				break;
			default:
				break;
			}
		}
	};
	
private String ByteToHex(byte mByte){
	char[] BaseStr="0123456789ABCDEF".toCharArray();
	String Out = ""+BaseStr[(mByte>>4)&0xf] + BaseStr[mByte&0xf];
	return Out;
}

private void _SendSteerValue(byte Channel, byte value)
{
		
		String SendDat = "FF01"+ByteToHex(Channel)+ByteToHex(value)+"FF";
		Log.i("aleyds", "channel:"+Channel + "   value:"+SendDat);
		sendData(SendDat);
}
	
private OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
	
	@Override
	public void onStopTrackingTouch(MySeekBar VerticalSeekBar) {
		// TODO Auto-generated method stub
		switch(VerticalSeekBar.getId())
		{
		case R.id.seekBar1:
			_SendSteerValue((byte)1,(byte)VerticalSeekBar.getProgress());
			break;
		case R.id.seekBar2:
			_SendSteerValue((byte)2,(byte)VerticalSeekBar.getProgress());
			break;
		case R.id.seekBar3:
			_SendSteerValue((byte)3,(byte)VerticalSeekBar.getProgress());
			break;
		case R.id.seekBar4:
			_SendSteerValue((byte)4,(byte)VerticalSeekBar.getProgress());
			break;
		case R.id.seekBar5:
			_SendSteerValue((byte)5,(byte)VerticalSeekBar.getProgress());
			break;
		case R.id.seekBar6:
			_SendSteerValue((byte)6,(byte)VerticalSeekBar.getProgress());
			break;
			default:
				break;
		}
	}
	
	@Override
	public void onStartTrackingTouch(MySeekBar VerticalSeekBar) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onProgressChanged(MySeekBar VerticalSeekBar, int progress, boolean fromUser) {
		// TODO Auto-generated method stub
		switch(VerticalSeekBar.getId())
		{
		case R.id.seekBar1:
			break;
		case R.id.seekBar2:
			break;
		case R.id.seekBar3:
			break;
		case R.id.seekBar4:
			break;
		case R.id.seekBar5:
			break;
		case R.id.seekBar6:
			break;
			default:
				break;
		}
	}
};

private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			
			switch(v.getId())
			{
			case R.id.LeftConLeftUp:
				RunningCommand(mControlCommand.mLeftUpCommand,event);
				break;
			case R.id.LeftConUp:
				RunningCommand(mControlCommand.mUpCommand,event);
				break;
			case R.id.LeftConRightUp:
				RunningCommand(mControlCommand.mRightUpCommand,event);
				break;
			case R.id.LeftConLeft:
				RunningCommand(mControlCommand.mLeftCommand,event);
				break;
			case R.id.LeftConRight:
				RunningCommand(mControlCommand.mRightCommand,event);
				break;
			case R.id.LeftConLeftDown:
				RunningCommand(mControlCommand.mLeftDownCommand,event);
				break;
			case R.id.LeftConDown:
				RunningCommand(mControlCommand.mDownCommand,event);
				break;
			case R.id.LeftConRightDown:
				RunningCommand(mControlCommand.mRightDownCommand,event);
				break;
			
				default:
					break;
			}
			return true;
		}
};

private View.OnClickListener mOncClickListener = new View.OnClickListener() {
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		float scals = (float) 0.0;
		int SeekBarValue = 0;
		switch(v.getId())
		{
		case R.id.AlarmImage:
			if(mAlarmStart){
				sendData(COMMAND_ALARM_OFF);
				mAlarmStart = false;
				mAlarmImage.setImageResource(R.drawable.speakeroff);
			}else
			{
				sendData(COMMAND_ALARM_ON);
				mAlarmStart = true;
				mAlarmImage.setImageResource(R.drawable.speakeron);
			}
			break;
		case R.id.RorchImage:
			if(mRorchStart){
				sendData(COMMAND_LED_OFF);
				mRorchStart = false;
				mRorchImage.setImageResource(R.drawable.off);
			}else
			{
				sendData(COMMAND_LED_ON);
				mRorchStart = true;
				mRorchImage.setImageResource(R.drawable.on);
			}
			break;
		case R.id.RightConUp:
			SeekBarValue = mSteerSeekBar1.getProgress();
			if(SeekBarValue >= mSteerSeekBar1.getMax())
			{
				break;
			}
			SeekBarValue++;
			_SendSteerValue((byte)1,(byte)SeekBarValue);
			mSteerSeekBar1.setPressed(true);
			mSteerSeekBar1.setProgress(SeekBarValue);
			scals = (float) (SeekBarValue/180.00);
			mSteerSeekBar1.onProgressRefresh(scals, true);
			
			break;
		case R.id.RightConLeft:
			SeekBarValue = mSteerSeekBar2.getProgress();
			if(SeekBarValue > mSteerSeekBar2.getMax())
			{
				break;
			}
			SeekBarValue++;
			mSteerSeekBar2.setPressed(true);
			mSteerSeekBar2.setProgress(SeekBarValue);
			//mSteerSeekBar2.setThumbOffset((mSteerSeekBar1.getHeight()/180)*SeekBarValue);
			scals = (float) (SeekBarValue/180.00);
			mSteerSeekBar2.onProgressRefresh(scals, true);
			_SendSteerValue((byte)2,(byte)SeekBarValue);
			break;
		case R.id.RightConRight:
			SeekBarValue = mSteerSeekBar2.getProgress();
			if(SeekBarValue <= 0){
				break;
			}
			SeekBarValue--;
			mSteerSeekBar2.setPressed(true);
			mSteerSeekBar2.setProgress(SeekBarValue);
			scals = (float) (SeekBarValue/180.00);
			mSteerSeekBar2.onProgressRefresh(scals, true);
			_SendSteerValue((byte)2,(byte)SeekBarValue);
			
			break;
		case R.id.RightConDown:
			SeekBarValue = mSteerSeekBar1.getProgress();
			if(SeekBarValue < 0){
				break;
			}
			SeekBarValue--;
			mSteerSeekBar1.setPressed(true);
			mSteerSeekBar1.setProgress(SeekBarValue);
			scals = (float) (SeekBarValue/180.00);
			mSteerSeekBar1.onProgressRefresh(scals, true);
			_SendSteerValue((byte)1,(byte)SeekBarValue);
			break;
			default:
				break;
		}
	}
};
	
//
private void RunningCommand(String Command,MotionEvent event){
	//按下按键
	if(event.getAction() == MotionEvent.ACTION_DOWN){
		sendData(Command);
	}else if(event.getAction() == MotionEvent.ACTION_UP){
		sendData(RUNNING_STOP);
	}
}

class MessageHandler extends Handler {
	public MessageHandler(Looper looper) {
			super(looper);
		}
	public void handleMessage(Message msg) {
		switch (msg.arg1) {
			case 3:
				if(msg.obj != null){
					VedioViewPic.setImageBitmap((Bitmap)msg.obj);
				}
				//VedioViewPic.setScaleType(ScaleType.FIT_XY);
			break;
			case 1:
				break;
			case 2:
				Toast.makeText(mContext, "无法连接上服务器!", Toast.LENGTH_SHORT).show(); 
				break;
			default:
				break;
		}

	}
}

private  byte[] hexStringToByte(String hex) {   
    int len = (hex.length() / 2);   
    byte[] result = new byte[len];   
    char[] achar = hex.toCharArray();   
    for (int i = 0; i < len; i++) {   
     int pos = i * 2;   
     result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));   
    }   
    return result;   
}  
  
private  byte toByte(char c) {   
    byte b = (byte) "0123456789ABCDEF".indexOf(c);   
    return b;   
}  

/**
 * 给小车发送指令,
 * 
 * @param orderStr
 *            发"a",则单片机接收到"a"
 * @param tips
 *            提示
 */

private void sendData(String orderStr) {
	
	if(isConnect)
	{
		byte[] SendByte = hexStringToByte(orderStr);
//		char[] SendChar= new char[SendByte.length];
//		for(int i = 0; i <SendByte.length; i++)
//		{
//			SendChar[i] = (char)SendByte[i];
//		}
////		printWriter.print(SendStr);
//		printWriter.write(SendChar);
//		printWriter.flush();
		try {
			mDataOutputStream.write(SendByte);
			mDataOutputStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// if(tips != null && !"".equals(tips)){
	// Tools.tips(Ctr.this, tips);
	// }

}

private void startControl(){
	thread = new Thread(runnable);
	thread.start();
}

// ============================================
// 线程mRunnable启动
private Runnable runnable = new Runnable() {
	public void run() {
		int LocalNumber = Integer.parseInt(mControlCommand.mControlPort);
		String AddressIP = mControlCommand.mControlAddr;
		try {
			// 连接服务器
			socket = new Socket(AddressIP, LocalNumber); //小车ip,端口
			// //取得输入、输出流
			// bufferedReader = new BufferedReader(new
			// InputStreamReader(socket.getInputStream()));
			isConnect = true;
			//printWriter = new PrintWriter(socket.getOutputStream(), true);// 根据新建的sock建立
			mDataOutputStream = new DataOutputStream(socket.getOutputStream()); 
//			OutputStream os = socket.getOutputStream();
//			printWriter = new PrintWriter(os, true);// 第二个参数是是否自动flush,printWriter方法可以在建立后,用print("XXX")来输出
		} catch (Exception e) {
//			Tools.tips(Ctr.this, "连接错误,请检查网络");
			isConnect = false;
			return;
		}
	}
};

/*
private void sendData(String str)
{
	DatagramSocket socketUDP=null;
	int RemoteNumber = Integer.parseInt(mControlCommand.mControlPort);
	
	int LocalNumber = Integer.parseInt(mControlCommand.mControlPort);
	String AddressIP = mControlCommand.mControlAddr;
	 try{
	         socketUDP = new DatagramSocket(LocalNumber);
	      } catch (Exception e) {
	 // TODO Auto-generated catch block
	  e.printStackTrace();}			
		try {
			      		    
		    InetAddress serverAddress = InetAddress.getByName(AddressIP);	 
		    Log.i("aleyds", "Command :" + str);
		    byte data[] = str.getBytes();//hexStringToByte(str); 
		    DatagramPacket packetS = new DatagramPacket(data,
		    		data.length,serverAddress,RemoteNumber);	
		    //从本地端口给指定IP的远程端口发数据包
		    socketUDP.send(packetS);
		    } catch (Exception e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
		   socketUDP.close(); 
}
*/
	
public void ServerConnect() {
		if(isStop){
			isStop = false;
			//conBtn.setText("Clo");
			setTitle("连接"); 
			new Thread() {
	            @SuppressWarnings("unchecked")
				public void run() {
	            	try {
						URL url =new URL(mControlCommand.mVideoAddr);
						
						Socket server = new Socket(url.getHost(), url.getPort());
						OutputStream os = server.getOutputStream();
						InputStream  is = server.getInputStream();
						
						StringBuffer request = new StringBuffer();
						request.append("GET " + url.getFile() + " HTTP/1.0\r\n");
						request.append("Host: " + url.getHost() + "\r\n");
						request.append("\r\n");
						os.write(request.toString().getBytes(), 0, request.length());

						StreamSplit localStreamSplit = new StreamSplit(new DataInputStream(new BufferedInputStream(is)));
						Hashtable localHashtable = localStreamSplit.readHeaders();
						
						String str3 = (String)localHashtable.get("content-type");
						int n = str3.indexOf("boundary=");
						Object localObject2 = "--";
						if (n != -1){
							localObject2 = str3.substring(n + 9);
							str3 = str3.substring(0, n);
							if (!((String)localObject2).startsWith("--"))
								localObject2 = "--" + (String)localObject2;
						}
						if (str3.startsWith("multipart/x-mixed-replace")){
							localStreamSplit.skipToBoundary((String)localObject2);
						}
						Message message1 = Message.obtain();
						message1.arg1 = 1;
					    messageHandler.sendMessage(message1);
						do{
							if (localObject2 != null){
								localHashtable = localStreamSplit.readHeaders();
								if (localStreamSplit.isAtStreamEnd())
									break;
								str3 = (String)localHashtable.get("content-type");
								if (str3 == null)
									throw new Exception("No part content type");
							}
							if (str3.startsWith("multipart/x-mixed-replace")){
								n = str3.indexOf("boundary=");
								localObject2 = str3.substring(n + 9);
								localStreamSplit.skipToBoundary((String)localObject2);
							}else{
								byte[] localObject3 = localStreamSplit.readToBoundary((String)localObject2);
								if (localObject3.length == 0)
									break;
								
								Message message = new Message();
								message.arg1 = 3;
							    message.obj = BitmapFactory.decodeByteArray(localObject3, 0, localObject3.length);
							    messageHandler.sendMessage(message);
							}
						    try{
						      Thread.sleep(10);
						    }catch (InterruptedException localInterruptedException){
						    	
						    }
						}while (!isStop);
						server.close();
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("错误");
						//Toast.makeText(mContext, "无法连接上服务器!", Toast.LENGTH_SHORT).show(); 
						Message message = Message.obtain();
						//VedioViewPic.setBackgroundResource(R.drawable.img_main_background);
						isConnectServer=false;
						message.arg1 = 2;
					    messageHandler.sendMessage(message);
					}
	            }
			}.start();
		}else{
			isStop = true;
			//conBtn.setText("Con");
			isConnectServer=false;
			setTitle("断开"); 
		}
	} 
	
}
