package com.mce.task;

import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.mce.action.SendMailAction;
import com.mce.action.SystemConfiguration;
import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.ModelSql;
import com.mce.uitl.ErrorLogUtil;
import com.mce.uitl.MCEStatus;
import com.mce.uitl.MCEUtil;
import com.mce.uitl.MD5Util;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * @author wanghongliang ��ʱ�����豸�����ձ�
 */
public class MakeReportTask implements Job {
	
	private static final Logger log = Logger.getLogger(MakeReportTask.class) ;
	private static final String[] MCESTATUS = new String[] { MCEStatus.CONNECTION_CLOSED,
															MCEStatus.HAS_ALARM,
															MCEStatus.SUCCESS };

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.TimerTask#run() connection_time : ָ�����кŵ�������ʱ�� close_time
	 * : ָ�����кŵ����Ͽ�ʱ�� ���������־ʱ �����״̬Ϊ���� ���и澯��ֻ��¼����ʱ�䣬û�жϿ�ʱ��
	 * ���device_connection����û�м�¼����ֱ����device_report���б�עΪ����δ���ӡ� ��������ʱ�� ��
	 * ���״̬Ϊ���������и澯ʱ ����connection_time��������־��һʱ���ļ����Ϊ��������ʱ��
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		DatabaseOperator db = new DatabaseOperator();
		String objuid = "";
		String summaryuid = "";
		summaryuid = MD5Util.getObjuid();
		int unruncount = 0, alarmcount = 0, successcount = 0, sumcount = 0;
		Map<String, Object> dataModel = new HashMap<String, Object>();

