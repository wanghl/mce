package com.mce.task;

import java.util.List;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.mce.db.operate.DatabaseOperator;
import com.mce.socket.server.SessionManager;
import com.mce.uitl.MCEStatus;

public class CheckDeviceStatusTask extends TimerTask{
	DatabaseOperator db = new DatabaseOperator() ;
	Logger log = Logger.getLogger(CheckDeviceStatusTask.class);
	@Override
	public void run() {
		String sql = "update env_position set positionstate = ? where positionid not in (" ;
		List<String> serialnoList = SessionManager.getMCESerialnoList() ;
		
		if(! serialnoList.isEmpty())
		{
			for(String positionid : serialnoList)
			{
				sql += "'" + positionid + "'," ; 
			}
			sql = sql.substring(0, sql.length() -1) ;
			sql += ")" ;
			log.info("更新未主动连接服务端的设备为断开状态 ...");
			db.executeSaveOrUpdate(sql, new Object[]{MCEStatus.CONNECTION_CLOSED}) ;
		}
		
	}

}
