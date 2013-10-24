package com.mce.action;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.JSONParser;
import com.mce.json.parser.ModelObject;
import com.mce.json.parser.ModelSql;
import com.mce.json.parser.SQLParserConfig;
import com.mce.uitl.MCEStatus;
import com.mce.uitl.MD5Util;

public class MCECommandAction implements IMCECommandAction{
	
	private static final Logger log = Logger.getLogger(MCECommandAction.class) ;
	protected String devicename ;
	protected String deviceuid ;
	protected String devicenumber;
	protected Map<String ,JSONObject> valuemap ;
	protected Map datamap ;
	protected Object[] paras ;
	protected SQLParserConfig sqlparser = new SQLParserConfig() ;
	protected DatabaseOperator db = new DatabaseOperator() ;
	protected MCECommandAction commandAction ;
	protected JSONParser jsonparser = new JSONParser() ;
	protected String serialno = null ;
	public void doAction(String message ,String deviceserialno ,IoSession session) throws Exception {
		
		serialno = deviceserialno ;
		commandAction = new MCE109CommandAction() ;
		
		commandAction.doAction(message ,deviceserialno ,session);
	}
	
	/**
	 * ��node
	 * @param sql
	 * @param linkdeviceuid
	 * @param devname
	 * @param devnumber
	 * @param jsonobject
	 * @throws SQLException
	 */
	protected void insertNode(ModelObject modelObject ,String linkdeviceuid ) throws SQLException
	{
		//ModelObject modelObject = sqlparser.getInsertSqlModelObject(devname, devnumber,jsonobject);
		modelObject.setObjuid(MD5Util.getObjuid());
		modelObject.setDeviceuid(linkdeviceuid);
		db.executeSaveOrUpdate(modelObject);
	}
	
	protected boolean  updateCurrentNode(String linkdeviceuid ,String devname ,String devnumber ,String deviceuid,JSONObject jsonobject ,String serialno ) throws SQLException 
	{
 		if(log.isDebugEnabled())
		{
			log.debug("DEVICE_ " + devname) ;
			log.debug(jsonobject) ;
		}
		String model = devname + "." + devnumber + ".current" + ".state.update_time" ;
		if ((Integer)jsonparser.getJsonValue(model, jsonobject) == 0)
		{
			if(log.isDebugEnabled())
			{
				log.debug(devname + "." + devnumber + " ����ʱ����Ч");
				
			}
			return false ;
		}
		Map currentValue = db.execueQuery(ModelSql.getCurrentByDevicduid(devname), new Object[]{linkdeviceuid}) ;
	
		if(currentValue != null)
		{
			// current
			ModelObject current = sqlparser.getCurrentModelObject( devname, devnumber,jsonobject);
			current.setObjuid(MD5Util.getObjuid());
			current.setDeviceuid(linkdeviceuid);
			//�ȱȽϵ�ǰ�յ������е�updatetime �� ���ݿ��е�updatetime  
			Timestamp source = new Timestamp( new Long((Integer)current.getJsonValue().get("update_time")) * 1000) ;
			Timestamp dest = (Timestamp) currentValue.get("updatetime") ;
			if(log.isDebugEnabled())
			{
				log.debug("DEVICE_ " + devname) ;
				log.debug(jsonobject) ;
				log.debug("������ʱ��: " + source);
				log.debug("���ݿ�ʱ��: " + dest);
			}
			//�����ǰ�յ������е�updatetime���� 
			if( source.after(dest))
			{
				processAlarmMessage(jsonobject ,devname, deviceuid ,serialno) ;
				db.executeSaveOrUpdate(current);
			}
			 //������ĺ����ݿ����ʱ�����
			else if (source.equals(dest) )
			{
				//�����ж��Ƿ���ͨѶ���ϡ�ͨѶ���Ϻ��豸��update_time����� ��
				if( !devicename.equals("901") && isDeviceCommunicationFault(jsonobject ,linkdeviceuid) && ! isAlarmOff())
				{
					db.executeSaveOrUpdate(current);
					
					String insertAlarmSql = "insert into alarm_message(objuid ,deviceuid ,alarmtime,alarmtype ,alarmstate) values(?,?,?,?,?)" ;
					Timestamp time = new Timestamp(new Date().getTime()) ;
					db.executeSaveOrUpdate(insertAlarmSql, new Object[]{MD5Util.getObjuid() ,linkdeviceuid ,time ,devicename+"001" , 0});
					//�κ��豸�и澯��Ϣ,�޸��豸�������״̬
					//db.executeSaveOrUpdate(ModelSql.updatePositionStatus() ,new Object[]{MCEStatus.HAS_ALARM, deviceserialno}) ;
					db.updatePositionStatus(MCEStatus.HAS_ALARM, serialno) ;
					// ���ͱ�������
					
					sendAlarmSMS(serialno, devicename + "001") ;
					return false ;
				}
			}
			else
			{
				if ( log.isDebugEnabled())
				{
					
					log.debug(devicename+ " �豸����ʱ����Ч"); 
				}
				return false; 
			}
		}else
		{
			processAlarmMessage(jsonobject ,devname, deviceuid ,serialno) ;
			ModelObject current = sqlparser.getCurrentModelObject( devicename, devicenumber,jsonobject);
			insertNode(current, linkdeviceuid) ;
			
		}
		
		return true ;
		
	}
	
	
	private boolean isDeviceCommunicationFault(JSONObject jsonObject ,String deviceuid)
	{
		boolean  alarm  = jsonObject.getJSONObject(devicename)
			.getJSONObject(devicenumber).getJSONObject("current").getJSONObject("alarm") 
			.getJSONObject(devicename + "001").getBoolean("active");
		String sql = "select * from device" + devicename + "_s where deviceuid = ? order by updatetime desc limit 1" ;
		Map dbAlarm = db.execueQuery(sql, new Object[]{deviceuid}) ;
		
		if( alarm
				&& dbAlarm.get("alarm" + devicename + "001").toString().equals("false"))
		{
			return true ;
		}
		return false  ;
	}
	
