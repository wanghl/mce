package com.mce.json.parser;

import java.util.Map;
import java.util.Map.Entry;

/**
 * @author wanghongliang
 *
 */
public class ModelSql {
	
	
	
	
	public static String getAllDeviceSql()
	{
		return "SELECT * FROM (SELECT LEFT(jsonNode,3) AS jsonNode FROM message_config WHERE LEFT(jsonNode,1) !='#') AS message_config  GROUP BY message_config.jsonNode" ;
	}
	
	
	/**
	 * 获取config节点配置SQL语句
	 * @return
	 */
	public static String getConfigNodeSql()
	{
		return "select jsonNode ,tableField,dataType,defaultValue from message_config where jsonNode like '#DEVICENAME#.#NUMBER#.config%'" ;
	}
	
	/**
	 * 根据deviceuid查找设备
	 * @return
	 */
	public static String selectDeviceByDeviceuidSql()
	{
		return "select objuid,positionuid,deviceid,devicename,devicetype,devicesubtype,configjson from device_config where deviceid = ?" ;
	}
	
	/**
	 * 获取节点配置
	 * @param devicename
	 * @return
	 */
	public static String getCurrentNodeSql(String devicename)
	{
		return "select jsonNode ,tableField,dataType,defaultValue from message_config where jsonNode like '" + devicename + "%#NUMBER#.current%'" ;
	}
	
	/**
	 * 获取节点配置
	 * @param devicename
	 * @return
	 */
	public static String getParameterNodeSql(String devicename)
	{
		return "select jsonNode ,tableField,dataType,defaultValue from message_config where jsonNode like '" + devicename + "%#NUMBER#.parameter%'" ;
	}
	
	public static String getSerialNoSql()
	{
		return "select jsonNode ,tableField,dataType,defaultValue from message_config where jsonNode like '901.1%serial_no'" ;
	}
	
	public static String getDescriptionSql()
	{
		return "select jsonNode ,tableField,dataType,defaultValue from message_config where jsonNode like '901.1%description'" ;
	}
	
	public static String get104ControlSql()
	{
		return "select * from device104_control where objuid=?" ;
	}
	
	
	public static String getAlarmTypeCode()
	{
		return "select * from alarmtype" ;
	}
	
	public static String getPositionBySerialNoSql()
	{
		return "select * from env_position where positionid = ?" ;
	}
	
	public static String getPositionByStateSql()
	{
		return "select * from env_position where positionstate = ?" ;
	}
	
	public static String getCurrentByDevicduid(String devicename)
	{
		return "select * from device" + devicename + "_s where deviceuid= ? order by updatetime desc limit 1" ;
	}
	
	public static String getInsertPositionSql() 
	
	{
		return "insert into env_position (objuid, positionid ,positiondesc ,positionstate) values(?,?,?,?)" ;
	}
	
	public static String getInsertDeviceStatusSql()
	{
		return "insert into device_status (objuid,deviceuid ,ori_status,current_status,change_time)" +
				" values (?,?,?,?,?)" ;
	}
	
	public static String getInsertDeviceConnectionSql()
	{
		return "insert into device_connection (objuid ,deviceuid ,ip ,port ,updatetime ,event_type)" +
				" values (?,?,?,?,?,?)" ;
	}
	
	public static String getUpdateConnectionDeviceuidSql()
	{
		return "update device_connection set deviceuid = ? where ip = ? and port = ?" ;
	}
	
	public static String updatePositionStatus()
	{
		return "update env_position set positionstate = ? where positionid = ?" ;
		
	}
	
	public static String updateAllPositionStatus(String status)
	{
		return "update env_position set positionstate = " + status ;
	}
	
	public static String getParmeterbyDeviceuidSql(String  devicename ,String deviceuid)
	{
		return "select * from device" + devicename + "_p where deviceuid='" + deviceuid + "'";
	}
	public static String getPositionHasAlarmSql()
	{
		return "select al.alarmstate from alarm_message al left join device_config d on al.deviceuid = d.objuid left join env_position p on d.positionuid = p.objuid where p.positionid=? and al.alarmstate=?" ;
	}
	
	public static String getSysParasSql()
	{
		return "select * from env_sysparas" ;
	}
	
	
	public static String getMakeDeviceReportSql()
	{
		return "SELECT (SELECT MAX(updatetime) FROM device_connection WHERE deviceuid=? AND event_type='0') AS connection_time  , (SELECT MAX(updatetime) FROM device_connection WHERE deviceuid=? AND event_type='3') AS close_time FROM device_connection LIMIT 1" ;
		
	}
	
	public static String getInsertSql(Map<Object,Object> dataMap ,String tableName )
	{
		String sql = "insert into " + tableName +  "(" ;
		String sqlValue  = " values( " ;
		for (Entry<Object ,Object> map : dataMap.entrySet())
		{
			sql += map.getKey() + " ," ;
			sqlValue += "?," ;
		}
		sql = sql.substring(0 ,sql.lastIndexOf(",")) + ")" ;
		sqlValue = sqlValue.substring(0 ,sqlValue.lastIndexOf(",")) + ")" ;
		return sql + sqlValue ;
		
	}

	public static String getMakeDeviceReportAlarmSql()
	{
		return "SELECT al.* FROM alarm_message al ,device_config d ,env_position p  WHERE  al.deviceuid = d.objuid AND d.positionuid=p.objuid AND p.positionid=? AND ( DATE_FORMAT(al.alarmtime, '%Y-%c-%d') = CURDATE() or DATE_FORMAT(al.alarmtime, '%Y-%m-%d') = ? )" ;
	}
	
	
	public static String getMakeDeviceReportConnectionSql()
	
	{
		return "SELECT * FROM device_connection WHERE deviceuid= ? AND ( DATE_FORMAT(updatetime, '%Y-%c-%d') = CURDATE() OR DATE_FORMAT(updatetime, '%Y-%m-%d') = ? ) ORDER BY updatetime ; " ;
	}
	
	
	public static String getErrorlogByCode()
	{
		return "select * from error_log where typecode= ?" ;
	}
	
	public static String updateErrorLog() 
	{
		return "update error_log set exceptiondesc = ? ,throwtimes = ? ,time=? where objuid=?" ;
	}
	
	public static String getEmailSql()
	{
		return "select email from do_org_user" ;
	}
	
	public static String get104Device(String serialno)
	{
		return "select * from device_config where deviceid like '" + serialno + ".104.%'" ;
	}
	
	public static String get104DeviceBySerialno()
	{
		return "select * from device_config where deviceid = ?" ;
	}

	
	

}
