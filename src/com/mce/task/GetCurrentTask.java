package com.mce.task;

import java.util.Map;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.mce.socket.server.SessionManager;

public class GetCurrentTask extends TimerTask{
	
	private Map<Long ,IoSession> map = null ;
	private static Logger log = Logger.getLogger(GetCurrentTask.class) ;
	public GetCurrentTask(Map<Long ,IoSession> map )
	{
		this .map = map ;
	}

	@Override
	public void run() {
		if(log.isDebugEnabled())
		{
			log.debug("当前管理客户端总数 ： " + map.keySet().size()) ;
		}
//	   for(Entry<Long ,IoSession> session : map.entrySet() )
//		   
//	   {
//		   if(log.isDebugEnabled())
//		   {
//			   log.debug("发送当前状态查询报文 。客户端信息 ： " + session.getValue()) ;
//		   }
//		   
//			   session.getValue().write("MOID 901.1.103\n") ;
//		   
//	   }
		SessionManager.write103Command() ;
		
	}

}