		List<Map> alarmList = new ArrayList<Map>();
		List<Map> unrunList = new ArrayList<Map>();
		List<Map> successList = new ArrayList<Map>();
		try {
		//for (String status : MCESTATUS) {
			// process closed position
			List<Map> list = db.execueQueryReturnMore(ModelSql.getPositionByStateSql(), null);
			sumcount += list.size();
			String status = null ;
			List<Map> value = new ArrayList<Map>();
			Map dbObject = new HashMap();
			Map dbRestult = new HashMap();
			if (!list.isEmpty()) {
				for (Map map : list) {
					dbRestult = db.execueQuery(ModelSql.getMakeDeviceReportSql(), new Object[] { map.get("positionid"),
					                                                                           map.get("positionid") });
					status =  (String) map.get("positionstate") ;
					if (dbRestult == null) {
						continue;
					}
					objuid = MD5Util.getObjuid();

					Map<String, Object> alarm = new HashMap<String, Object>();
					Map<String, Object> unrun = new HashMap<String, Object>();
					Map<String, Object> success = new HashMap<String, Object>();

					List<String> alarmInfo = new ArrayList<String>();
					List<String> socketinfo = new ArrayList<String>();

					if (dbRestult.get("connection_time") == null && status.equals(MCEStatus.CONNECTION_CLOSED)) {
						dbObject.put("objuid", objuid);
						dbObject.put("positionuid", map.get("objuid"));
						dbObject.put("devicename", map.get("positiondesc"));
						dbObject.put("serialno", map.get("positionid"));
						dbObject.put("maketime", MCEUtil.getCurrentDate());
						dbObject.put("status", status);
						dbObject.put("summaryuid", summaryuid);
						dbObject.put("memo", "��δ����");
					} else {

						dbObject.put("objuid", objuid);
						dbObject.put("positionuid", map.get("objuid"));
						dbObject.put("devicename", map.get("positiondesc"));
						dbObject.put("serialno", map.get("positionid"));
						dbObject.put("connection_time", dbRestult.get("connection_time"));
						if (status.equals(MCEStatus.CONNECTION_CLOSED)) {
							dbObject.put("close_time", dbRestult.get("close_time"));
						} else {
							try {
								long runtime = ((Timestamp) dbRestult.get("connection_time")).getTime();
								dbObject.put("runtime", MCEUtil.getBetweenDate(runtime));
							} catch (Exception e) {
								e.printStackTrace();
								log.error(e) ;

							}

						}
						dbObject.put("summaryuid", summaryuid);
						dbObject.put("status", status);
						dbObject.put("maketime", MCEUtil.getCurrentDate());

					}
					// save it
					db.executeAll(ModelSql.getInsertSql(dbObject, "device_report"), dbObject);

					if (status.equals(MCEStatus.HAS_ALARM)) {
						alarm.put("devicename", dbObject.get("devicename"));
						alarm.put("serialno", dbObject.get("serialno"));
						alarm.put("runtime", dbObject.get("runtime"));
					} else if (status.equals(MCEStatus.CONNECTION_CLOSED)) {
						unrun.put("devicename", dbObject.get("devicename"));
						unrun.put("serialno", dbObject.get("serialno"));
						unrun.put("runtime", dbObject.get("runtime"));
					} else {
						success.put("devicename", dbObject.get("devicename"));
						success.put("serialno", dbObject.get("serialno"));
						success.put("runtime", dbObject.get("runtime"));
					}
					dbObject.clear();

					// link alarm ;

					value = db.execueQueryReturnMore(ModelSql.getMakeDeviceReportAlarmSql(), new Object[] { map.get("positionid"),
																											MCEUtil.getBeforeDate(new Date()) });
					for (Map vm : value) {
						dbObject.put("objuid", MD5Util.getObjuid());
						dbObject.put("reportuid", objuid);
						dbObject.put("alarmtype", vm.get("alarmtype"));
						dbObject.put("alarmtime", vm.get("alarmtime"));
						dbObject.put("alarmstate", vm.get("alarmstate"));
						dbObject.put("alarmdesc", vm.get("alarmdesc"));
						dbObject.put("affirmuseruid", vm.get("affirmuseruid"));
						dbObject.put("affirmdesc", vm.get("affirmdesc"));
						dbObject.put("affirmtime", vm.get("affirmtime"));

						alarmInfo
								.add(vm.get("alarmtime").toString().substring(0, vm.get("alarmtime").toString().length() - 2) + " " + SystemConfiguration.getProperty(vm.get("alarmtype").toString()));

						db.executeAll(ModelSql.getInsertSql(dbObject, "report_link_alarm"), dbObject);
						dbObject.clear();

					}

					// link socket connection info

					value = db.execueQueryReturnMore(ModelSql.getMakeDeviceReportConnectionSql(), new Object[] { map.get("positionid"),
																												MCEUtil.getBeforeDate(new Date()) });
					for (Map vm : value) {
						dbObject.put("objuid", MD5Util.getObjuid());
						dbObject.put("reportuid", objuid);
						dbObject.put("deviceuid", vm.get("deviceuid"));
						dbObject.put("ip", vm.get("ip"));
						dbObject.put("port", vm.get("port"));
						dbObject.put("updatetime", vm.get("updatetime"));
						dbObject.put("event_type", vm.get("event_type"));

						socketinfo.add(vm.get("updatetime").toString().substring(0, vm.get("updatetime").toString().length() - 2) + "  "
								+ ((vm.get("event_type").toString().equals("0")) ? "�Ͽ�" : "����"));
						
						
						db.executeAll(ModelSql.getInsertSql(dbObject, "report_link_connection"), dbObject);
						dbObject.clear();
					}

					if (map.get("positionstate").toString().equals("1")) {
						successcount += 1;

						success.put("alarminfo", alarmInfo);
						success.put("socketinfo", socketinfo);
						successList.add(success);
					} else if (map.get("positionstate").toString().equals("2")) {
						alarmcount += 1;

						alarm.put("alarminfo", alarmInfo);
						alarm.put("socketinfo", socketinfo);
						alarmList.add(alarm);
					} else {
						unruncount += 1;

						unrun.put("alarminfo", alarmInfo);
						unrun.put("socketinfo", socketinfo);
						unrunList.add(unrun);

					}

				}
			}
		//}

		String times = SystemConfiguration.getProperty("createreporttime").toString();
		times = times.split(" ")[2] + ":" + times.split(" ")[1] + ":" + times.split(" ")[0];
		StringBuffer sub = new StringBuffer();

		String summarydesc = "�� " + MCEUtil.getBeforeDate(MCEUtil.getCurrentDate()) + " " + times + " �� " + MCEUtil.getCurrentDate() + " " + times + " ������ " + sumcount + "̨�豸����ʹ��״̬" + "������δ����"
				+ unruncount + "̨ ���и澯" + alarmcount + "̨����������" + successcount + "̨����������ϸ��Ϣ��";
		System.out.println( summarydesc ) ;
		dataModel.put("summary", summarydesc);
		dataModel.put("success", successList);
		dataModel.put("unrun", unrunList);
		dataModel.put("alarm", alarmList);
		//		
		// System.out.println("alarm " + alarmList.size()) ;
		// System.out.println("success " + successList.size()) ;
		// System.out.println("unrun " + successList.size()) ;
		//		
		Map map = new HashMap();
		map.put("objuid", summaryuid);
		map.put("summarydesc", summarydesc);
		db.executeAll(ModelSql.getInsertSql(map, "device_report_summary"), map);
		
		// send report mail .
		
		// ���жϷ����ʼ�����
		SystemConfiguration.reload() ;
		if ( SystemConfiguration.getProperty("reportmailonoff").toString().equals("1") )
		{
			
			String mailContent = MCEUtil.createHtml(dataModel);
			
			List<Map> revlist = db.execueQueryReturnMore(ModelSql.getEmailSql(), null) ;
			StringBuffer sb = new StringBuffer() ;
			for(Map receiver : revlist)
			{
				sb.append(receiver.get("email").toString() + " ,") ;
			}
			String title = "MCE " + MCEUtil.getCurrentDate() + " ������־�����������ƣ�" + SystemConfiguration.getProperty("servername") + " δ����"
				+ unruncount + "̨ ���и澯" + alarmcount + "̨����������" + successcount + "̨" ;
			log.info("׼������������־��TITLE :" + title) ;
			//sned it 
			SendMailAction.sendMail(sb.toString() , title ,mailContent) ;
		}
		}catch (Exception e)
		{
			e.printStackTrace();
			Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.SENDMAIL_ERROR_CODE, e.getLocalizedMessage(), "�ʼ����ʹ���", "ִ��λ�ã�MakeReportTask" + e.getMessage());
			db.saveErrorLog(datamap);
        	log.error(e) ;
		}
	}

	

	public static void main(String[] argvs) throws Exception {
		SystemConfiguration.loadProperty();
		MakeReportTask t = new MakeReportTask();
		t.execute(null);

	}

}
