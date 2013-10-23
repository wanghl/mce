package com.mce.uitl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class MCEUtil {
	
	/**
	 * 统计给定字符串中某个字符的个数
	 * @param model
	 * @param character
	 * @return
	 */
	public static int getCharCount(String model ,char character)
	{
		int num = 0;
		char[] chars = model.toCharArray();
		for(int i = 0; i < chars.length; i++)
		{
		    if( character == chars[i])
		    {
		       num++;
		    }
		}
		
		return num ;
	}
	
	public static String getCurrentDate() {
		Locale loc = new Locale("zh", "CN");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", loc);
		
		return sdf.format(new Date());
	}
	public static String getCurrentDateAll() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		
		return sdf.format(new Date());
	}
	
	public static String getBeforeDate (String dateValue )
	{
		if ( dateValue == null )
		{
			
			return "null" ;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date ;
		try {
			 date = sdf.parse(dateValue);
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null ;
		}
		
		Calendar c = Calendar.getInstance(); 
		c.setTime(date) ;
		int day=c.get(Calendar.DATE); 
		
		c.set(Calendar.DATE,day-1); 
		String dayBefore=new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()); 
		return dayBefore ;
	}
	
	
	public static Date formatDate(String dstr) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.parse(dstr);
	}
	
	public static boolean dateCompare( Date source ,Date dest)
	{
		Calendar sourceDate = Calendar.getInstance() ;
		Calendar destDate = Calendar.getInstance() ;
		sourceDate.setTime(source);
		destDate.setTime(dest) ;
		
		return sourceDate.after(destDate) ;
		
	}
	
	
	public static  String getBeforeDate (Date dateValue )
	{
		if ( dateValue == null )
		{
			
			return "null" ;
		}
		
		Calendar c = Calendar.getInstance(); 
		c.setTime(dateValue) ;
		int day=c.get(Calendar.DATE); 
		c.set(Calendar.DATE,day-1); 
		String dayBefore=new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()); 
		return dayBefore ;
	}
	
	public static  String getBetweenDate(long date) {

		Date now = new Date();

		long time = (now.getTime() - date);

		long days, hours, minutes, seconds;
		days = time / 86400000;
		time = time - (days * 3600 * 24 * 1000);
		hours = time / 3600000;
		time = time - (hours * 3600 * 1000);
		minutes = time / 60000;
		time = time - (minutes * 60 * 1000);
		seconds = time / 1000;

		return days + "天" + hours + "小时" + minutes + "分钟" + seconds + "秒";

	}
	
	
	public static Long getUTCString() throws ParseException
	{
		return  new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(MCEUtil.getCurrentDateAll()).getTime();

	}

	public static  String createHtml(Map data) {
		StringWriter sw = new StringWriter();
		try {

			Configuration cfg = new Configuration();
			//System.out.println( MCEUtil.class.getResource("/report.ftl").getFile() );
			cfg.setDirectoryForTemplateLoading(new File (MCEUtil.class.getResource("/").getFile())) ;
			Template temp = cfg.getTemplate("report.ftl", "utf-8");
			temp.process(data, sw);
			

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sw.toString() ;
	}
	
	
	public static void main(String[] argvs) throws ParseException
	{
		System.out.println(getUTCString()) ;
	}
}
