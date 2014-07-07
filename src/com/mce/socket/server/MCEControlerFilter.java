package com.mce.socket.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.ModelSql;
import com.mce.uitl.MCEStatus;
import com.mce.uitl.MCEUtil;
import com.mce.uitl.StoreMessageUtil;

public class MCEControlerFilter extends IoFilterAdapter {
	Logger log = Logger.getLogger(MCEControlerFilter.class);
	DatabaseOperator db = new DatabaseOperator() ;
	public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
		InetSocketAddress inetSocketAddress = (InetSocketAddress) session.getRemoteAddress();
		log.info("收到新的连接请求，IP：" + inetSocketAddress.getAddress().getHostAddress() + " 端口:" + inetSocketAddress.getPort());
		
		StoreMessageUtil.storeMessage2LogFile(session, "收到连接请求，IP：" + inetSocketAddress.getAddress().getHostAddress() + " 端口:" + inetSocketAddress.getPort());
		
		session.write("MOID 901.1.109\n");
		// 初始化session状态 ，每个连接会新建一个session
		session.setAttribute("jsonstring", "");
		session.setAttribute("deviceuid", "");
		session.setAttribute("receivetimes",0 ) ;
		session.setAttribute("status", "") ;
		// 记录连接时间
		
		session.setAttribute("connectiontime" ,MCEUtil.getCurrentDateAll()) ;
		//save socket opened log...
		db.saveSocketConnectionLog(session, MCEStatus.CONNECTION_OPENED) ;
		nextFilter.sessionOpened(session);
	}

	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
		
		
		
		if(log.isDebugEnabled())
		{
			log.debug("=========收到设备报文，日志输出开始=========") ;
			log.debug("内容 ：" + message) ;
			log.debug("网络信息:" + session) ;
			log.debug("=========日志输出结束=========") ;
		}
		try{
		if ( session.getAttribute("messagereceivetimes") == null )
		{
			session.setAttribute("messagereceivetimes" , 0 ) ;
			
		}
		else
		{
			session.setAttribute("messagereceivetimes" , Integer.parseInt( session.getAttribute("messagereceivetimes").toString() ) ) ;
		}
		}catch (Exception e)
		{
			e.printStackTrace(); 
		}
		// log.info("RECEIVED: " + message ) ;
		nextFilter.messageReceived(session, message);

	}

	public void sessionIdle(IoFilter.NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
		log.warn("客户端 【" + session + " 】" + " 读数据超时,关闭连接！" ) ;
		try {
			StoreMessageUtil.storeMessage2LogFile(session, "客户端超时 ，规定时间内未收到任何返回数据，准备断开连接" );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		session.setAttribute("event_type", MCEStatus.CONNECTION_IDEL_CLOSED) ;
		session.close(true) ;
		nextFilter.sessionIdle(session, status);
	}
	
	public void sessionClosed(IoFilter.NextFilter nextFilter, IoSession session)
       throws Exception
   {
		
		log.warn("客户端 【" + session + " 】" + " 连接关闭！" ) ;
		String deviceuid = (String) session.getAttribute("deviceuid") ;
	
		//if(! SessionManager.isDeviceConnected(deviceuid))
		//{
			log.info("更改MCE连接状态为断开 。序列号： " + deviceuid) ;
			//db.executeSaveOrUpdate(ModelSql.updatePositionStatus(), new Object[]{MCEStatus.CONNECTION_CLOSED ,deviceuid}) ;
			
			
			db.updatePositionStatus(MCEStatus.CONNECTION_CLOSED, deviceuid) ;
		//}
		//else
		//{
		//	log.info("关闭超时连接，不修改设备状态.序列号：" + deviceuid) ;
		//}
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