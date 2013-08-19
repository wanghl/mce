package com.mce.json.parser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.mce.action.SystemConfiguration;
import com.mce.db.operate.DatabaseOperator;
import com.mce.uitl.ErrorLogUtil;
import com.zephyr.sql.DBConnection;

/**
 * 生成SQL语句
 * 
 * @author wanghongliang
 * 
 */
public class SQLParserConfig {

	private static final Logger log = Logger.getLogger(SQLParserConfig.class);

	/**
	 * 
	 * @param modelsql
	 *            从message_config表中查询相关节点数据的SQL
	 * @param devicenumber
	 *            设备编号
	 * @param jsonobject
	 * @return
	 * @throws SQLException
	 */

	public ModelObject getConfigNode(String devicename ,String devicenumber ,JSONObject json) throws SQLException
	{
		return getInsertSqlModelObject("configNode" ,devicename ,devicenumber,json) ;
	}
	public ModelObject getConfigNode4Update(String devicename ,String devicenumber ,String wherecase,JSONObject json) throws SQLException
	{
		return getUpdateSqlModelObject("configNode" ,devicename ,devicenumber,wherecase ,json) ;
	}
	
	public ModelObject getCurrentModelObject(String devicename ,String devicenumber ,JSONObject json) throws SQLException
	{
		return getInsertSqlModelObject("device"+ devicename + "_s" ,devicename ,devicenumber,json) ;
	}
	
	
	public ModelObject getParmeterModelObject(String devicename ,String devicenumber ,JSONObject json) throws SQLException
	{
		return getInsertSqlModelObject("device" + devicename + "_p" ,devicename ,devicenumber,json) ;
	}
	
