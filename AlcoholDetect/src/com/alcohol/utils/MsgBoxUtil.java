package com.alcohol.utils;

import android.app.Activity;
import android.widget.Toast;

public class MsgBoxUtil {
	
	private static Toast mToast;
	public static void showCustomToast(Activity context, String msg){
		if(null == context || null == msg) {
			return;
		}
		if(null != mToast) {
			mToast.cancel();
			mToast = null;
		}
		mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
		mToast.show();
	}
	public static void showCustomToast(Activity context, int resid){
		if(null == context) {
			return;
		}
		if(null != mToast) {
			mToast.cancel();
			mToast = null;
		}
		mToast = Toast.makeText(context, resid, Toast.LENGTH_SHORT);
		mToast.show();
	}
	
	public static void showCustomToast(Activity context, int resid, int duration) {
		if(null == context) {
			return;
		}
		if(null != mToast) {
			mToast.cancel();
			mToast = null;
		}
		mToast = Toast.makeText(context, resid, duration);
		mToast.show();
	}
	
//	public static void showNetworkUnavailableToast(Activity context) {
//		showCustomToast(context, R.string.cn_ura_network_unavailable);
//	}
	
	public static void clearToast() {
		if(null != mToast) {
			mToast.cancel();
			mToast = null;
		}
	}
	
	/*public static AlertDialog ShowAlert(Context context, String title, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.MyDialogStyle);
		builder.setMessage(msg).setTitle(title);
		AlertDialog msgbox = builder.create();
		msgbox.setCanceledOnTouchOutside(true);
		msgbox.show();
		return msgbox;
	}
	*/
	
//	public static M2MPromptNoButtonDialog ShowAlert(Context context, String title, String msg) {
//		Builder builder = new  M2MPromptNoButtonDialog.Builder(context, R.style.MyDialogStyle);
//		builder.setMessage(msg).setTitle(title);
//		M2MPromptNoButtonDialog msgbox = builder.create();
//		msgbox.setCanceledOnTouchOutside(true);
//		msgbox.show();
//		
//		return msgbox;
//	}
	
//	public static M2MPromptNoButtonDialog ShowAlert(Context context, int title_resid, int msg_resid) {
//		M2MPromptNoButtonDialog.Builder builder = new  M2MPromptNoButtonDialog.Builder(context, R.style.MyDialogStyle);
//		builder.setMessage(context.getResources().getString(msg_resid)).setTitle(title_resid);
//		M2MPromptNoButtonDialog msgbox = builder.create();
//		msgbox.setCanceledOnTouchOutside(true);
//		msgbox.show();
//		
//		return msgbox;
//	}
/*	public static AlertDialog ShowAlert(Context context, int title_resid, int msg_resid) {
		AlertDialog.Builder builder = new  AlertDialog.Builder(context, R.style.MyDialogStyle);
		builder.setMessage(context.getResources().getString(msg_resid)).setTitle(title_resid);
		AlertDialog msgbox = builder.create();
		msgbox.setCanceledOnTouchOutside(true);
		msgbox.show();
		
		return msgbox;
	}
	*/
//	public static void ShowPromptDialog(Context context, int title_resid, int msg_resid){
//		M2MPromptDialog.Builder dialog=new M2MPromptDialog.Builder(context, R.style.Dialog);
//		dialog.setTitle(title_resid);
//		dialog.setMessage(msg_resid);
//		M2MPromptDialog msgbox=dialog.create();
//		msgbox.setCanceledOnTouchOutside(true);
//		msgbox.show();
//		
//	}
//	public static void ShowPrompNoBtntDialog(Context context, int title_resid, int msg_resid){
//		M2MPromptNoButtonDialog.Builder dialog=new M2MPromptNoButtonDialog.Builder(context, R.style.Dialog);
//		dialog.setTitle(title_resid);
//		dialog.setMessage(msg_resid);
//		M2MPromptNoButtonDialog msgbox=dialog.create();
//		msgbox.setCanceledOnTouchOutside(true);
//		msgbox.show();
//		
//	}
	
//	public static void ShowConfirmDialog(Context context, int title_resid, int msg_resid,
//											OnClickListener positive, OnClickListener negtive) {
//		AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.MyDialogStyle);
//		dialog.setTitle(title_resid);
//		dialog.setMessage(context.getResources().getString(msg_resid));
//		dialog.setPositiveButton(R.string.cn_ok, positive);
//		dialog.setNegativeButton(R.string.cn_cancel, negtive);
//		AlertDialog msgbox = dialog.create();
//		msgbox.setCanceledOnTouchOutside(false);
//		msgbox.show();
//	}
//	public static void ShowConfirmDialog(Context context, int title_resid, int msg_resid,
//			OnClickListener positive, OnClickListener negtive) {
//		M2MPromptDialog.Builder dialog = new M2MPromptDialog.Builder(context, R.style.MyDialogStyle);
//       dialog.setTitle(title_resid);
//       dialog.setMessage(context.getResources().getString(msg_resid));
//       dialog.setPositiveButton(R.string.cn_ok, positive);
//       dialog.setNegativeButton(R.string.cn_cancel, negtive);
//       M2MPromptDialog msgbox = dialog.create();
//       msgbox.setCanceledOnTouchOutside(true);
//       msgbox.show();
//    }
//	public static void ShowConfirmDialog(Context context, int title_resid, int msg_resid,
//											OnClickListener positive) {
//		ShowConfirmDialog(context, title_resid, msg_resid, positive, null);
//	}
	
//	public static void ShowConfirmDialog(Context context, int title_resid, int msg_resid,
//			OnClickListener positive, OnClickListener negtive, int positive_title,
//			int negtive_title) {
//		M2MPromptDialog.Builder dialog = new M2MPromptDialog.Builder(context, R.style.MyDialogStyle);
//		dialog.setTitle(title_resid);
//		dialog.setMessage(context.getResources().getString(msg_resid));
//		dialog.setPositiveButton(positive_title, positive);
//		dialog.setNegativeButton(negtive_title, negtive);
//		M2MPromptDialog msgbox = dialog.create();
//		msgbox.setCanceledOnTouchOutside(false);
//		msgbox.show();
//	}

}
