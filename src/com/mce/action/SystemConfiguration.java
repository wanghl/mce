package com.mce.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.ModelSql;
import com.mce.uitl.ErrorLogUtil;
import com.zephyr.sql.DBConnection;

public class SystemConfiguration {
	private static Logger log = Logger.getLogger(SystemConfiguration.class);
	private static  Map<String ,Object> configuration = new ConcurrentHashMap<String,Object>() ;
	
	private SystemConfiguration () {};
	
	public  void  initialization()
	{
		if (configuration.isEmpty())
		{
			loadProperty() ;
		}
	}
	
	public static void reload()
	{
		configuration.clear() ;
		loadProperty() ;
	}
	
	public static Object getProperty(String key)
	{
		return configuration.get(key) ;
	}
	
	public static void loadProperty()
	{
		DBConnection conn = null ;
		PreparedStatement ps = null ;
		ResultSet rs = null  ;
		List<String> list  = null ;
		List wrap = new ArrayList<String> () ; ;
		DatabaseOperator db = new DatabaseOperator() ;
		try
		{
			conn =  DBConnection.getInstance("tools"); 
			ps = conn.prepareStatement(ModelSql.getSysParasSql()) ;
			rs = ps.executeQuery() ;
			log.info("加载系统配置...");
			while( rs.next() )
			{
				configuration.put(rs.getString("parasname"), rs.getObject("parasvalue")) ;
			}
			//load confignode 
			log.info("加载报文解析模版...");
			ps = conn.prepareStatement(ModelSql.getConfigNodeSql());
			rs = ps.executeQuery() ;
			while( rs.next() )
			{
				list = new ArrayList<String> () ;
				list.add(rs.getString("jsonNode")) ;
				list.add(rs.getString("tableField")) ;
				list.add(rs.getString("dataType")) ;
				list.add(rs.getString("defaultValue")) ;
				
				wrap.add(list);
			}
			configuration.put("configNode", wrap);
			
			ps = conn.prepareStatement(ModelSql.getAllDeviceSql()); 
			rs = ps.executeQuery() ;
			String deviceName  ;
			while ( rs.next() )
			{
				deviceName = rs.getString(1);
				configuration.put("device" + deviceName + "_s", loadMessageConfig(ModelSql.getCurrentNodeSql(deviceName)));
				configuration.put("device" + deviceName + "_p", loadMessageConfig(ModelSql.getParameterNodeSql(deviceName)));
			}
//			//load device102_s
//			configuration.put("device102_s", loadMessageConfig(ModelSql.getCurrentNodeSql("102")));
//			//load device103_s
//			configuration.put("device103_s", loadMessageConfig(ModelSql.getCurrentNodeSql("103")));
//			//load device104_s
//			configuration.put("device103_s", loadMessageConfig(ModelSql.getCurrentNodeSql("104")));
//			//load device901_s
//			configuration.put("device901_s", loadMessageConfig(ModelSql.getCurrentNodeSql("901")));
//			
//			//load device102_p
//			configuration.put("device102_p", loadMessageConfig(ModelSql.getParameterNodeSql("102")));
//			//load device103_p
//			configuration.put("device103_p", loadMessageConfig(ModelSql.getParameterNodeSql("103")));
//			//load device103_p
//			configuration.put("device103_p", loadMessageConfig(ModelSql.getParameterNodeSql("104")));
//			//load device901_p
//			configuration.put("device901_p", loadMessageConfig(ModelSql.getParameterNodeSql("901")));
			
			//load serialno 
			ps = conn.prepareStatement(ModelSql.getSerialNoSql());
			rs = ps.executeQuery() ;
			while( rs.next() )
			{
				configuration.put("serialno", rs.getString("jsonNode")) ;
			}
			//load descript 
			ps = conn.prepareStatement(ModelSql.getDescriptionSql());
			rs = ps.executeQuery() ;
			while( rs.next() )
			{
				configuration.put("description", rs.getString("jsonNode")) ;
			}
			
			//load alarm type code 
			
			ps = conn.prepareStatement(ModelSql.getAlarmTypeCode()) ;
			rs = ps.executeQuery() ;
			while( rs.next() )
			{
				configuration.put(rs.getString("alarmtypecode"), rs.getString("alarmdesc")) ;
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace() ;
			log.error(e);
			log.error("严重错误！MCE服务程序初始化失败！" + e.getMessage()) ;
			Map map = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.MESSAGE_ERROR_CODE, e.getMessage(), "MCE服务初始化失败", null) ;
			db.saveErrorLog(map);
			try {
				throw e ;
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		finally
		{
			if( conn != null)
			{
				conn.close() ;
			}
		}
		
	}
	
	
	private static List loadMessageConfig(String sql)
	{
		DBConnection conn = null ;
		PreparedStatement ps = null ;
		ResultSet rs = null  ;
		List<String> list  = null ;
		List wrap = new ArrayList<String> () ; ;
		try
		{
			conn =  DBConnection.getInstance("tools"); 
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery() ;
			while( rs.next() )
			{
				list = new ArrayList<String> () ;
				list.add(rs.getString("jsonNode")) ;
				list.add(rs.getString("tableField")) ;
				list.add(rs.getString("dataType")) ;
				list.add(rs.getString("defaultValue")) ;
				
				wrap.add(list);
			}
		}
		catch (Exception e)
		{
			log.error(e);
			
		}
		finally
		{
			if( conn != null)
			{
				conn.close() ;
			}
		}
		
		return wrap ;
	}
	
	public static void main(String[] argvs)
	{
		SystemConfiguration cfg  =new SystemConfiguration() ;
		cfg.initialization();
	}

}
