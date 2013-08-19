package com.mce.action;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class MCEReceiveMessageAction extends MCECommandAction{
	
	private static final Logger log = Logger.getLogger(MCEReceiveMessageAction.class);
	
	
	public void doAction(String message ,String deviceserialno ,IoSession session) throws Exception
	{
		
		JSONObject jsonobject = JSON.parseObject(message);
		String oid = jsonobject.getString("oid") ;
		int errno = jsonobject.getInteger("errno") ;
		if ( oid.contains("12"))
		{
			//��Ϊ���ò������ر���
			if(oid.startsWith("104"))
			{
				String key = session.getAttribute("deviceuid").toString() + ".104." + oid.split("\\.")[1] ;
				session.setAttribute(key +".errno" ,jsonobject.getString("errno")) ;
				session.setAttribute(key + ".errstr" ,jsonobject.getString("errstr")) ;
				
				if( jsonobject.getInteger("errno") == 0)
				{
					log.info("�յ��������óɹ��� MCE ���кţ� " + session.getAttribute("deviceuid"));
				}
				else
				{
					log.info("�յ���������ʧ�ܣ� MCE���кţ�" + session.getAttribute("deviceuid")) ;
					log.info("������� �� " + jsonobject.getString("errno")) ;
					log.info("������Ϣ �� " + jsonobject.getString("errstr")) ;
				}
				
				if ( session.getAttribute("restart") != null &&  (Boolean) session.getAttribute("restart") == true){
					session.write("MOID 901.1.199\n") ;
				}
				else
				{
					session.write("MOID 901.1.109\n") ;
				}
			}
		}
		else if ( oid.endsWith(".2"))
		{
			//��Ϊ��ȡ���÷��ر���
			if(oid.startsWith("104"))
			{
				if (errno == 0)
				{
					//��ȡ104�豸�ķ���ֵ�ŵ�session�� ��keyΪ�豸���к�.104.���ֱ�ʶ
					String key = session.getAttribute("deviceuid").toString() + ".104." + oid.split("\\.")[1] + ".paras" ;
					session.setAttribute(key , message);
					log.info("��ȡ�յ������������ɹ��� MCE���кţ�" + session.getAttribute("deviceuid")) ;
				}
				else
				{
					log.info("��ȡ�յ�����ʧ�ܣ� MCE���кţ�" + session.getAttribute("deviceuid")) ;
					log.info("������� �� " + jsonobject.getString("errno")) ;
					log.info("������Ϣ �� " + jsonobject.getString("errstr")) ;
				}
				
			}
		}
		else if ( oid.endsWith(".199"))
		{
			log.info("*********************** MCE�豸������...���к�: "  + session.getAttribute("deviceuid") + " *********************** ") ;
		}
		else if ( oid.endsWith(".117")) 
		{
			if ( errno == 0 )
			{
				log.info("�豸ʱ��ͬ���ɹ������кţ�" + session.getAttribute("deviceuid")) ;
				
			}
		}
		else
		{
			throw new RuntimeException("δ֪OID�� " + oid) ;
		}
	}

}
