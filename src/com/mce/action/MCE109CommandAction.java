package com.mce.action;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mce.json.parser.ModelObject;
import com.mce.json.parser.ModelSql;
import com.mce.socket.server.SessionManager;
import com.mce.uitl.MCECommand;
import com.mce.uitl.MCEStatus;
import com.mce.uitl.MCEUtil;
import com.mce.uitl.MD5Util;

public class MCE109CommandAction extends MCECommandAction  {

	private static final Logger log = Logger.getLogger(MCE109CommandAction.class);
	public MCE109CommandAction() {
		commandAction = new MCE103CommandAction();
	}

	@SuppressWarnings( { "unchecked", "static-access" })
	public void doAction(String message, String deviceserialno,IoSession session) throws Exception {
		JSONObject jsonobject = JSON.parseObject(message);
		String positionid ;
		if (jsonobject.getString("oid").equals("901.1.109"))
		{
			try
			{
				jsonobject = jsonobject.getJSONObject("retval");
				Map<String, JSONObject> map = jsonobject.parseObject(jsonobject.toJSONString(),
						HashMap.class);
				for (Entry<String, JSONObject> entry : map.entrySet())
				{
					devicename = entry.getKey();
					if (devicename.equals("911"))
						continue;
					///deviceuid = deviceserialno;
					valuemap = JSON.parseObject(entry.getValue().toJSONString(), HashMap.class);
					for (Entry<String, JSONObject> set : valuemap.entrySet())
					{
						devicenumber = set.getKey();
						deviceuid = deviceserialno +"." + devicename + "." + devicenumber;
						// 根据deviceuid查device_config
						paras = new Object[] { deviceuid };
						datamap = db.execueQuery(ModelSql.selectDeviceByDeviceuidSql(), paras);
						// 如果设备不存在 则插 devcie_config curren parameter
						if (datamap == null || datamap.isEmpty())
						{
							// 先根据deviceuid在env_position表里查对应的device记录
							Map position  = db.execueQuery(ModelSql.getPositionBySerialNoSql(), new Object[]{deviceserialno}) ;
							positionid = position.get("objuid").toString() ;
							Map<String ,Object> positionValue = new HashMap<String,Object>() ;
							Map<String ,Object> jsonValue = new HashMap<String,Object>() ;
							positionValue.put("String",positionid) ;
							jsonValue.put("String", entry.getValue().getJSONObject(devicenumber).getJSONObject("config").toJSONString()) ;
							
							
							// config table
							ModelObject config = sqlparser.getConfigNode(devicename, devicenumber, jsonobject);
							config.setSql(config.getSql().replace("deviceuid", "deviceid,positionuid,configjson"));
							config.setSql(config.getSql().replace("values(?", "values(?,?, ?"));
							config.setDeviceuid(deviceuid);
							config.setObjuid(MD5Util.getObjuid());
							config.getValue().add(0, positionValue) ;
							config.getValue().add(1, jsonValue) ;
							db.executeSaveOrUpdate(config);
							
							// alarm
							processAlarmMessage(jsonobject ,devicename ,config.getObjuid(),deviceserialno ,new Date().getTime()) ;
							
							// insert current node 
							ModelObject current = sqlparser.getCurrentModelObject( devicename, devicenumber,jsonobject);
							insertNode(current,config.getObjuid() ) ;
							
							// insert parmeter node 
							ModelObject parmeter = sqlparser.getParmeterModelObject( devicename, devicenumber,jsonobject);
							insertNode(parmeter,config.getObjuid() ) ;
							
							
							

						} else
						{
							Map<String ,Object >para = new HashMap<String ,Object>() ;
							para.put("String", datamap.get("objuid")) ;
							// config table update
							ModelObject config = sqlparser.getConfigNode4Update(devicename, devicenumber, "objuid" ,jsonobject);
							config.setDeviceuid(deviceuid);
							config.setObjuid(datamap.get("objuid").toString());
							config.getValue().add(para) ;
							db.executeSaveOrUpdate(config);
							
							//update current ;
							
							boolean rev = updateCurrentNode(datamap.get("objuid").toString() ,devicename, devicenumber ,config.getObjuid() ,jsonobject ,deviceserialno ) ;
							
							// alarm
//							if ( rev )
//							{
//								processAlarmMessage(set.getValue() ,devicename ,config.getObjuid() ,deviceserialno) ;
//								
//							}
							
							para.clear() ;
							para.put("String", config.getObjuid()) ;
							ModelObject parameter = sqlparser.getParmeterModelObject4Update( devicename, devicenumber, "deviceuid" ,jsonobject);
							parameter.setDeviceuid(config.getObjuid());
							parameter.getValue().add(para) ;
							int returnValue = db.executeSaveOrUpdate(parameter);
							
							if(returnValue != 1)
							{
								
								ModelObject parmeter = sqlparser.getParmeterModelObject( devicename, devicenumber,jsonobject);
								insertNode(parmeter,config.getObjuid() ) ;

								
							}
							
							// parmeter
							
						}
					}
				}
			} catch (Exception e)
			{
				e.printStackTrace() ;
				log.error(e);
				throw e ;
			}
			//读901设备参数
			session.write("MOID 901.1.2\n") ;
			// 读取104设备参数  。设备每次连接后 都把设备参数读过来放到session对象里
			Thread.sleep(20000);
			List list = db.execueQueryReturnMore(ModelSql.get104Device(deviceserialno), null);
			int j = 1 ;
			for ( int i = 0 ; i < list.size() ; i++)
			{
				session.write("MOID 104." + j + ".2\n") ;
				j++ ;
			}
			
			//发送时间同步命令 ，同步MCE时间
			
			//String dateline = Long.toString(MCEUtil.getUTCString() / 1000 ) ; 
			//session.write(MCECommand.SET_TIME  + dateline + "\n") ;
			
		}
		else
		{
			commandAction.doAction(message, deviceserialno,session);
		}
	}
	

}
