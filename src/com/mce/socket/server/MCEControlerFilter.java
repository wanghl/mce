package com.mce.socket.server;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.ModelSql;
import com.mce.uitl.MCEStatus;

public class MCEControlerFilter extends IoFilterAdapter {
	Logger log = Logger.getLogger(MCEControlerFilter.class);
	DatabaseOperator db = new DatabaseOperator() ;
	public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
		InetSocketAddress inetSocketAddress = (InetSocketAddress) session.getRemoteAddress();
		log.info("�յ��µ���������IP��" + inetSocketAddress.getAddress().getHostAddress() + " �˿�:" + inetSocketAddress.getPort());
		session.write("MOID 901.1.109\n");
		// ��ʼ��session״̬ ��ÿ�����ӻ��½�һ��session
		session.setAttribute("jsonstring", "");
		session.setAttribute("deviceuid", "");
		session.setAttribute("receivetimes",0 ) ;
		session.setAttribute("status", "") ;
		//save socket opened log...
		db.saveSocketConnectionLog(session, MCEStatus.CONNECTION_OPENED) ;
		nextFilter.sessionOpened(session);
	}

	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
		
		
		session.setAttribute("status" ,MCEStatus.READ_STATUS) ;
		if(log.isDebugEnabled())
		{
			log.debug("=========�յ��豸���ģ���־�����ʼ=========") ;
			log.debug("���� ��" + message) ;
			log.debug("������Ϣ:" + session) ;
			log.debug("=========��־�������=========") ;
		}
		// log.info("RECEIVED: " + message ) ;
		nextFilter.messageReceived(session, message);

	}

	public void sessionIdle(IoFilter.NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
		log.warn("�ͻ��� ��" + session + " ��" + " �����ݳ�ʱ,�ر����ӣ�" ) ;
		session.setAttribute("event_type", MCEStatus.CONNECTION_IDEL_CLOSED) ;
		session.close(true) ;
		nextFilter.sessionIdle(session, status);
	}
	
	public void sessionClosed(IoFilter.NextFilter nextFilter, IoSession session)
       throws Exception
   {
		
		log.warn("�ͻ��� ��" + session + " ��" + " ���ӹرգ�" ) ;
		String deviceuid = (String) session.getAttribute("deviceuid") ;
	
		if(! SessionManager.isDeviceConnected(deviceuid))
		{
			log.info("����MCE����״̬Ϊ�Ͽ� �����кţ� " + deviceuid) ;
			//db.executeSaveOrUpdate(ModelSql.updatePositionStatus(), new Object[]{MCEStatus.CONNECTION_CLOSED ,deviceuid}) ;
			
			
			db.updatePositionStatus(MCEStatus.CONNECTION_CLOSED, deviceuid) ;
		}
		else
		{
			log.info("�رճ�ʱ���ӣ����޸��豸״̬.���кţ�" + deviceuid) ;
		}
		//save socket closed log ...
		Object reson = session.getAttribute("event_type") ;
		if (reson == null)
			reson = "3" ;
		db.saveSocketConnectionLog(session, reson.toString()) ;
		nextFilter.sessionClosed(session);
   }

	public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
		cause.printStackTrace();
		log.error("Exception: " + cause);
		log.error("ClientInfo: " + session);
	}

}