package com.mce.socket.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import com.mce.uitl.MCEStatus;
import com.mce.uitl.MessageFactory;
import com.mce.uitl.StoreMessageUtil;

public class SessionManager {
	private final static Logger log = Logger.getLogger(SessionManager.class);
	private static Map<Long, IoSession> sessionMap = new ConcurrentHashMap <Long, IoSession>() ;
	private static Map<String, String> ktconfigMap = new ConcurrentHashMap <String, String>() ;
	
	
	private static IoAcceptor currentAcceptor = null;
	private static final String QUERY_STATUS = "MOID 901.1.103\n" ;
	public static void initialize(IoAcceptor acceptor)
	{
		sessionMap  = acceptor.getManagedSessions();
		currentAcceptor = acceptor ;
	}
	
	//  设置空调参数  
	public static void setKtConfig( String deviceuid ,String configstr)
	{
		log.info("已收到空调配置 ： 设备序列号" + deviceuid + " 配置   " + configstr);
		
		ktconfigMap.put(deviceuid, configstr) ;
	}
	
	public static String getKtConfig(String deviceuid)
	{
		log.info("当前空调配置");
		for(Entry<String ,String> entry : ktconfigMap.entrySet())
		{
			log.info("键 名： " + entry.getKey() );
		}
		return ktconfigMap.get(deviceuid) ;
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
	
	public static Integer setDevice104Parameter(String deviceuid ,String cluid ,String clname ,Boolean restart )
	{
		String deviceIndex = deviceuid.split("\\.")[2];
		String serialno = deviceuid.split("\\.")[0];
		try{
		for(Entry<Long ,IoSession> entry : sessionMap.entrySet())
		{
			if(entry.getValue().getAttribute("deviceuid").toString().equals(serialno))
			{
//				for ( Object obj : entry.getValue().getAttributeKeys()) 
//				{
//					log.info("Session keys:" + obj);
//					
//					log.info("Values : " + entry.getValue().getAttribute(obj));
//				}
//				if ( entry.getValue().getAttribute(deviceuid + ".paras") == null ){
//					log.info("设备 " + serialno + " 有网络延迟情况 ，放弃参数设置") ;
//					return -1 ;
//				}
				entry.getValue().setAttribute("restart" ,restart) ;
					
			/*	if ( log.isDebugEnabled())
				{
					log.debug("发送命令 ： " +"MOID 104." + deviceIndex + ".12 " + MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject(entry.getValue().getAttribute(deviceuid + ".paras").toString()) )+ "\n");
				}
				entry.getValue().write("MOID 104." + deviceIndex + ".12 " + MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject(entry.getValue().getAttribute(deviceuid + ".paras").toString()) )+ "\n") ;
				break ;*/
				
				if ( log.isDebugEnabled())
				{
					log.debug("发送命令 ： " +"MOID 104." + deviceIndex + ".12 " + MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject( SessionManager.getKtConfig(serialno) ) )+ "\n");
				}
				if ( getKtConfig( serialno + ".104." + deviceIndex + ".paras" )  == null 
						|| getKtConfig( serialno + ".104." + deviceIndex + ".paras" ).equals(""))
				{
					return -2;
				}
				log.info(serialno + " 设备空调配置： " + SessionManager.getKtConfig(serialno + ".104." + deviceIndex + ".paras"));
				String cmdmsg = MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject(  SessionManager.getKtConfig( serialno + ".104." + deviceIndex + ".paras" )  ) );
				log.info("发送命令 ： " +"MOID 104." + deviceIndex + ".12 " + cmdmsg + "\n");

				entry.getValue().write("MOID 104." + deviceIndex + ".12 " + cmdmsg + "\n") ;
				break ;
				
			}
		}
		}catch (Exception e)
		{
			log.error(e);
			e.printStackTrace(); 
			return -1 ;
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
					try {
						StoreMessageUtil.storeMessage2LogFile(entry.getValue(), "上一状态查询指令未收到返回报文。暂不发送103命令" );
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
