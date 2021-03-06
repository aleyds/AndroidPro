package com.alcohol.activity;

import android.os.Bundle;

import com.alcohol.activety.R;
import com.alcohol.db.AppStartParam;
import com.alcohol.subActivity.ViewGuidePage;

import android.app.Activity;
import android.view.Menu;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class StartActivity extends Activity {

	private final String TAG = "UI-StartActivity";
	private ImageView mStartImageView;
	private Animation mAnimation;
	
	private void WedgitInit(){
		mStartImageView = (ImageView)findViewById(R.id.StartImageView);
		mAnimation = AnimationUtils.loadAnimation(this, R.anim.start_animaton);
		mStartImageView.startAnimation(mAnimation);
		
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		WedgitInit();
	}
	
	

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mAnimation.setAnimationListener(mAnimationListener);
	}

	
	private AnimationListener mAnimationListener = new Animation.AnimationListener() {
		
		@Override
		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onAnimationEnd(Animation animation) {
			// TODO Auto-generated method stub
			AppStartParam mAppStartParam = new AppStartParam(StartActivity.this);
			if(mAppStartParam.isFirstStart()){
				mAppStartParam.saveFirstStart(false);
				ViewGuidePage Gurid = new ViewGuidePage(StartActivity.this);
				Gurid.showGuidePage();
			}else{
				ViewGuidePage Gurid = new ViewGuidePage(StartActivity.this);
				Gurid.showGuidePage();
			}
		}
	};
}
