package com.mce.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.ModelSql;
import com.mce.uitl.ErrorLogUtil;

/**
 * 
 * 使用推立方 发送告警信息到指定手机号
 * 
 * @author wanghongliang
 * 
 */
public class SendSMSAction {
	private final static Logger log = Logger.getLogger(SendSMSAction.class) ;
	public static void sendMessage(String numberList, String content)
	{
		DatabaseOperator db = new DatabaseOperator() ;
//		String messgaeTitle = SystemConfiguration.getProperty("messagetitle").toString();
//		if ( messgaeTitle.contains(":"))
//		{
//			content = messgaeTitle.replace("(.*)", " " + content) ;
//		}
//		else
//		{
//			content = messgaeTitle.replace("(.*)", ": " + content) ;
//		}
	//	content = SystemConfiguration.getProperty("messagetitle").toString().replace("(.*)", content) ;
		try {
			HttpClient client = new HttpClient();
			HttpMethod method;
			method = new GetMethod(
					"http://www.tui3.com/api/send/?k=d5e883e9ea7227a098722bd521b3711d&p=1&t="
							+ numberList
							+ "&c="
							+ java.net.URLEncoder
									.encode(
											//SystemConfiguration.getProperty("messagetitle").toString() + ": " +  content,
										content ,	
										"utf-8"));
			client.executeMethod(method);
			InputStream in = method.getResponseBodyAsStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in,
					"utf-8"));
			String tmp = "";
			while ((tmp = br.readLine()) != null) {
				log.info("短信发送返回报文：" + tmp) ;
				tmp = native2Ascii(tmp ) ;
			}
			if ( tmp != null)
			{
				Map obj = JSON.parseObject(tmp).getJSONObject("result") ;
				if ( obj.get("err_code").toString().equals("0") )
				{
					log.info( "短信发送成功！" ) ;
				}
				else
				{
					log.info( "短信发送失败！！" ) ;
					Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.SENDSMS_ERROR_CODE, "错误码 ：" + obj.get("err_code"), "短信发送失败", "执行位置：SendSMSAction" + "错误信息：" + obj.get("err_msg"));
					db.saveErrorLog(datamap);
				}
			}
			method.releaseConnection();
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			log.error(e); 
			
			Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.SENDSMS_ERROR_CODE, e.getMessage(), "短信发送错误", "执行位置：SendSMSAction" + e.getMessage());
			db.saveErrorLog(datamap);
		} catch (IOException e) {
			log.error(e); 
			Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.SENDSMS_ERROR_CODE, e.getMessage(), "短信发送错误", "执行位置：SendSMSAction" + e.getMessage());
			db.saveErrorLog(datamap);
		}

	}
	private static String PREFIX = "\\u";
	public static String native2Ascii(String str) {
		char[] chars = str.toCharArray();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < chars.length; i++) {
			sb.append(char2Ascii(chars[i]));
		}
		return sb.toString();
	}
	
	private static String char2Ascii(char c) {
		if (c > 255) {
			StringBuilder sb = new StringBuilder();
			sb.append(PREFIX);
			int code = (c >> 8);
			String tmp = Integer.toHexString(code);
			if (tmp.length() == 1) {
				sb.append("0");
			}
			sb.append(tmp);
			code = (c & 0xFF);
			tmp = Integer.toHexString(code);
			if (tmp.length() == 1) {
				sb.append("0");
			}
			sb.append(tmp);
			return sb.toString();
		} else {
			return Character.toString(c);
		}
	}
	
	public static void main(String[] argvs)
	{
		String s = "ECS环境控制系统提示您：设备告警: @1 。序列号 ：@2 ，环控点名称：@3 ，谢谢！"; 
		s = s.replace("@1", "nb1");
		s = s.replace("@2", "nb2");
		s = s.replace("@3", "nb3");
		//System.out.println(s);
		//SystemConfiguration.reload() ;
		SendSMSAction.sendMessage("18647132049", s) ;
	}

}
