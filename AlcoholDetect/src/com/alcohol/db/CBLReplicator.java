package com.alcohol.db;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.couchbase.lite.Database;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.replicator.Replication.ChangeEvent;
import com.couchbase.lite.replicator.Replication.ReplicationStatus;

public class CBLReplicator {
	
	public static abstract class CBLReplicationCallback {
		public abstract void onReplicationFinished();
		public abstract void onReplicationTimeout();
		public abstract void onReplicationFailed();
		public abstract void onProgress(int progress);
	}
	
	private static final String TAG = "CBLReplicator";
	private static final int REPLICATION_OK = 0;
	private static final int REPLICATION_FAILED = 1;
	private static final int REPLICATION_TIMEOUT = 2;
	private static final int REPLICATION_PROGRESS = 3;
	
	private static CBLReplicator Replicator = null;
	
	/**
	 * get single-ton instance of CBLReplicator
	 * @return
	 */
	public static CBLReplicator getReplicator() {
		if(null == Replicator) {
			Replicator = new CBLReplicator();
		}
		return Replicator;
	}
	
	private Database mCouchbase = null;
	private URL mReplicationURL = null;
	private CBLReplicationCallback mCallback = null;
	private long mReplicationTimeout = 50000;
	private boolean mIsRunning = false;
	private Replication mPush = null;
	private Replication mPull = null;
	private ReplicationHandler mReplicationHandler = null;
	private Runnable mReplicationTimeoutCallback = new Runnable() {

		@Override
		public void run() {
			mReplicationHandler.sendEmptyMessage(REPLICATION_TIMEOUT);
		}
		
	};
	
	private static class ReplicationHandler extends Handler {
		WeakReference<CBLReplicator> mRef;
		
		private int mPullTotal, mPullCompeleted, mPushTotal, mPushCompleted;
		
		ReplicationHandler(CBLReplicator replicator) {
			mRef = new WeakReference<CBLReplicator>(replicator);
		}
		
		public void clearProgress() {
			mPushCompleted = 0;
			mPushTotal = 0;
			mPullCompeleted = 0;
			mPullTotal = 0;
		}
		
		public void handleMessage(Message msg) {
			CBLReplicator replicator = mRef.get();
			if(null != replicator) {
				replicator.mReplicationHandler.removeCallbacks(replicator.mReplicationTimeoutCallback);
				replicator.mIsRunning = false;
				if(REPLICATION_OK == msg.what) {
					Log.d("RepTime", "replication time: " + (System.currentTimeMillis() - replicator.mRepStartTime));
					replicator.mCallback.onReplicationFinished();
				}
				else if(REPLICATION_TIMEOUT == msg.what) {
					replicator.mPull.stop();
					replicator.mPush.stop();
					replicator.mPull = null;
					replicator.mPush = null;
					replicator.mCallback.onReplicationTimeout();
				}
				else if(REPLICATION_PROGRESS == msg.what) {
					float total = (float)msg.arg1;
					float completed = (float)msg.arg2;
					int progress = 0;
					if(0 != total) {
						progress = (int)((completed / total) * 100.0);
					}
					replicator.mCallback.onProgress(progress);
				}
				else {
					
					replicator.mCallback.onReplicationFailed();
				}
			}
		}
	}
	
	private CBLReplicator() {
		mReplicationHandler = new ReplicationHandler(this);
	}
	
	/**
	 * set replication database
	 * @param db
	 */
	public void setDatabase(Database db) {
		mCouchbase = db;
	}
	
	/**
	 * set remote URL being replicated to/from
	 * @param url
	 * @return
	 */
	public boolean setReplicationURL(String url) {
		try {
			mReplicationURL = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			mReplicationURL = null;
		}
		if(null == mReplicationURL) {
			return false;
		}
		else {
			return true;
		}
	}
	
	/**
	 * set replication callback
	 * @param cb
	 */
	public void setCallback(CBLReplicationCallback cb) {
		mCallback = cb;
	}
	
	/**
	 * set replication timeout, the default value is 50000ms
	 * @param timeout
	 */
	public void setTimeout(long timeout) {
		mReplicationTimeout = timeout;
	}
	
