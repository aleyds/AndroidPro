package com.m2mkey.utils;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class M2MStringUtils {
	
	/**
	 * 十六进制字符串转换为字节数组
	 * @param s
	 * @return
	 */
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		if(len%2 != 0){
			return null;
		}
		
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
	
	final private static char[] HexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * 字节数组转换为十六进制字符串
	 * @param bytes
	 * @return
	 */
	public static String byteArrayToHexString(byte[] bytes) {
		if(bytes == null)
		{
			return null;
		}
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HexArray[v >>> 4];
			hexChars[j * 2 + 1] = HexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	/**
	 * UTF-8字符串转换为十六进制字符串
	 * @param utf8_str
	 * @return
	 */
	public static String utf8Str2HexStr(String utf8_str) {
		String hex_str = "";
		if((utf8_str == null) || utf8_str.isEmpty()){
			return hex_str;
		}
		try {
			byte [] bytes = utf8_str.getBytes("UTF8");
			hex_str = byteArrayToHexString(bytes);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return hex_str;
	}
	
	/**
	 * 十六进制字符串转换为UTF-8字符串
	 * @param hex_str
	 * @return
	 */
	public static String hexStr2Utf8Str(String hex_str) {
		String utf8_str = "";
		if((hex_str == null) || hex_str.equals("")){
			return utf8_str;
		}
		byte [] bytes = hexStringToByteArray(hex_str);
		try {
			utf8_str = new String(bytes, "UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return utf8_str;
	}
	
	/**
	 * partially hide email address, for example, if the original email is "wangjiang@126.com",
	 * this method will return "wa******ng@126.com"
	 * if the length of sub string before "@" is not larger than 4, we just show the first character,
	 * like "w******@126.com"
	 * @param email
	 * @return
	 */
	public static String hidePartialEmail(String email) {
		if(null == email) {
			return null;
		}
		
		int at_idx = email.indexOf("@");
		if(-1 == at_idx) {
			return email;
		}
		String username = email.substring(0, at_idx);
		if(username.length() > 4) {
			username = username.substring(0, 2) + "******" + username.substring(username.length()-2);
		}
		else {
			username = username.substring(0, 1) + "******";
		}
		String domain = email.substring(at_idx);
		
		return username + domain;
	}
	
	/**
	 * 将时间戳转换为日期字符串
	 * @param time_stamp - 时间戳，单位为ms
	 * @param format - 日期格式，例如“yyyy-MM-dd HH:mm:ss”
	 * @return
	 * 格式化的日期字符串
	 */
	public static String GetFormatDateString(long time_stamp, String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(new Date(time_stamp));
	}
	
	/**
	 * 使用默认的时间格式“yyyy-MM-dd HH:mm:ss”，将时间戳转换为日期字符串
	 * @param time_stamp - 时间戳，单位为ms
	 * @return
	 * 格式化的日期字符串
	 */
	public static String GetFormatDateString(long time_stamp) {
		String format = "yyyy-MM-dd HH:mm:ss";
		return GetFormatDateString(time_stamp, format);
	}
	
	public static boolean containsOnlyNumber(String exam) {
		boolean only_contain_number = false;
		if(null != exam) {
			if(exam.matches("[0-9]+")) {
				only_contain_number = true;
			}
		}
		
		return only_contain_number;
	}
}