	public ModelObject getCurrentModelObject4Update(String devicename ,String devicenumber ,String wherecase ,JSONObject json) throws SQLException
	{
		return getUpdateSqlModelObject("device" + devicename + "_s" ,devicename ,devicenumber,wherecase ,json) ;
	}
	
	
	public ModelObject getParmeterModelObject4Update(String devicename ,String devicenumber ,String wherecase ,JSONObject json) throws SQLException
	{
		return getUpdateSqlModelObject("device" + devicename + "_p" ,devicename ,devicenumber,wherecase ,json) ;
	}
	
	
	
	
	private   ModelObject getInsertSqlModelObject(String key, String devicename, String devicenumber, JSONObject jsonobject) throws SQLException {
		String jsonstring = "";
		String finalsql = "";
		String tableinfo;
		String tablename = "";
		Object defaultValue;
		Object jsonvalue;
		JSONParser jsonparser = new JSONParser();
		StringBuffer sqlfield = new StringBuffer();
		StringBuffer sqlvalue = new StringBuffer();
		ModelObject object = new ModelObject();
		DatabaseOperator db = new DatabaseOperator() ;
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map;
		Map<String, Object> jsonmap = new HashMap<String, Object>();
		try {
			List<List> cfg = (List<List>) SystemConfiguration.getProperty(key);
			sqlfield.append("insert into ");
			sqlvalue.append(") values(?,?,");
			
			for (List cfglist : cfg) {
				map = new HashMap<String, Object>();
				jsonstring = (String) cfglist.get(0);
				tableinfo = (String) cfglist.get(1);
				defaultValue = (String) cfglist.get(3);
				if (tablename.equals("")) {
					tablename = tableinfo.split("\\.")[0];
					sqlfield.append(tablename).append("(objuid ,deviceuid ,").append(tableinfo.split("\\.")[1] + " ,");
				} else {
					sqlfield.append(tableinfo.split("\\.")[1] + " ,");
				}
				if (defaultValue == null) {
					sqlvalue.append("?,");
					// 解析
					jsonstring = jsonstring.replace("#NUMBER#", devicenumber);
					jsonstring = jsonstring.replace("#DEVICENAME#", devicename);
					jsonvalue = jsonparser.getJsonValue(jsonstring, jsonobject);
					//TODO :  报文解析错误存入错误日志表
					if ( jsonvalue == null )
					{
						Map errmap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.MESSAGE_ERROR_CODE, "报文解析错误", "报文项：" + jsonstring + " 解析值为NULL，该项不存在", 
								 "执行位置：SQLParserConfig.getInsertSqlModelObject 报文内容：" + jsonobject.toJSONString()) ;
						db.saveErrorLog(errmap) ;
					}
					map.put((String) cfglist.get(2), jsonvalue);
					jsonmap.put(jsonstring.substring(jsonstring.lastIndexOf(".") + 1, jsonstring.length()), jsonvalue);
					list.add(map);
				} else {
					sqlvalue.append(defaultValue + ",");
				}
			}
			finalsql += sqlfield.toString();
			// 去掉最后一个逗号
			finalsql = finalsql.substring(0, finalsql.length() - 1);

			finalsql += sqlvalue.toString();
			finalsql = finalsql.substring(0, finalsql.length() - 1);
			finalsql += ")";
			object.setParameter(list);
			object.setSql(finalsql);
			object.setJsonValue(jsonmap);
			if (log.isDebugEnabled()) {
				log.debug(finalsql);
				log.debug(list);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getLocalizedMessage());
			throw new RuntimeException(e) ;
		}
		return object;
	}

	//
	private ModelObject getUpdateSqlModelObject(String key, String devicename, String devicenumber, String wherecase, JSONObject jsonobject) {
		String jsonstring = "";
		String finalsql = "";
		String tableinfo;
		String tablename = "";
		Object jsonvalue;
		DBConnection connection = null;
		JSONParser jsonparser = new JSONParser();
		StringBuffer sqlfield = new StringBuffer();
		StringBuffer sqlvalue = new StringBuffer();
		ModelObject object = new ModelObject();
		DatabaseOperator db = new DatabaseOperator() ;
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map;
		Map<String, Object> jsonmap = new HashMap<String, Object>();
		sqlfield.append("update ");
		sqlvalue.append(") values(?,?,");
		try {
			List<List> cfg = (List<List>) SystemConfiguration.getProperty(key);
			for (List cfglist : cfg) {
				map = new HashMap<String, Object>();
				if ( cfglist.get(3) != null)
				{
					continue ;
				}
				jsonstring = (String) cfglist.get(0);
				tableinfo = (String) cfglist.get(1);
				if (tablename.equals("")) {
					tablename = tableinfo.split("\\.")[0];
					sqlfield.append(tablename).append(" set ").append(tableinfo.split("\\.")[1] + "=? ,");
				} else {
					sqlfield.append(tableinfo.split("\\.")[1] + "=? ,");
				}
				// 解析
				jsonstring = jsonstring.replace("#NUMBER#", devicenumber);
				jsonstring = jsonstring.replace("#DEVICENAME#", devicename);
				jsonvalue = jsonparser.getJsonValue(jsonstring, jsonobject);
				//TODO :  报文解析错误存入错误日志表
				if ( jsonvalue == null )
				{
					Map errmap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.MESSAGE_ERROR_CODE, "报文解析错误", "报文项：" + jsonstring + " 解析值为NULL，该项不存在", 
							 "执行位置：SQLParserConfig.getUpdateSqlModelObject 报文内容：" + jsonobject.toJSONString()) ;
					db.saveErrorLog(errmap) ;
				}
				map.put((String) cfglist.get(2), jsonvalue);

				jsonmap.put(jsonstring.substring(jsonstring.lastIndexOf("."), jsonstring.length()), jsonvalue);
				list.add(map);
			}
			finalsql += sqlfield.toString();
			// 去掉最后一个逗号
			finalsql = finalsql.substring(0, finalsql.length() - 1);

			if (wherecase != null || !wherecase.equals("")) {

				finalsql += " where " + wherecase + "=?";
			}
			// finalsql = finalsql.substring(0 ,finalsql.length() - 1) ;
			// finalsql += ")";
			object.setParameter(list);
			object.setSql(finalsql);
			object.setJsonValue(jsonmap);
			if (log.isDebugEnabled()) {
				log.debug(finalsql);
				log.debug(list);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
		return object;
	}

}