	private Replication.ChangeListener mReplicationListener = new Replication.ChangeListener() {
		
		@Override
		public void changed(ChangeEvent event) {
			boolean active = (mPull.getStatus() == ReplicationStatus.REPLICATION_ACTIVE)
								|| (mPush.getStatus() == ReplicationStatus.REPLICATION_ACTIVE);
			if(!active) {
				Throwable push_error = mPush.getLastError();
				Throwable pull_error = mPull.getLastError();
				if(null == pull_error && null == push_error) {
					mReplicationHandler.sendEmptyMessage(REPLICATION_OK);
				}
				else {
					mReplicationHandler.sendEmptyMessage(REPLICATION_FAILED);
				}
			}
			else {
				int finished = mPull.getCompletedChangesCount() + mPush.getCompletedChangesCount();
				int total = mPull.getChangesCount() + mPush.getChangesCount();
				Log.d(TAG, "total: " + total + " finished: " + finished);
				Message msg = new Message();
				msg.arg1 = total;
				msg.arg2 = finished;
				msg.what = REPLICATION_PROGRESS;
				mReplicationHandler.sendMessage(msg);
			}
		}
	};
	
	private long mRepStartTime = 0;
	public boolean oneShotSyncStart() {
		if(null == mCouchbase || null == mReplicationURL) {
			Log.e(TAG, "database or url is null");
			return false;
		}
		mIsRunning = true;
		mPull = mCouchbase.createPullReplication(mReplicationURL);
		mPull.setContinuous(false);
		mPush = mCouchbase.createPushReplication(mReplicationURL);
		mPush.setContinuous(false);
		mPush.setFilter(CBL.PUSH_FILTER);
		mReplicationHandler.clearProgress();
		mPull.addChangeListener(mReplicationListener);
		mPush.addChangeListener(mReplicationListener);
		mRepStartTime = System.currentTimeMillis();
		mPull.start();
		mPush.start();
		
		return true;
	}
	
	private boolean mPullFinished = false;
	private boolean mPushFinished = false;
	private Replication.ChangeListener mPullListener = new Replication.ChangeListener() {

		@Override
		public void changed(ChangeEvent event) {
			boolean active = mPull.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE;
			if(!active) {
				mPullFinished = true;
				Log.d(TAG, "Pull Progress: Done");
			}
			else {
				mPullFinished = false;
				Log.d(TAG, "Pull Progress: " + mPull.getCompletedChangesCount() + " / " + mPull.getChangesCount());
			}
		}
	};

	private Replication.ChangeListener mPushListener = new Replication.ChangeListener() {

		@Override
		public void changed(ChangeEvent arg0) {
			boolean active = mPush.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE;
			if(!active) {
				mPushFinished = true;
				Log.d(TAG, "Push Progress: Done");
			}
			else {
				mPushFinished = false;
				Log.d(TAG, "Push Progress: " + mPush.getCompletedChangesCount() + " / " + mPush.getChangesCount());
			}
		}
	};
	
	public boolean startContinuousSync() {
		if(null == mCouchbase || null == mReplicationURL) {
			Log.e(TAG, "database or url is null");
			return false;
		}
		mIsRunning = true;
		mPull = mCouchbase.createPullReplication(mReplicationURL);
		mPull.setContinuous(true);
		mPush = mCouchbase.createPushReplication(mReplicationURL);
		mPush.setContinuous(true);
		mPush.setFilter(CBL.PUSH_FILTER);
		mPull.addChangeListener(mPullListener);
		mPush.addChangeListener(mPushListener);
		mPull.start();
		mPush.start();
//		// start a new thread for database replication
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				Log.d(TAG, "start continuous replication");
//				mPull.start();
//				mPush.start();
//			}
//			
//		}).start();
		return true;
	}
	
	protected boolean terminate() {
		if(null != mReplicationHandler && null != mReplicationTimeoutCallback) {
			mReplicationHandler.removeCallbacks(mReplicationTimeoutCallback);
		}
		if(null != mPull) {
			mPull.stop();
			mPull.removeChangeListener(mPullListener);
			mPull = null;
		}
		if(null != mPush) {
			mPush.stop();
			mPush.removeChangeListener(mPushListener);
			mPush = null;
		}
		mIsRunning = false;
		return true;
	}
	
	public boolean isReplicationFinished() {
		if(mPushFinished && mPullFinished) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean isRunning() {
		return mIsRunning;
	}

}
