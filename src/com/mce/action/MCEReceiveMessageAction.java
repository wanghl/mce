package com.mce.action;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mce.socket.server.SessionManager;
import com.mce.uitl.MCECommand;

public class MCEReceiveMessageAction extends MCECommandAction{
	
	private static final Logger log = Logger.getLogger(MCEReceiveMessageAction.class);
	
	
	public void doAction(String message ,String deviceserialno ,IoSession session) throws Exception
	{
		
		JSONObject jsonobject = JSON.parseObject(message);
		String oid = jsonobject.getString("oid") ;
		int errno = jsonobject.getInteger("errno") ;
		if ( oid.contains("12"))
		{
			//此为设置参数返回报文
			if(oid.startsWith("104"))
			{
				String key = session.getAttribute("deviceuid").toString() + ".104." + oid.split("\\.")[1] ;
				//session.setAttribute(key +".errno" ,jsonobject.getString("errno")) ;
				//session.setAttribute(key + ".errstr" ,jsonobject.getString("errstr")) ;

				SessionManager.setKtConfig(key +".errno" ,jsonobject.getString("errno"));
				SessionManager.setKtConfig(key + ".errstr",jsonobject.getString("errstr"));
				if( jsonobject.getInteger("errno") == 0)
				{
					log.info("空调参数设置成功！ MCE 序列号： " + session.getAttribute("deviceuid"));
				}
				else
				{
					log.info("空调参数设置失败！ MCE序列号：" + session.getAttribute("deviceuid")) ;
					log.info("错误代码 ： " + jsonobject.getString("errno")) ;
					log.info("错误信息 ： " + jsonobject.getString("errstr")) ;
				}
				
				if ( session.getAttribute("restart") != null &&  (Boolean) session.getAttribute("restart") == true){
					session.write("MOID 901.1.199\n") ;
				}
				else
				{
					session.write("MOID 901.1.109\n") ;
				}
			}
			else if (oid.startsWith("901"))
			{
				if( jsonobject.getInteger("errno") == 0)
				{
					log.info("MCE参数设置成功！ MCE 序列号： " + session.getAttribute("deviceuid"));
					log.info("重新读取设备参数， 发送901命令。。。") ;
					session.write(MCECommand.ALL_STATE_109) ;
				}
				else
				{
					log.info("MCE参数设置失败！ MCE序列号：" + session.getAttribute("deviceuid")) ;
					log.info("错误代码 ： " + jsonobject.getString("errno")) ;
					log.info("错误信息 ： " + jsonobject.getString("errstr")) ;
				}
			}
		}
		else if ( oid.endsWith(".2"))
		{
			//此为读取空调配置返回报文
			if(oid.startsWith("104"))
			{
				if (errno == 0)
				{
					/////  改为使用IP+端口的形式作为空调参数设置的返回值  
					//读取104设备的返回值放到session中 。key为设备序列号.104.数字标识
					String key = session.getAttribute("deviceuid").toString() + ".104." + oid.split("\\.")[1] + ".paras" ;
					//session.setAttribute(key , message);

					SessionManager.setKtConfig(key, message);
					
					log.info("读取空调控制器参数成功！ MCE序列号：" + session.getAttribute("deviceuid")) ;
				}
				else
				{
					log.info("读取空调参数失败！ MCE序列号：" + session.getAttribute("deviceuid")) ;
					log.info("错误代码 ： " + jsonobject.getString("errno")) ;
					log.info("错误信息 ： " + jsonobject.getString("errstr")) ;
				}
				
			}
			//读取设备配置信息返回报文
			else if (oid.startsWith("901"))
			{
				if (errno == 0)
				{
					log.info("读取901设备配置参数成功！ MCE序列号：" + session.getAttribute("deviceuid"))  ;
					String key = session.getAttribute("deviceuid").toString() + "901.1.2" ;
					session.setAttribute(key ,message) ;
				}
				else
				{
					log.info("读取MCE设备配置参数失败！ MCE序列号：" + session.getAttribute("deviceuid")) ;
					log.info("错误代码 ： " + jsonobject.getString("errno")) ;
					log.info("错误信息 ： " + jsonobject.getString("errstr")) ;
				}
			}
		}
		else if ( oid.endsWith(".199"))
		{
			log.info("*********************** MCE设备重启中...序列号: "  + session.getAttribute("deviceuid") + " *********************** ") ;
		}
		else if ( oid.endsWith(".117")) 
		{
			if ( errno == 0 )
			{
				log.info("设备时间同步成功！序列号：" + session.getAttribute("deviceuid")) ;
				
			}
		}
		else if( oid.endsWith(".131"))
		{
			if( errno == 0 )
			{
				log.info(" 打开设备代理端口成功") ;
			}
		}
		else
		{
			throw new RuntimeException("未知OID： " + oid +  " MESSAGE : " + message) ;
		}
	}

}
