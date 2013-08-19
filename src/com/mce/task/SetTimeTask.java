package com.mce.task;

import java.util.Date;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.mce.socket.server.SessionManager;
import com.mce.uitl.MCECommand;

public class SetTimeTask implements Job{
	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		String dateline = new Date().getTime() + "";
		SessionManager.write(MCECommand.SET_TIME + dateline.substring(0, 10) + "\n") ;
		
	}

}