	/**
	 * ����澯��Ϣ
	 * @param jsonObject
	 * @param devicename
	 * @param deviceuid
	 */
	protected void processAlarmMessage(JSONObject jsonObject ,String devicename ,String deviceuid ,String deviceserialno)
	{
		
		//�����ж��ܱ��������Ƿ�رգ�����ر��򲻴����κθ澯
		if (isAlarmOff())
		{
			log.info("�ܸ澯���عرգ����и澯��Ϣ��������");
			return ;
		}
		Map jsonMap =jsonObject.getJSONObject(devicename)
		.getJSONObject(devicenumber).getJSONObject("current").getJSONObject("alarm") ;
		Map<String,String >jsonAlarm = parseAlarm(jsonMap ); 
		String sql = "select * from device" + devicename + "_s where deviceuid = ? order by updatetime desc limit 1" ;
		String insertAlarmSql = "insert into alarm_message(objuid ,deviceuid ,alarmtime,alarmtype ,alarmstate) values(?,?,?,?,?)" ;
		Map dbAlarm = db.execueQuery(sql, new Object[]{deviceuid}) ;
		
		
		
		Timestamp time = new Timestamp(new Date().getTime()) ;
		//���dbalarmΪ�� ˵���豸��һ�ο������У���ôjsonAlarm���и澯Ϊtrue�Ķ�����alarm_message��
		if(dbAlarm == null)
		{
			for(Entry<String ,String> entry : jsonAlarm.entrySet())
			{
				if (entry.getValue().equals("true"))
				{
					log.info("�豸�澯  ���ͣ� " + entry.getKey()) ;
					if (log.isDebugEnabled())
					{
						log.debug("�ڵ㱨��:" + jsonObject.toJSONString()) ;
					}
					
					
					if(isAlarmShow(deviceuid ,devicename ,entry.getKey()))
					{
						db.executeSaveOrUpdate(insertAlarmSql, new Object[]{MD5Util.getObjuid() ,deviceuid ,time ,entry.getKey() , 0});
						//�κ��豸�и澯��Ϣ,�޸��豸�������״̬
						//db.executeSaveOrUpdate(ModelSql.updatePositionStatus() ,new Object[]{MCEStatus.HAS_ALARM, deviceserialno}) ;
						db.updatePositionStatus(MCEStatus.HAS_ALARM, deviceserialno) ;
						// ���ͱ�������
						
						sendAlarmSMS(deviceserialno, entry.getKey()) ;
						
					
					}
					else
					{
						log.info("�������ã����Ըø澯");
					}
				}
			}
		}else {
			
			for(Entry<String, String> set : jsonAlarm.entrySet())
			{
				if(set.getValue().equals("true") && ( dbAlarm.get("alarm" + set.getKey()) != null &&
						dbAlarm.get("alarm" + set.getKey()).toString().equals("false")))
				{
					log.info("�豸�澯  ���ͣ� " + set.getKey()) ;
					if (log.isDebugEnabled())
					{
						log.debug("�ڵ㱨��:" + jsonObject.toJSONString()) ;
					}
					//���������CURRENT�ڵ�ĸ���ʱ��������������ĸ���ʱ��
					//�п������豸������ 103 104����MCE�ϲ���� ��������²�����澯��Ϣ
					Map currentMap = jsonObject.getJSONObject(devicename)
					.getJSONObject(devicenumber).getJSONObject("current").getJSONObject("state") ;
					Timestamp source = new Timestamp( new Long((Integer)currentMap.get("update_time")) * 1000) ;
					Timestamp dest = (Timestamp) dbAlarm.get("updatetime") ;
					
					if(source.before(dest))
					{
						return ;
					}
					if(isAlarmShow(deviceuid ,devicename ,set.getKey()))
					{
						db.executeSaveOrUpdate(insertAlarmSql, new Object[]{MD5Util.getObjuid() ,deviceuid ,time ,set.getKey() , 0});
						//�κ��豸�и澯��Ϣ,�޸��豸�������״̬
						//db.executeSaveOrUpdate(ModelSql.updatePositionStatus() ,new Object[]{MCEStatus.HAS_ALARM, deviceserialno}) ;
						db.updatePositionStatus(MCEStatus.HAS_ALARM, deviceserialno) ;
						
						// ���ͱ�������
						sendAlarmSMS(deviceserialno, set.getKey()) ;
					}
					else
					{
						log.info("�������ã����Ըø澯");
					}
				}
			}
		}
		
		
	}
	
	
	private boolean isAlarmShow(String deviceuid , String devicename ,String alarmType)
	{
		Boolean rev = false; 
		Map parmeter = db.execueQuery(ModelSql.getParmeterbyDeviceuidSql(devicename, deviceuid) ,null) ;
		if(parmeter != null)
		{
			rev = (Boolean) parmeter.get("isalarm" + alarmType);
			if (rev == null)
				rev = true;
		}
		else
		{
			rev = true ;
		}
		return rev ;
	}
	
