package com.mce.socket.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSON;
import com.mce.action.SystemConfiguration;
import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.ModelSql;
import com.mce.uitl.MCECommand;
import com.mce.uitl.MCEStatus;
import com.mce.uitl.MessageFactory;

public class SessionManager {
	private final static Logger log = Logger.getLogger(SessionManager.class);
	private static Map<Long, IoSession> sessionMap = new ConcurrentHashMap <Long, IoSession>() ;
	private static IoAcceptor currentAcceptor = null;
	private static final String QUERY_STATUS = "MOID 901.1.103\n" ;
	public static void initialize(IoAcceptor acceptor)
	{
		sessionMap  = acceptor.getManagedSessions();
		currentAcceptor = acceptor ;
	}
	
	/**
	 * 向指定序列号的设备发送命令
	 * @param deviceuid
	 * @param commandString
	 */
	public static void write(String deviceuid ,String commandString)
	{
		for(Entry<Long ,IoSession> entry : sessionMap.entrySet())
		{
			if(entry.getValue().getAttribute("deviceuid").toString().equals(deviceuid))
			{
				entry.getValue().write(commandString) ;
				log.info("发送命令成功! " +deviceuid  + " "  + commandString) ;
				break ;
			}
		}
	}
	
	public static void write(String commandString)
	{
		for(Entry<Long ,IoSession> entry : sessionMap.entrySet())
		{
				entry.getValue().write(commandString) ;
		}
		
		
	}
	
	public static IoSession getSession(String deviceuid)
	{
		for(Entry<Long ,IoSession> entry : sessionMap.entrySet())
		{
			if(entry.getValue().getAttribute("deviceuid").toString().equals(deviceuid))
			{
				return entry.getValue() ;
			}
		}
		
		return null ;
	}
	
	public static Integer setDevice104Parameter(String deviceuid ,String cluid ,String clname ,Boolean restart)
	{
		String deviceIndex = deviceuid.split("\\.")[2];
		String serialno = deviceuid.split("\\.")[0];
		for(Entry<Long ,IoSession> entry : sessionMap.entrySet())
		{
			if(entry.getValue().getAttribute("deviceuid").toString().equals(serialno))
			{
				if ( entry.getValue().getAttribute(deviceuid + ".paras") == null ){
					log.info("设备 " + serialno + " 有网络延迟情况 ，放弃参数设置") ;
					return -1 ;
				}
				entry.getValue().setAttribute("restart" ,restart) ;
					
				if ( log.isDebugEnabled())
				{
					log.debug("发送命令 ： " +"MOID 104." + deviceIndex + ".12 " + MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject(entry.getValue().getAttribute(deviceuid + ".paras").toString()) )+ "\n");
				}
				entry.getValue().write("MOID 104." + deviceIndex + ".12 " + MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject(entry.getValue().getAttribute(deviceuid + ".paras").toString()) )+ "\n") ;
				break ;
			}
			
		}
		return 0 ;
	}
	/**
	 * 向所有设备发送命令
	 * @param commandString
	 */
	public static void write103Command()
	{
		
	
		for(Entry<Long,IoSession> entry : sessionMap.entrySet())
		{
			
			if( entry.getValue().getAttribute("status").toString().equals("") || (entry.getValue().isConnected() && entry.getValue().getAttribute("status").toString().equals(MCEStatus.READ_STATUS)) )
			{
				
				entry.getValue().write(QUERY_STATUS) ;
				entry.getValue().setAttribute("status",MCEStatus.WRITE_STATUS) ;
				if(log.isDebugEnabled())
				{
					log.debug("=========发送状态查询命令开始=========");
					log.debug("命令发送成功！设备序列号: " + entry.getValue().getAttribute("deviceuid"));
					log.debug("session信息: " + entry.getValue()) ;
					log.debug("session状态： " + MCEStatus.WRITE_STATUS);
					log.debug("=========命令发送完成=========");
					log.debug("") ;
				}
			}
			else
			{
				
				if (!entry.getValue().isConnected())
				{
					log.info("发送状态查询命令失败! 设备序列号: " + entry.getValue().getAttribute("deviceuid"));
					log.info("session信息: " + entry.getValue()) ;
					log.info("原因: 网络连接异常") ;
				}
				//如果发送命令为103 且session状态为写 说明上一103命令发送后还未收到返回报文
				if ( ! entry.getValue().getAttribute("status").toString().equals(MCEStatus.READ_STATUS))
				{
					log.info("发送状态查询命令终止! 设备序列号: " + entry.getValue().getAttribute("deviceuid"));
					log.info("session信息: " + entry.getValue()) ;
					log.info("原因: 上一状态查询指令未收到返回报文") ;
				}
				if ( SystemConfiguration.getProperty("closemode").equals("0"))
				{
					log.info("设备控制命令发送失败，准备断开连接 ") ;
					entry.getValue().setAttribute("event_type",MCEStatus.SERVER_SIDE_CLOSE) ;
					entry.getValue().close(true) ;
					
				}
				else
				{
					log.info("放弃发送103查询报文， 等待MCE返回结果") ;
				}
			}
		}
	}
	
	/**
	 * 根据传入的序列号判断设备是否连接
	 * @param deviceuid
	 * @return
	 */
	public static boolean isDeviceConnected(String deviceuid)
	{
		boolean connected = false; 
		for(Entry<Long ,IoSession> entry : sessionMap.entrySet())
		{
			if(entry.getValue().getAttribute("deviceuid").toString().equals(deviceuid))
			{
				connected = true ;
				break ;
			}
		}
		return connected ;
	}
	
	
	public static List<String> getMCESerialnoList()
	{
		List<String> serialnoList = new ArrayList<String>() ;
		for(Entry<Long,IoSession> session : sessionMap.entrySet())
		{
			serialnoList.add( session.getValue().getAttribute("deviceuid").toString() ) ;
		}
		return serialnoList ;
	}
	
	public static Set<Entry<Long,IoSession>> getSessionMapEntrySet(){
		return sessionMap.entrySet();
	}
	
	
	public static void closeServer()
	{
		log.info("关闭MCE服务端...... ");
		//设置服务端断开原因
		for(Entry<Long ,IoSession> entry : sessionMap.entrySet())
		{
			entry.getValue().setAttribute("event_type" ,MCEStatus.SERVER_CLOSED) ;
		}
		currentAcceptor.dispose(true) ;
	}
	
	public static void main(String[] argvs)
	{
		 System.out.println(new Date().toLocaleString()) ;
	}
	
}
