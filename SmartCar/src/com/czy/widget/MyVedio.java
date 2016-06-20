package com.czy.widget;

import android.view.SurfaceView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Hashtable;

import com.czy.StreamSplit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;

public class MyVedio extends SurfaceView implements Callback, Runnable{

	private static int screenWidth;
	private static int screenHeight;
	
	private boolean runFlag = false;
	private static SurfaceHolder holder;
	private HttpURLConnection conn;
	private Thread thread;
	private String mImageUrl = "";
	private Context mContext;
	
	
	public MyVedio(Context context, AttributeSet attrs) {
		// TODO Auto-generated constructor stub
		super(context, attrs);
		screenValue();
		holder = this.getHolder();
		holder.addCallback(this);
		mContext = context;
	}
	
	private void screenValue() {
		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
	}
	
	public void SetUrl(String url){
		mImageUrl = url;
	}

	
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.arg1) {
			case 0:
				//VedioViewPic.setImageBitmap((Bitmap)msg.obj);
				//VedioViewPic.setScaleType(ScaleType.FIT_XY);
			break;
			case 1:
				break;
			case 2:
				Toast mTost = Toast.makeText(mContext, "无法连接上视频服务器!", Toast.LENGTH_SHORT); 
				mTost.show();
				
				break;
			default:
				break;
		}
			super.handleMessage(msg);
		}
		
	};

	

	@SuppressWarnings("finally")
	@Override
	public void run() {
		Canvas c;
		Bitmap bmp;
		InputStream is;
		URL videoURL = null;
//		Paint p = new Paint(); // ��������,��ͼ����Բ���Ҫ
		String imageURL = "http://192.168.1.1:8080/?action=stream";
		if(mImageUrl.equals("")){
			imageURL = "http://192.168.1.1:8080/?action=stream";
		}else
		{
			imageURL = mImageUrl;
		}
		try {
			videoURL = new URL(imageURL);
		} catch (Exception e) {
		}
		//��ͼ��������
//		BitmapFactory.Options o = new BitmapFactory.Options();// ����ԭͼ����ֵ
//		o.inPreferredConfig = Bitmap.Config.ARGB_8888;// ������
		if (runFlag) {
			c = null;
			try {
				synchronized (holder) {
					c = holder.lockCanvas();
				 	//conn = (HttpURLConnection)videoURL.openConnection();//�˷�����new HttpURLConnection������connect()
				 	Socket server = new Socket(videoURL.getHost(), videoURL.getPort());
					OutputStream os = server.getOutputStream();
					is = server.getInputStream();
					StringBuffer request = new StringBuffer();
					request.append("GET " + videoURL.getFile() + " HTTP/1.0\r\n");
					request.append("Host: " + videoURL.getHost() + "\r\n");
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
							
							bmp = BitmapFactory.decodeByteArray(localObject3, 0, localObject3.length);
							bmp = Bitmap.createScaledBitmap(bmp, screenWidth,
									screenHeight, true);// 
							c.drawBitmap(bmp, 0, 0, null);
//							Message message = Message.obtain();
//							message.arg1 = 0;
//						    message.obj = BitmapFactory.decodeByteArray(localObject3, 0, localObject3.length);
//						    messageHandler.sendMessage(message);
						}
					    try{
					    	Thread.sleep(30);
					    }catch (InterruptedException localInterruptedException){
					    	
					    }
					}while (runFlag);
					
//					conn.connect();//getInputStream���Զ����ô˷���.�˷���һ������new HttpURLConnection֮��.(new��ʱ��û�з�����������)
//					is = conn.getInputStream(); //��ȡ��
//					bmp = BitmapFactory.decodeStream(is, null, o);
//					bmp = Bitmap.createScaledBitmap(bmp, screenWidth,
//							screenHeight, true);// ��ͼƬ������Ļ�ߴ��������
//					c.drawBitmap(bmp, 0, 0, null);
//					
//					Thread.sleep(30);// ���ʱ��,�������������Ž�������.����Լ�ֱܷ�42��������ͼ��.					
				}
			} catch (Exception e) {
//				System.out.println(e.getMessage());
				
				if(runFlag){
					Message message = Message.obtain();
					message.arg1  = 2;
					mHandler.sendMessage(message);
				}
				
			}finally{
				if(null != c)
				{
					holder.unlockCanvasAndPost(c);// ������ͼ���ύ
					c=null;
				}
				if(conn != null){
					conn.disconnect();
					conn = null;
				}
				if(runFlag){
					Message message = Message.obtain();
					message.arg1  = 2;
					mHandler.sendMessage(message);
				}
			}
		}

	}
	
	public void Draw()
    {       
        Canvas canvas=this.holder.lockCanvas();       
       // canvas.drawColor(Color.GREEN);       
        this.holder.unlockCanvasAndPost(canvas);
    }

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		runFlag = true;
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		runFlag = false;
		if(conn!=null)
		{
			conn.disconnect();
		}
		
		
	}

}
