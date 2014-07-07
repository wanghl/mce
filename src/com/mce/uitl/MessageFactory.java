package com.mce.uitl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.ModelSql;

public class MessageFactory {
	
	public static final Logger log = Logger.getLogger(MessageFactory.class) ;
	
	@SuppressWarnings("unchecked")
	public static String makeDevice104ParameterMessage(String cluid ,String clname ,JSONObject jsonObject)
	{
		DatabaseOperator db = new DatabaseOperator() ;
		
		Map value = db.execueQuery(ModelSql.get104ControlSql(), new Object[]{cluid}) ;
		
		//null 值替换
		Object key  ;
		for(Iterator it = value.keySet().iterator(); it.hasNext() ;)
		{
			key = it.next() ;
			
			if ( value.get(key) == null )
			{
				value.put(key, 0) ;
			}
		}
		
		JSONObject paras = jsonObject.getJSONObject("retval") ;
		if ( clname.equals("wendu"))
		{
			paras.getJSONObject("auto_control").getJSONObject("wendu").put("high", Integer.parseInt(value.get("wenduhigh").toString()));
			paras.getJSONObject("auto_control").getJSONObject("wendu").put("low", Integer.parseInt(value.get("wendulow").toString()));
			paras.getJSONObject("auto_control").put("type", "wendu");
		}
		else if ( clname.equals("shijian"))
		{
			paras.getJSONObject("auto_control").getJSONObject("shijian").put("on", Integer.parseInt(value.get("ontime").toString()));
			paras.getJSONObject("auto_control").getJSONObject("shijian").put("off", Integer.parseInt(value.get("offtime").toString()));
			paras.getJSONObject("auto_control").getJSONObject("shijian").put("week", value.get("weektime").toString());
			paras.getJSONObject("auto_control").put("type", "shijian");
			
		}
		else if (clname.equals("qiangzhi"))
		{
			paras.getJSONObject("auto_control").getJSONObject("qiangzhi").put("run", (value.get("qzrun").toString().equals("1") ? true : false));
			paras.getJSONObject("auto_control").put("type", "qiangzhi");
			
		}
		else if (clname.equals("buganyu"))
		{
			paras.getJSONObject("auto_control").put("type", "none");
		}
		else if (clname.equals("shijianwendu"))
		{
			paras.getJSONObject("auto_control").getJSONObject("wendu").put("high", Integer.parseInt(value.get("wenduhigh").toString()));
			paras.getJSONObject("auto_control").getJSONObject("wendu").put("low", Integer.parseInt(value.get("wendulow").toString()));
			paras.getJSONObject("auto_control").getJSONObject("shijian").put("on", Integer.parseInt(value.get("ontime").toString()));
			paras.getJSONObject("auto_control").getJSONObject("shijian").put("off", Integer.parseInt(value.get("offtime").toString()));
			paras.getJSONObject("auto_control").getJSONObject("shijian").put("week", value.get("weektime").toString());
			paras.getJSONObject("auto_control").put("type", "shijianwendu");
			
		}
		//if (log.isDebugEnabled())
	//	{
			log.info(" 准备下发报文 ： " + JSON.toJSONString(paras));
		//}
		return JSON.toJSONString(paras);
	}
	
	public static void main(String[] argvs)
	{
		
		byte[] b = null;
		InputStream in = null ;
		try {
			in = new FileInputStream(new File("d:/02test.txt")) ;
			
			b = new byte[in.available()] ;
			in.read(b) ;
		} catch (FileNotFoundException e) {
			
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			if ( in != null )
			{
				try {
					in.close() ;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		JSONObject j = JSON.parseObject(new String(b)) ;
		 System.out.println(MessageFactory.makeDevice104ParameterMessage("402881e4405920f501405923bcf40003", "qiangzhi", j)) ;
		
	}

}