	private boolean isAlarmOff()
	{
		 if ( SystemConfiguration.getProperty("alarmonoff").toString().equals("0"))
		 {
			 log.info("�澯���عر� �����и澯��������") ;
			 return true ;
		 }else
		 {
			 return false ;
		 }
	}
	

	
	private Map<String, String> parseAlarm(Map map)
	{
		Map<String, String > value = new HashMap<String,String>() ;
		String key ;
		for(Iterator it = map.keySet().iterator() ; it.hasNext() ;)
		{
			key = it.next().toString() ;
			value.put(key, JSON.parseObject(map.get(key).toString()).getString("active")) ;
		}
		return value ;
	}
	
	
	public void sendAlarmSMS(String deviceserialno ,String alarmType)
	{
		SystemConfiguration.reload() ;
		if( SystemConfiguration.getProperty("alarmsmsonoff").equals("1"))
		{
			Map position = db.execueQuery(ModelSql.getPositionBySerialNoSql(), new Object[]{deviceserialno}) ;
			String telNumber ="" ;
			List<Map> lm = db.execueQueryReturnMore(ModelSql.getUserMobile(), new Object[]{position.get("objuid")}) ;
			for (int i = 0 ; i < lm.size() ; i++)
			{
				if ( lm.get(i) != null && lm.get(i).toString().length() > 0)
				{
					telNumber = telNumber+ lm.get(i).get("mobile") + "," ;
				}
			}
			telNumber = telNumber.substring(0 ,telNumber.length() -1) ;
			
			Map alarmDesc = db.execueQuery(ModelSql.getAlarmDescBycode(), new Object[]{alarmType}) ;
			
			String message = "�豸�澯: " + alarmType + " " + alarmDesc.get("alarmdesc") + " �����к� ��" + position.get("positionid") + " ,���ص����ƣ�" + position.get("positiondesc") ;
			log.info("׼�����͸澯���� �����պ��룺" + telNumber + " �������ݣ� " + message ) ;
			SendSMSAction.sendMessage(telNumber, message) ;
			
		}
	}
	
	
	public static void main(String[] argvs)
	{
		SystemConfiguration.loadProperty() ;
		MCECommandAction m = new MCECommandAction() ;
		m.sendAlarmSMS("13990005",  "103113") ;
		
	}
	
	

}
