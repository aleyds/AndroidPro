package com.alcohol.subActivity;

import java.util.ArrayList;

import com.alcohol.activety.R;
import com.alcohol.activity.MainActivity;
import com.alcohol.adapter.GuirdPageAdapter;
import com.alcohol.db.UserInfo;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class ViewGuidePage {
	
	public static final String GUIRD_PAGE_ENTER = "GuirdEnter";
	
	private Activity mActivity;
	
	private ViewPager mViewPager;
	private ImageView GuidePage0;
	private ImageView GuidePage1;
	private ImageView GuidePage2;
	private ImageView GuidePage3;
	private ArrayList<ImageView> mImageList= new ArrayList<ImageView>(); 
	
	private Button mEnterButton1 ;
	private Button mEnterButton2 ;
	private Button mEnterButton3 ;
	private Button mEnterButton4 ;
	private GuirdPageAdapter mGuirdPageAdapter;
	
	
	public ViewGuidePage(Activity GuideAct){
		mActivity = GuideAct;
	}
	
	private void startClick(){
//		UserInfo mUserInfo = new UserInfo(mActivity);
//		String UserName = mUserInfo.getUserName();
//		String UserPwd = mUserInfo.getUserPwd();
		
		Intent intent = new Intent();
		intent.setClass(mActivity, MainActivity.class);
		intent.putExtra(GUIRD_PAGE_ENTER, true);
		mActivity.startActivity(intent);
		
		mActivity.finish();
		
		
	}
	
	private View.OnClickListener mClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			startClick();
		}
	};
	
	
	private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
		
		@Override
		public void onPageSelected(int pos) {
			// TODO Auto-generated method stub
			setPagePoint(pos);
		}
		
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onPageScrollStateChanged(int arg0) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private void setPagePoint(int pos){
		int index = 0;
		for (ImageView imageView : mImageList) {
			if(index == pos){
				imageView.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.page_indicator_focused));
			}else{
				imageView.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.page_indicator_unfocused));
			}
			index++;
		}
	}
	
	public void showGuidePage(){
		mActivity.setContentView(R.layout.view_guirdpagers);
		mViewPager = (ViewPager)mActivity.findViewById(R.id.viewpager);
		GuidePage0 = (ImageView)mActivity.findViewById(R.id.guideTipPoint0);
		GuidePage1 = (ImageView)mActivity.findViewById(R.id.guideTipPoint1);
		GuidePage2 = (ImageView)mActivity.findViewById(R.id.guideTipPoint2);
		GuidePage3 = (ImageView)mActivity.findViewById(R.id.guideTipPoint3);
		mImageList.add(GuidePage0);
		mImageList.add(GuidePage1);
		mImageList.add(GuidePage2);
		mImageList.add(GuidePage3);
		
		GuidePage0.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.page_indicator_focused));
		GuidePage1.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.page_indicator_unfocused));
		GuidePage2.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.page_indicator_unfocused));
		GuidePage3.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.page_indicator_unfocused));
		
	    LayoutInflater mLi = LayoutInflater.from(mActivity);  
	    View view1 = mLi.inflate(R.layout.view_guirdpage1, null);  
	    View view2 = mLi.inflate(R.layout.view_guirdpage2, null);  
	    View view3 = mLi.inflate(R.layout.view_guirdpage3, null);  
	    View view4 = mLi.inflate(R.layout.view_guirdpage4, null);
	    
	    mEnterButton1 = (Button) view1.findViewById(R.id.buttonEnterApp1);
	    mEnterButton2 = (Button) view2.findViewById(R.id.buttonEnterApp2);
	    mEnterButton3 = (Button) view3.findViewById(R.id.buttonEnterApp3);
	    mEnterButton4 = (Button) view4.findViewById(R.id.buttonEnterApp4);
	    mEnterButton1.setOnClickListener(mClickListener);
	    mEnterButton2.setOnClickListener(mClickListener);
	    mEnterButton3.setOnClickListener(mClickListener);
	    mEnterButton4.setOnClickListener(mClickListener);
	    
	    mGuirdPageAdapter = new GuirdPageAdapter();
	    mGuirdPageAdapter._AddView(view1);
	    mGuirdPageAdapter._AddView(view2);
	    mGuirdPageAdapter._AddView(view3);
	    mGuirdPageAdapter._AddView(view4);
	    mViewPager.setOnPageChangeListener(mOnPageChangeListener);
	    mViewPager.setAdapter(mGuirdPageAdapter);
	}
}
