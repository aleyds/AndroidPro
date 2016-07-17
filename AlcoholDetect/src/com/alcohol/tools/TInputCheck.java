package com.alcohol.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TInputCheck {
	
	private static String PASSWORD_PATT = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{8,16}$";
	private static String PHONE_NUMBER_PATT = "^((13[0-9])|(15[^4,//D])|(18[0,5-9]))//d{8}$";

	public static boolean isPwd(String mInput){
		Pattern p = Pattern.compile(PASSWORD_PATT);
		Matcher m  = p.matcher(mInput);
		return m.matches();
	}
	
	public static boolean isPhoneNumber(String mInput){
		Pattern p = Pattern.compile(PHONE_NUMBER_PATT);
		Matcher m  = p.matcher(mInput);
		return m.matches();
	}
}
