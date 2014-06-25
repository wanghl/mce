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
import com.mce.json.parser.ModelSql;
import com.mce.task.CheckDeviceStatusTask;
import com.mce.task.GetCurrentTask;
import com.mce.task.MakeReportTask;
import com.mce.task.SetTimeTask;
import com.mce.uitl.ErrorLogUtil;

public class MinaServer extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L; 
	static Logger log = Logger.getLogger(MinaServer.class);
	DatabaseOperator db = new DatabaseOperator();

	public void init() throws ServletException {

		try {
			// load sysconfig
			log.info("系统初始化 ...");
			SystemConfiguration .loadProperty() ;
			//打开网络监听前将所有设备的状态职位断开 
			db.executeSaveOrUpdate(ModelSql.updateAllPositionStatus("3"), null) ;
			log.info("系统初始化完成！ ") ;
			IoAcceptor acceptor = new NioSocketAcceptor();
			acceptor.getFilterChain().addLast("controler", new MCEControlerFilter());
			// 多线程过滤器，将IO和业务处理分开
			acceptor.getFilterChain().addLast("excuteThreadPool", new ExecutorFilter(ThreadPoolManager.getThreadPoolInstance()));
			// 报文解码过滤器
			acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MCEProtocolCodeFactory()));
			
			int idleTime = 0;
			try{
				idleTime = Integer.parseInt(SystemConfiguration.getProperty("linkouttime").toString()) ;
			}catch(Exception e )
			{
				log.error("env_sysparas 表中无 linkouttime配置或配置值非法！已经使用默认参数配置。") ;
				Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.RUNTIME_ERROR_CODE, e.getMessage(), "env_sysparas 表中无 linkouttime配置或配置值非法！已经使用默认参数配置。", "执行位置：MinaServer ");
				db.saveErrorLog(datamap);
				idleTime = 1200 ;
			}
			acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, idleTime);
			acceptor.getSessionConfig().setReadBufferSize(20480);
			acceptor.setHandler(new MCEProcessHandler());
			// 定时任务 - 定时查询103报文

			Timer timer = new Timer();
			GetCurrentTask task = new GetCurrentTask(acceptor.getManagedSessions());
			int getdataperiod = 0 ;
			
			try
			{
				getdataperiod = Integer.parseInt(SystemConfiguration.getProperty("getdataperiod").toString());
			}catch (Exception e)
			{
				log.error("env_sysparas 表中无 getdataperiod配置或配置值非法！已经使用默认参数配置。") ;
				Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.RUNTIME_ERROR_CODE, e.getMessage(), "env_sysparas 表中无 getdataperiod 配置或配置值非法！已经使用默认参数配置。", "执行位置：MinaServer ");
				db.saveErrorLog(datamap);
				getdataperiod = 20000 ;
			}
			timer.schedule(task, 3000, getdataperiod);

			// 启动2分钟后，查询MCE状态
			Timer chkstatusTimer = new Timer();
			int updatestatustime = 0 ;
			try{
				updatestatustime = Integer.parseInt(SystemConfiguration.getProperty("updatestatustime").toString());
			}catch (Exception e)
			{
				log.error("env_sysparas 表中无 updatestatustime配置或配置值非法！已经使用默认参数配置。") ;
				Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.RUNTIME_ERROR_CODE, e.getMessage(), "env_sysparas 表中无 updatestatustime 配置或配置值非法！已经使用默认参数配置。", "执行位置：MinaServer ");
				db.saveErrorLog(datamap);
				updatestatustime = 120000 ;
			}
			chkstatusTimer.schedule(new CheckDeviceStatusTask(), updatestatustime);
			// 初始化SocketSession
			SessionManager.initialize(acceptor);

			// 定时生成设备运行日报
			String createreporttime = null ;
			try
			{
				createreporttime = SystemConfiguration.getProperty("createreporttime").toString();
			}catch(Exception e)
			{
				log.error("env_sysparas 表中无 createreporttime配置或配置值非法！已经使用默认参数配置。") ;
				Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.RUNTIME_ERROR_CODE, e.getMessage(), "env_sysparas 表中无 createreporttime 配置或配置值非法！已经使用默认参数配置。", "执行位置：MinaServer ");
				db.saveErrorLog(datamap);
				createreporttime = "0 0 8 * * ?";
			}
			
			JobDetail j = new JobDetail("MakeMCEReport", "task1", MakeReportTask.class);
			CronTrigger c = new CronTrigger("t1", "t1");
			CronExpression ce = new CronExpression(createreporttime);
			c.setCronExpression(ce);
			SchedulerFactory s = new StdSchedulerFactory();
			Scheduler t = s.getScheduler();
			t.scheduleJob(j, c);
			t.start();

			// 定时下发当前时间到设备
			JobDetail jc = new JobDetail("SettimeTask", "task2", SetTimeTask.class);
			CronTrigger cc = new CronTrigger("t2", "t2");
			CronExpression cec = new CronExpression("0 0 23 * * ?");
			cc.setCronExpression(cec);
			SchedulerFactory sc = new StdSchedulerFactory();
			Scheduler tc = sc.getScheduler();
			tc.scheduleJob(jc, cc);
			tc.start();

			// 启动MINA服务
			String port = (String) SystemConfiguration.getProperty("socketport");
			if (port == null) {
				port = "10101";
			}
			log.info("server start...");
			acceptor.bind(new InetSocketAddress(Integer.parseInt(port)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
			Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.NETWORK_ERROR_CODE, e.getMessage(), "网络错误", "执行位置：MinaServer ");
			db.saveErrorLog(datamap);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			log.error(e);
			Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.RUNTIME_ERROR_CODE, e.getMessage(), "运行时错误", "执行位置：MinaServer 设置生产日志定时任务时抛出异常：" + e.getMessage());
			db.saveErrorLog(datamap);

		} catch (ParseException e) {
			log.error(e);
			Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.RUNTIME_ERROR_CODE, e.getMessage(), "运行时错误", "执行位置：MinaServer 设置生产日志定时任务时抛出异常： 解析执行任务时间配置错误。请检查env_sysparas表中配置:" + e.getMessage());
			db.saveErrorLog(datamap);
		}catch (Exception e)
		{
			log.error(e);
		}

	}

	public static void main(String[] argvs) throws ServletException {
		MinaServer m = new MinaServer();
		m.init();
	}
}
