package com.mce.json.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

/**
 * 
 * @author wanghongliang
 *
 */
public class ModelObject {
	
	
	private String sql ;
	private String deviceuid ;
	private JSONObject jsonobject ;
	private String objuid ;
	private List<Map<String ,Object>> value = new ArrayList<Map<String,Object>>() ;
	private Map<String ,Object> jsonValue = new HashMap<String ,Object>() ;
	
	
	public Map<String, Object> getJsonValue() {
		return jsonValue;
	}
	public void setJsonValue(Map<String, Object> jsonValue) {
		this.jsonValue = jsonValue;
	}
	
	public JSONObject getJsonobject() {
		return jsonobject;
	}
	public String getObjuid() {
		return objuid;
	}
	public void setObjuid(String objuid) {
		this.objuid = objuid;
	}
	public void setValue(List<Map<String, Object>> value) {
		this.value = value;
	}
	public void setJsonobject(JSONObject jsonobject) {
		this.jsonobject = jsonobject;
	}
	public String getDeviceuid() {
		return deviceuid;
	}
	public void setDeviceuid(String deviceuid) {
		this.deviceuid = deviceuid;
	}
	
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	public List<Map<String, Object>> getValue() {
		return value;
	}
	public void setParameter(List<Map<String, Object>> value) {
		this.value = value;
	}

}
