package com.mce.socket.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.Map;
import java.util.Timer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import com.mce.action.SystemConfiguration;
import com.mce.db.operate.DatabaseOperator;
import com.mce.task.CheckDeviceStatusTask;
import com.mce.task.GetCurrentTask;
import com.mce.task.MakeReportTask;
import com.mce.task.SetTimeTask;
import com.mce.uitl.ErrorLogUtil;


public class MinaServer extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(MinaServer.class);
	DatabaseOperator db = new DatabaseOperator() ;
	public void init() throws ServletException 
	{
		//load sysconfig
		log.info("ϵͳ��ʼ�� ...");
		SystemConfiguration cfg = new SystemConfiguration();
		cfg.initialization() ;
		IoAcceptor acceptor = new NioSocketAcceptor(); 
		acceptor.getFilterChain().addLast("controler", new MCEControlerFilter() );
		//���̹߳���������IO��ҵ����ֿ�
		acceptor.getFilterChain().addLast("excuteThreadPool" ,new ExecutorFilter(ThreadPoolManager.getThreadPoolInstance()));
		//���Ľ��������
		acceptor.getFilterChain().addLast("codec" ,new ProtocolCodecFilter(new MCEProtocolCodeFactory()));
		int idleTime = Integer.parseInt(SystemConfiguration.getProperty("linkouttime").toString()) ;
		//acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, idleTime);
		acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, 1200);
		acceptor.getSessionConfig().setReadBufferSize(20480) ;
		acceptor.setHandler( new MCEProcessHandler() );
		//��ʱ���� - ��ʱ��ѯ103����
		
		Timer timer = new Timer() ;
		GetCurrentTask task = new GetCurrentTask(acceptor.getManagedSessions()) ;
		int getdataperiod = Integer.parseInt(SystemConfiguration.getProperty("getdataperiod").toString()) ;
		timer.schedule(task, 3000 , getdataperiod) ;
		
		// ����2���Ӻ󣬲�ѯMCE״̬
		Timer chkstatusTimer = new Timer();
		int updatestatustime = Integer.parseInt(SystemConfiguration.getProperty("updatestatustime").toString()) ;
		chkstatusTimer.schedule(new CheckDeviceStatusTask(), updatestatustime) ;
		//��ʼ��SocketSession
		SessionManager.initialize(acceptor) ;
		
		try
		{
		//��ʱ�����豸�����ձ�
			String createreporttime = SystemConfiguration.getProperty("createreporttime").toString() ;
			if (createreporttime == null )
				createreporttime = "0 0 8 * * ?" ;
			JobDetail j = new JobDetail("MakeMCEReport", "task1", MakeReportTask.class);
			CronTrigger c = new CronTrigger("t1", "t1");
			CronExpression ce = new CronExpression(createreporttime);
			c.setCronExpression(ce);
			SchedulerFactory s = new StdSchedulerFactory();
			Scheduler t = s.getScheduler();
			t.scheduleJob(j, c);
			t.start();
			
			//��ʱ�·���ǰʱ�䵽�豸
			JobDetail jc = new JobDetail("SettimeTask", "task2", SetTimeTask.class);
			CronTrigger cc = new CronTrigger("t2", "t2");
			CronExpression cec = new CronExpression("0 0 23 * * ?");
			cc.setCronExpression(cec);
			SchedulerFactory sc = new StdSchedulerFactory();
			Scheduler tc = sc.getScheduler();
			tc.scheduleJob(jc, cc);
			tc.start();
			
		//����MINA����
			String port =  (String) SystemConfiguration.getProperty("socketport") ;
			if ( port == null )
			{
				port = "10101"; 
			}
			log.info("server start...");
			acceptor.bind(new InetSocketAddress(Integer.parseInt(port))) ;
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			log.error(e) ;
			Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.NETWORK_ERROR_CODE, e.getMessage(), "�������", 
					"ִ��λ�ã�MinaServer ") ;
			db.saveErrorLog(datamap);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			log.error(e) ;
			Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.RUNTIME_ERROR_CODE, e.getMessage(), "����ʱ����", 
			"ִ��λ�ã�MinaServer ����������־��ʱ����ʱ�׳��쳣��" + e.getMessage()) ;
			db.saveErrorLog(datamap);
			
		} catch (ParseException e) {
			log.error(e) ;
			Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.RUNTIME_ERROR_CODE, e.getMessage(), "����ʱ����", 
					"ִ��λ�ã�MinaServer ����������־��ʱ����ʱ�׳��쳣�� ����ִ������ʱ�����ô�������env_sysparas��������:" + e.getMessage()) ;
			db.saveErrorLog(datamap);
		}
		
	}
	
	public static void main(String[] argvs) throws ServletException
	{
		MinaServer m = new MinaServer() ;
		m.init() ;
	}
}
