package com.mce.socket.server;

import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;

public class ThreadPoolManager {
	
	private static OrderedThreadPoolExecutor excutor = null ;
	
	public static OrderedThreadPoolExecutor getThreadPoolInstance()
	{
		if ( excutor  == null )
		{
			excutor = new OrderedThreadPoolExecutor(6,30) ;
			return excutor ;
		}
		else
		{
			return excutor ;
		}
	}

}
