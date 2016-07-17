package com.m2mkey.stcontrol;

import android.util.Log;

import com.m2mkey.stcontrol.M2MBLEController.CmdType;


public class M2MLog {
	public final static String TAG = "SLog";
	public int mLogId;
	public long mTimeStp;
	public int mKeyId; // mkey: 1-21 ikey: 129-158
	public String mKeyUuid; // 根据mKey LTK或sKey ROM ID生成
	public int mFingerprintId; // fingerprint ID
	public int mUserPasswdId; // user password ID
	public CmdType mCmdType;
	/**
	 * description of mKeyId, usually it is the user name
	 */
	public String mKeyComment;
	/**
	 * description of mCmdType
	 */
	public String mCmdComment;
	/**
	 * original content of the log
	 */
	public byte[] mOrigLog;
	
	private static final int MSG_UNLOCK = 2;
	private static final int MSG_AUTOLOCK_DISABLE = 3; // office mode
	private static final int MSG_AUTOLOCK_ENABLE = 4; // home mode
	private static final int MSG_ALARM_ON = 5;
	private static final int MSG_ALARM_OFF = 6;
	private static final int MSG_RTC_SYNC = 15;
	private static final int MSG_ILLEGAL = 1111;
	private static final int MSG_ILLEGAL_KEY_TRIES = 2222;
	private static final int MSG_UNKNOWN = 1000;
	
	public static int getUnknownCmdType() {
		return MSG_UNKNOWN;
	}
	
	public M2MLog(byte[] log) {
		initLog(log, -1); // just use -1 to indicate that the log can be decoded as normal
	}
	
	/**
	 * for cLock-2, 2 bytes are used to represent key ID
	 * for other models, key ID just needs 1 byte
	 * @param log
	 * @param device_model
	 */
	public M2MLog(byte[] log, int device_model) {
		initLog(log, device_model);
	}
	
	public M2MLog(int log_id, long time, int key_id, String key_uuid, String key_comment, int cmd_id) {
		mLogId = log_id;
		mTimeStp = time;
		mKeyId = key_id;
		mKeyUuid = key_uuid;
		mKeyComment = key_comment;
		mCmdType = getCmdType(cmd_id);
	}
	
	public int getCmdID() {
		if(CmdType.CMDTYPE_UNLOCK == this.mCmdType) {
			return MSG_UNLOCK;
		}
		else if(CmdType.CMDTYPE_ALARMOFF == this.mCmdType) {
			return MSG_ALARM_OFF;
		}
		else if(CmdType.CMDTYPE_ALARMON == this.mCmdType) {
			return MSG_ALARM_ON;
		}
		else if(CmdType.CMDTYPE_ILLEGAL_UNLOCK == this.mCmdType) {
			return MSG_ILLEGAL;
		}
		else if(CmdType.CMDTYPE_ILLEGAL_TRIES == this.mCmdType) {
			return MSG_ILLEGAL_KEY_TRIES;
		}
		else if(CmdType.CMDTYPE_HOMEMODE == this.mCmdType) {
			return MSG_AUTOLOCK_ENABLE;
		}
		else if(CmdType.CMDTYPE_OFFICEMODE == this.mCmdType) {
			return MSG_AUTOLOCK_DISABLE;
		}
		else if(CmdType.CMDTYPE_SYNCTIME == this.mCmdType) {
			return MSG_RTC_SYNC;
		}
		else {
			return MSG_UNKNOWN;
		}
	}
	
	private void initLog(byte[] log, int device_model) {
		// first 2 bytes represent log id
		/*
		 * By default, JAVA treat byte as singed value, we use '& 0xFF' to get its unsigned value
		 * Reference: http://blog.csdn.net/defonds/article/details/8782785
		 */
		this.mLogId = (int)(log[0] & 0xFF | (log[1] & 0xFF) << 8);
		
		// second 4 bytes represent time stamp
		this.mTimeStp = (long)(log[2] & 0xFF | (log[3] & 0xFF) << 8
						| (log[4] & 0xFF) << 16 | (log[5] & 0xFF) << 24);
		
		// next 1 byte represents mkey/ikey id
		if(M2MBLEDevice.TYPE_CLOCK2 == device_model) {
			Log.d("debug", "log[6]: " + log[6]);
			Log.d("debug", "log[7]: " + log[7]);
			this.mKeyId = (int)(log[6] & 0xFF | (log[7] & 0xFF) << 8);
		}
		else {
			this.mKeyId = (int)(log[6] & 0xFF);
		}
		
		Log.d("debug", "key id: " + this.mKeyId);
		
		// final 2 bytes represent command type
		int cmd = -1;
		if(M2MBLEDevice.TYPE_CLOCK2 == device_model) {
			cmd = (int)(log[8] & 0xFF | (log[9] & 0xFF) << 8);
			this.mCmdType = getCmdType(cmd);
		}
		else {
			cmd = (int)(log[7] & 0xFF | (log[8] & 0xFF) << 8);
			if(M2MLock.FINGERPRINT_MKEY == this.mKeyId) {
				/*
				 * for fingerprint lock, command type is used to record the fingerprint ID,
				 * and for fingerprint, command type only can be unlock
				 */
				this.mFingerprintId = cmd;
				this.mCmdType = CmdType.CMDTYPE_UNLOCK;
				
			}
			else if(M2MLock.COMBINATION_MKEY == this.mKeyId) {
				/*
				 * for password lock, command type is used to record the password ID,
				 * and for user password, command type only can be unlock
				 */
				this.mUserPasswdId = cmd;
				this.mCmdType = CmdType.CMDTYPE_UNLOCK;
			}
			else {
				this.mCmdType = getCmdType(cmd);
			}
		}
		
		mKeyUuid = null;
		
		mOrigLog = log;
	}
	
	private CmdType getCmdType(int cmd_id) {
		CmdType cmd_type = CmdType.CMDTYPE_UNKNOWN;
		switch(cmd_id) {
		case MSG_UNLOCK:
			cmd_type = CmdType.CMDTYPE_UNLOCK;
			break;
		case MSG_AUTOLOCK_DISABLE:
			cmd_type = CmdType.CMDTYPE_OFFICEMODE;
			break;
		case MSG_AUTOLOCK_ENABLE:
			cmd_type = CmdType.CMDTYPE_HOMEMODE;
			break;
		case MSG_ALARM_ON:
			cmd_type = CmdType.CMDTYPE_ALARMON;
			break;
		case MSG_ALARM_OFF:
			cmd_type = CmdType.CMDTYPE_ALARMOFF;
			break;
		case MSG_ILLEGAL:
			cmd_type = CmdType.CMDTYPE_ILLEGAL_UNLOCK;
			break;
		case MSG_ILLEGAL_KEY_TRIES:
			cmd_type = CmdType.CMDTYPE_ILLEGAL_TRIES;
			break;
		case MSG_RTC_SYNC:
			cmd_type = CmdType.CMDTYPE_SYNCTIME;
			break;
		default:
			cmd_type = CmdType.CMDTYPE_UNKNOWN;
			break;
		}
		return cmd_type;
	}
}
