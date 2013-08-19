package com.mce.uitl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ErrorLogUtil {
	
	public static final String SYSTEMINIT_ERROR_CODE = "E001" ; //ϵͳ��ʼ������
	
	public static final String NETWORK_ERROR_CODE = "E002" ; //�������
	
	public static final String MESSAGE_ERROR_CODE = "E003" ; //���Ľ�������

	public static final String DB_ERROR_CODE = "E004" ; //���ݿ�������
	
	public static final String RUNTIME_ERROR_CODE = "E005" ; //����ʱ����
	
	@SuppressWarnings("unchecked")
	public  static Map getErrorInfoMap(String typecode ,String exception ,String exceptiondesc ,String memo )
	{
		
		Map map = new HashMap() ;
		
		map.put("objuid", MD5Util.getObjuid()) ;
		map.put("typecode", typecode);
		map.put("exception", exception) ;
		map.put("exceptiondesc", exceptiondesc);
		map.put("time", new Date()) ;
		if (memo != null)
		{
			map.put("memo", memo) ;
		}
		map.put("throwtimes", 1) ;
		
		return map ;
	}
	
}
