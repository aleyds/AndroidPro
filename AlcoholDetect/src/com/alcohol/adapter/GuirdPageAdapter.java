package com.alcohol.adapter;

import java.util.ArrayList;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

public class GuirdPageAdapter extends PagerAdapter{

	private ArrayList<View> mviews = new ArrayList<View>();
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mviews.size();
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		// TODO Auto-generated method stub
		return arg0==arg1;
	}
	
	@Override
	public void destroyItem(View container, int position, Object object) {
		((ViewPager) container).removeView(mviews.get(position));
	}

	@Override
	public Object instantiateItem(View container, int position) {
		((ViewPager) container).addView(mviews.get(position));
		return mviews.get(position);
	}
	
	public void _AddView(View mview){
		if(null != mview){
			mviews.add(mview);
		}
	}
	

}
