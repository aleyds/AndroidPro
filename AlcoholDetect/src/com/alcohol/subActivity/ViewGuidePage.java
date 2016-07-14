package com.alcohol.subActivity;

import com.alcohol.activety.R;
import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.widget.ImageView;

public class ViewGuidePage {
	
	private Activity mActivity;
	
	private ViewPager mViewPager;
	private ImageView GuidePage0;
	private ImageView GuidePage1;
	private ImageView GuidePage2;
	private ImageView GuidePage3;
	
	
	public ViewGuidePage(Activity GuideAct){
		mActivity = GuideAct;
	}
	
	public void showGuidePage(){
		mActivity.setContentView(R.layout.view_guirdpagers);
		mViewPager = (ViewPager)mActivity.findViewById(R.id.viewpager);
		GuidePage0 = (ImageView)mActivity.findViewById(R.id.guideTipPoint0);
		GuidePage1 = (ImageView)mActivity.findViewById(R.id.guideTipPoint1);
		GuidePage2 = (ImageView)mActivity.findViewById(R.id.guideTipPoint2);
		GuidePage3 = (ImageView)mActivity.findViewById(R.id.guideTipPoint3);
	}
}
