package com.m2mkey.stcontrol;

import android.util.Log;

public class M2MCmdManager {

	/**
	 * 指令发送间隔，对于需要写入日志的指令，若指令间隔小于2s时，会导致设备端的日志文件被清除，
	 * 顾设置该时间间隔，保证设备端日志顺利写入
	 */
	private static final long LOG_CMD_INTERVAL = 2000;
	/**
	 * 记录上一次指令发送的时间
	 */
	private long mLastCmdSendTime = 0;
	/**
	 * 记录上一次需要设备端写日志的指令的发送时间
	 */
	private long mLastLogCmdSendTime = 0;
	/**
	 * 记录上一次发送的指令
	 */
	private int mLastCmd = -1;
	/**
	 * 标志指令是否需要等待设备端返回，若指令需要等待返回，则当返回还没有收到时，下一条指令不能被发送，此时如果调用
	 * 指令发送的接口，onCommunicationError()回调会被触发，错误类型为CMD_SENT_TOO_FAST
	 * true表示需要等待返回
	 * false表示不需要等待返回
	 */
	private boolean mLastCmdNeedFeedback = false;
	/**
	 * 当指令需要设备端返回时，标志返回是否已收到
	 * true表示已收到
	 * false表示未收到
	 */
	private boolean mLastCmdGetFeedback = false;
	
//	/**
//	 * 设置上一次指令发送的时间
//	 * @param time - UNIX时间戳，单位ms
//	 */
//	public void setLastCmdSendTime(long time) {
//		mLastCmdSendTime = time;
//	}
//	
//	/**
//	 * 设置指令是否需要等待设备端返回
//	 * @param need
//	 */
//	public void setCmdNeedFeedback(boolean need_feedback) {
//		mLastCmdNeedFeedback = need_feedback;
//	}
	
	/**
	 * 设置设备端返回是否已收到
	 * @param get_feedback
	 */
	public void setCmdGetFeedback(boolean get_feedback) {
		mLastCmdGetFeedback = get_feedback;
	}
	
	/**
	 * 初始化指令状态
	 */
	public void initCmdStatus() {
		mLastCmdSendTime = 0;
		mLastCmdNeedFeedback = false;
		mLastCmdGetFeedback = false;
	}
	
	/**
	 * 检查当前是否可以向设备端发送指令
	 * @param cmd_type
	 * @return
	 */
	public boolean isCmdSendable(int cmd_type) {
		// 根据上一条指令的状态，判断是否可以发送下一条指令
		if(isLogCmd(cmd_type)
				&& System.currentTimeMillis() - mLastLogCmdSendTime < LOG_CMD_INTERVAL) {
			// 当前指令需要设备端写入日志，但是上一次日志指令的间隔时间小于2s，则不能发送
			return false;
		}
		
		if(mLastCmdNeedFeedback && !mLastCmdGetFeedback) {
			// 如果上一条指令需要设备端返回，但是还没有收到，则不能发送
			return false;
		}
		
		return true;
	}
	
	public void setCmd(int cmd_type) {
		mLastCmd = cmd_type;
		mLastCmdSendTime = System.currentTimeMillis();
		if(isLogCmd(cmd_type)) {
			mLastLogCmdSendTime = mLastCmdSendTime;
		}
		// 设置指令的状态
		switch(cmd_type) {
		// 需要等待设备返回的指令
		case M2MBLEController.MSG_GET_STATUS: // 获取设备的状态
		case M2MBLEController.MSG_UNLOCK: // 开锁指令
		case M2MBLEController.MSG_AUTOLOCK_DISABLE: // 切换到办公室模式
		case M2MBLEController.MSG_AUTOLOCK_ENABLE: // 切换到家庭模式
		case M2MBLEController.MSG_ALARM_ON: // 开警报
		case M2MBLEController.MSG_ALARM_OFF: // 
		case M2MBLEController.MSG_IKEY_ADD_MODE: //
		case M2MBLEController.MSG_IKEY_RUN_MODE: //
		case M2MBLEController.MSG_IKEY_DELETE_ROMID: //
		case M2MBLEController.MSG_IKEY_DELETE_ALL:
		case M2MBLEController.MSG_MKEY_ADD:
		case M2MBLEController.MSG_MKEY_DEL:
		case M2MBLEController.MSG_LOG_READ:
		case M2MBLEController.MSG_RTC_SYNC:
		case M2MBLEController.MSG_LTK_VERIFY:
		case M2MBLEController.MSG_LTK_WRITE:
		case M2MBLEController.MSG_MKEY_DELALL:
		case M2MBLEController.MSG_FINGER_RECORD:
		case M2MBLEController.MSG_FINGER_DELETE:
		case M2MBLEController.MSG_FINGER_RESET:
		case M2MBLEController.RECV_MSG_SET_PERMANENT_COMB:
		case M2MBLEController.MSG_WIFI_SSID:
		case M2MBLEController.MSG_WIFI_PWD:
		case M2MBLEController.MSG_REMOTE_CONF_VERIF_CODE:
		case M2MBLEController.MSG_CTL_WIFI_OFF:
		case M2MBLEController.MSG_CTL_WIFI_ON:
		case M2MBLEController.MSG_CTL_WIFI_STATE:
		case M2MBLEController.RECV_MSG_UPDATE_ONETIME_COMBREPO:
		case M2MBLEController.RECV_MSG_GET_ONETIME_COMBREPO:
			mLastCmdNeedFeedback = true;
			mLastCmdGetFeedback = false;
			break;
		default:
			mLastCmdNeedFeedback = false;
			break;
		}
	}
	
	/**
	 * 检查指令是否需要设备记录日志，这种类型的指令我们称之为“日志指令”
	 * @param cmd_type
	 * @return
	 */
	private boolean isLogCmd(int cmd_type) {
		boolean log_cmd = false;
		switch(cmd_type) {
		case M2MBLEController.MSG_UNLOCK:
		case M2MBLEController.MSG_ALARM_ON:
		case M2MBLEController.MSG_ALARM_OFF:
		case M2MBLEController.MSG_AUTOLOCK_ENABLE:
		case M2MBLEController.MSG_AUTOLOCK_DISABLE:
		case M2MBLEController.MSG_RTC_SYNC:
			log_cmd = true;
			break;
		}
		
		return log_cmd;
	}

}
