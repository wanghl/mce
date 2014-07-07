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
	
	//  ���ÿյ�����  
	public static void setKtConfig( String deviceuid ,String configstr)
	{
		log.info("���յ��յ����� �� �豸���к�" + deviceuid + " ����   " + configstr);
		
		ktconfigMap.put(deviceuid, configstr) ;
	}
	
	public static String getKtConfig(String deviceuid)
	{
		log.info("��ǰ�յ�����");
		for(Entry<String ,String> entry : ktconfigMap.entrySet())
		{
			log.info("�� ���� " + entry.getKey() );
		}
		return ktconfigMap.get(deviceuid) ;
	}
	/**
	 * ��ָ�����кŵ��豸��������
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
				log.info("��������ɹ�! " +deviceuid  + " "  + commandString) ;
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
//					log.info("�豸 " + serialno + " �������ӳ���� ��������������") ;
//					return -1 ;
//				}
				entry.getValue().setAttribute("restart" ,restart) ;
					
			/*	if ( log.isDebugEnabled())
				{
					log.debug("�������� �� " +"MOID 104." + deviceIndex + ".12 " + MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject(entry.getValue().getAttribute(deviceuid + ".paras").toString()) )+ "\n");
				}
				entry.getValue().write("MOID 104." + deviceIndex + ".12 " + MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject(entry.getValue().getAttribute(deviceuid + ".paras").toString()) )+ "\n") ;
				break ;*/
				
				if ( log.isDebugEnabled())
				{
					log.debug("�������� �� " +"MOID 104." + deviceIndex + ".12 " + MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject( SessionManager.getKtConfig(serialno) ) )+ "\n");
				}
				if ( getKtConfig( serialno + ".104." + deviceIndex + ".paras" )  == null 
						|| getKtConfig( serialno + ".104." + deviceIndex + ".paras" ).equals(""))
				{
					return -2;
				}
				log.info(serialno + " �豸�յ����ã� " + SessionManager.getKtConfig(serialno + ".104." + deviceIndex + ".paras"));
				String cmdmsg = MessageFactory.makeDevice104ParameterMessage(cluid, clname, JSON.parseObject(  SessionManager.getKtConfig( serialno + ".104." + deviceIndex + ".paras" )  ) );
				log.info("�������� �� " +"MOID 104." + deviceIndex + ".12 " + cmdmsg + "\n");

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
	 * �������豸��������
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
					log.debug("=========����״̬��ѯ���ʼ=========");
					log.debug("����ͳɹ����豸���к�: " + entry.getValue().getAttribute("deviceuid"));
					log.debug("session��Ϣ: " + entry.getValue()) ;
					log.debug("session״̬�� " + MCEStatus.WRITE_STATUS);
					log.debug("=========��������=========");
					log.debug("") ;
				}
			}
			else
			{
				
				if (!entry.getValue().isConnected())
				{
					log.info("����״̬��ѯ����ʧ��! �豸���к�: " + entry.getValue().getAttribute("deviceuid"));
					log.info("session��Ϣ: " + entry.getValue()) ;
					log.info("ԭ��: ���������쳣") ;
				}
				//�����������Ϊ103 ��session״̬Ϊд ˵����һ103����ͺ�δ�յ����ر���
				if ( ! entry.getValue().getAttribute("status").toString().equals(MCEStatus.READ_STATUS))
				{
					log.info("����״̬��ѯ������ֹ! �豸���к�: " + entry.getValue().getAttribute("deviceuid"));
					log.info("session��Ϣ: " + entry.getValue()) ;
					log.info("ԭ��: ��һ״̬��ѯָ��δ�յ����ر���") ;
					try {
						StoreMessageUtil.storeMessage2LogFile(entry.getValue(), "��һ״̬��ѯָ��δ�յ����ر��ġ��ݲ�����103����" );
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if ( SystemConfiguration.getProperty("closemode").equals("0"))
				{
					log.info("�豸���������ʧ�ܣ�׼���Ͽ����� ") ;
					entry.getValue().setAttribute("event_type",MCEStatus.SERVER_SIDE_CLOSE) ;
					entry.getValue().close(true) ;
					
				}
				else
				{
					log.info("��������103��ѯ���ģ� �ȴ�MCE���ؽ��") ;
				}
			}
		}
	}
	
	/**
	 * ���ݴ�������к��ж��豸�Ƿ�����
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
		log.info("�ر�MCE�����...... ");
		//���÷���˶Ͽ�ԭ��
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
