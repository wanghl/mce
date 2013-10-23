package com.mce.db.operate;

import java.net.InetSocketAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.mce.json.parser.ModelObject;
import com.mce.json.parser.ModelSql;
import com.mce.uitl.ErrorLogUtil;
import com.mce.uitl.MD5Util;
import com.zephyr.sql.DBConnection;

/**
 * @author wanghongliang
 *
 */
public class DatabaseOperator {

	private static final Logger log = Logger.getLogger(DatabaseOperator.class)  ;
	
	/**
	 * 疏忽 ，只能返回一条记录  暂时先这么用吧 
	 * 通用查询方法 
	 * @param sql  查询SQL语句
	 * @param paras 字段值
	 * @return
	 */
	public Map execueQuery(String sql ,Object[] paras )
	{
		if ( log.isDebugEnabled() )
		{
			log.debug("Execute sql : " + sql) ;
			
		}
		Map <String ,Object>map  = null ;
		DBConnection connection = null ;
		PreparedStatement ps = null ;
		ResultSet rs = null  ;
		ResultSetMetaData rsmeta = null ;
		try
		{
			connection = DBConnection.getInstance("tools");
			if ( connection == null )
			{
				log.error("从连接池中获取数据库连接失败。");
			}
			ps = connection.prepareStatement(sql);
		
			int j = 1 ;
			if( paras != null){
			for(int i = 0 ; i < paras.length ;i ++)
			{
				if ( log.isDebugEnabled() )
				{
					
					log.debug("Parmeter " + j + " Value :" + paras[i]) ;
				}
				ps.setObject(j, paras[i]);
				j ++ ;
			}
			}
			rs = ps.executeQuery() ;
			
			rsmeta = rs.getMetaData();
			int numberOfMetaData = rsmeta.getColumnCount();
			while (rs.next())
			{
				map  = new HashMap<String, Object>();
				for( int r = 1 ; r < numberOfMetaData + 1 ; r++)
				{
					if(rsmeta.getColumnName(r).equals("updatetime"))
					{
						map.put(rsmeta.getColumnName(r), rs.getTimestamp(r)) ;
						
					}else{
						map.put(rsmeta.getColumnName(r), rs.getObject(r)) ;
					}
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace() ;
			log.error(e);
			Map errmap =  ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.DB_ERROR_CODE, e.getLocalizedMessage(), "执行查询SQL语句出错。", "SQL语句：" + sql + " 出错位置：DatabaseOperator.execueQuery方法") ;
			
			saveErrorLog(errmap);
		}finally{
			if(connection != null)
			{
				connection.close() ;
			}
			
			
		}
		
		return map ;
		
	}
	
	/**
	 * 通用查询方法 
	 * @param sql  查询SQL语句
	 * @param paras 字段值
	 * @return
	 */
	public List<Map> execueQueryReturnMore(String sql ,Object[] paras )
	{
		if ( log.isDebugEnabled())
		{
			
			log.debug("Execute sql : " + sql) ;
		}
		Map <String ,Object>map  = null ;
		List<Map> list = new ArrayList<Map> () ;
		DBConnection connection = null ;
		PreparedStatement ps = null ;
		ResultSet rs = null  ;
		ResultSetMetaData rsmeta = null ;
		try
		{
			connection = DBConnection.getInstance("tools");
			ps = connection.prepareStatement(sql);
			
			int j = 1 ;
			if( paras != null){
				for(int i = 0 ; i < paras.length ;i ++)
				{
					if ( log.isDebugEnabled())
					{
						
						log.debug("Parmeter " + j + " Value :" + paras[i]) ;
					}
					ps.setObject(j, paras[i]);
					j ++ ;
				}
			}
			rs = ps.executeQuery() ;
			
			rsmeta = rs.getMetaData();
			int numberOfMetaData = rsmeta.getColumnCount();
			while (rs.next())
			{
				map  = new HashMap<String, Object>();
				for( int r = 1 ; r < numberOfMetaData + 1 ; r++)
				{
					if(rsmeta.getColumnName(r).equals("updatetime"))
					{
						map.put(rsmeta.getColumnName(r), rs.getTimestamp(r)) ;
						
					}else{
						map.put(rsmeta.getColumnName(r), rs.getObject(r)) ;
					}
				}
				list.add(map) ;
			}
		} catch (Exception e)
		{
			e.printStackTrace() ;
			log.error(e);
			Map errmap =  ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.DB_ERROR_CODE, e.getLocalizedMessage(), "执行查询SQL语句出错。", "SQL语句：" + sql + " 出错位置：DatabaseOperator.execueQueryReturnMore方法") ;
			
			saveErrorLog(errmap);			
		}finally{
			if(connection != null)
			{
				connection.close() ;
			}
			
			
		}
		
		return list ;
		
	}
	
	public void saveSocketConnectionLog(IoSession session ,String eventType)
	{
		 InetSocketAddress inetSocketAddress = (InetSocketAddress) session.getRemoteAddress();
		 String ip = inetSocketAddress.getAddress().getHostAddress();
		 int port = inetSocketAddress.getPort() ;
		 Object deviceuid = session.getAttribute("deviceuid") ;
		 executeSaveOrUpdate(ModelSql.getInsertDeviceConnectionSql() ,new Object[]{MD5Util.getObjuid() ,deviceuid,ip,port,new Date() ,eventType}) ;
	}
	
	/**
	 * 改变节点状态。更改状态后 device_status表新增一条记录
	 * @param status
	 * @param deviceuid
	 */
	public void updatePositionStatus(String status ,String deviceuid)
	{
		
		DBConnection connection = null ;
		PreparedStatement ps = null ;
		ResultSet rs = null  ;
		try{
			connection = DBConnection.getInstance("tools");
			connection.beginTrans() ;
			ps = connection.prepareStatement(ModelSql.getPositionBySerialNoSql()) ;
			ps.setString(1, deviceuid);
			rs = ps.executeQuery() ;
			String ori_status = null;
			while ( rs.next() )
			{
				ori_status = rs.getString("positionstate") ;
			}
			if(ori_status != null && ! ori_status.equals(status))
			{
				//更改position状态
				ps = connection.prepareStatement(ModelSql.updatePositionStatus());
				ps.setString(1, status);
				ps.setString(2, deviceuid);
				ps.executeUpdate() ;
				//device_status表记录日志
				ps = connection.prepareStatement(ModelSql.getInsertDeviceStatusSql()) ;
				ps.setString(1, MD5Util.getObjuid() );
				ps.setString(2, deviceuid);
				ps.setString(3, ori_status);
				ps.setString(4, status);
				ps.setObject(5, new Date());
				ps.executeUpdate() ;
				connection.commit() ;
			}
		}
		catch(Exception e)
		{
			connection.rollback();
			e.printStackTrace();
			log.error(e);
			Map errmap =  ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.DB_ERROR_CODE, e.getLocalizedMessage(), "执行查询SQL语句出错。", "出错位置：DatabaseOperator.updatePositionStatus方法") ;
			
			saveErrorLog(errmap);	
		}
		finally
		{
			if(connection != null)
			{
				try {
					connection.getConnection().setAutoCommit(true);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				connection.close() ;
				
			}
			
		}
		
	}
	
	
	public int executeSaveOrUpdate(String sql ,Object[] paras)
	{
		if ( log.isDebugEnabled())
		{
			
			log.debug("Execute sql : " + sql) ;
		}
		DBConnection connection = null ;
		PreparedStatement ps = null ;
		int returnValue = 0 ;
		try
		{
			connection = DBConnection.getInstance("tools");
			ps = connection.prepareStatement(sql);
		
			int j = 1 ;
			if( paras != null){
			for(int i = 0 ; i < paras.length ;i ++)
			{
				if (log.isDebugEnabled())
				{
					
					log.debug("Parmeter " + j + " Value :" + paras[i]) ;
				}
				ps.setObject(j, paras[i]);
				j ++ ;
			}
			}
			returnValue = ps.executeUpdate() ;
			
		}catch(Exception e){
			e.printStackTrace() ;
			log.error(e);
			Map errmap =  ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.DB_ERROR_CODE, e.getLocalizedMessage(), "执行查询SQL语句出错。", "执行SQL： " + sql + "出错位置：DatabaseOperator.executeSaveOrUpdate方法") ;
			
			saveErrorLog(errmap);	
		}finally{
			if(connection != null)
			{
				connection.close() ;
				
			}
		}
		return returnValue ; 
	}
	
	 
	public int executeSaveOrUpdate(ModelObject modelobject){
		if ( log.isDebugEnabled())
		{
			
			log.debug("Execute sql : " + modelobject.getSql()) ;
		}
		boolean isInsertAction = modelobject.getSql().indexOf("insert") >= 0 ;
		DBConnection connection = null ;
		PreparedStatement ps = null ;
		int returnValue = 0 ;
		try
		{
			connection = DBConnection.getInstance("tools");
			 ps =connection.prepareStatement(modelobject.getSql());
			int index = 1 ;
			if(isInsertAction)
			{
				index = 3;
				if ( log.isDebugEnabled())
				{
					
					log.debug("Value: " + modelobject.getObjuid()) ;
				}
				ps.setString(1, modelobject.getObjuid());
				if ( log.isDebugEnabled())
				{
					
					log.debug("Value: " + modelobject.getDeviceuid()) ;
				} 
				ps.setString(2, modelobject.getDeviceuid());
			}
			for (int i = 0; i < modelobject.getValue().size(); i++)
			{
				
				for (Entry<String, Object> entry : modelobject.getValue().get(i).entrySet())
				{
					setValue(ps, index, entry.getKey(), entry.getValue());
					index++;
				}
			}
				
				returnValue = ps.executeUpdate();
				
				
		} catch (Exception e)
		{
			e.printStackTrace();
			// TODO Auto-generated catch block
			log.error(e);
			Map errmap =  ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.DB_ERROR_CODE, e.getLocalizedMessage(), "执行查询SQL语句出错。", "出错位置：DatabaseOperator.executeSaveOrUpdate方法") ;
			
			saveErrorLog(errmap);	
			
		}finally
		{
			if(connection != null)
			{
				connection.close() ;
				
			}
			
		}
		return returnValue ; 

	}
	
	
	public List<Map> executeAll(String sql , Map parmeter)
	{

		DBConnection connection = null ;
		PreparedStatement ps = null ;
		ResultSet rs = null  ;
		ResultSetMetaData rsmeta = null ;
		List<Map> list = new ArrayList<Map>() ;
		Map<String ,Object > map   ;
		try
		{
			connection = DBConnection.getInstance("tools");
			ps =connection.prepareStatement(sql);
			
			setValue(ps ,sql,parmeter) ;
			if ( sql.toLowerCase().startsWith("select") )
			{
				rs = ps.executeQuery() ;
				
				rsmeta = rs.getMetaData();
				int numberOfMetaData = rsmeta.getColumnCount();
				while (rs.next())
				{
					map  = new HashMap<String, Object>();
					for( int r = 1 ; r < numberOfMetaData + 1 ; r++)
					{
							map.put(rsmeta.getColumnName(r), rs.getObject(r)) ;
					}
					list.add(map) ;
				}
			}
			else
			{
				ps.executeUpdate() ;
			}
				
				
				
		} catch (Exception e)
		{
			e.printStackTrace();
			// TODO Auto-generated catch block
			log.error(e);
			Map errmap =  ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.DB_ERROR_CODE, e.getLocalizedMessage(), "执行查询SQL语句出错。", "执行SQL： " + sql + " 出错位置：DatabaseOperator.executeAll方法") ;
			
			saveErrorLog(errmap);	
			
		}finally
		{
			if(connection != null)
			{
				connection.close() ;
				
			}
			
		}
		
		return list ;

	}
	
	private void setValue(PreparedStatement ps ,String sql ,Map map) throws SQLException
	{
		if ( sql.startsWith("insert"))
		{
			sql = sql.substring(0 ,sql.indexOf("values") - 1  ) ;
			sql = sql.substring(sql.indexOf("(") + 1, sql.length()).replace(")", "")	 ;
			String[] paras =  sql.split(",") ;
			for (int i = 1 ; i <= paras.length ; i ++)
			{
				ps.setObject(i, map.get(paras[i -1].toString().trim())) ;
			}
		}
		else
		{
			String[] p = sql.split(" ");
			String paras ;
			for ( int i = 1 ; i <= p.length ; i++ )
			{
				if ( p[i].startsWith( "="))
				{
					paras = p[i -1].trim() ;
					ps.setObject(i, paras) ;
				}
				else if (p[i].indexOf("=") > 0)
				{
					paras = p[i].split("=")[0].replace("?", "").replace(",", "").trim() ;
					ps.setObject(i, paras) ;
				}
			}
		}
	}
	
	private void setValue(PreparedStatement ps, int index, String valuetype, Object object)
    {

		try {
			if (object == null)
			{
				ps.setObject(index, null) ;
				return ;
			}
		if (valuetype.equals("String"))
		{
			log.debug("Value: " + object.toString()) ;
			ps.setString(index, object.toString());
		}
		if (valuetype.equals("Integer"))
		{
			log.debug("Value: " + object) ;
			ps.setInt(index, (Integer) object);
		}
		if (valuetype.equals("Byte"))
		{
			log.debug("Value: " + (object.toString().equals("true") ? true : false)) ;
			ps.setBoolean(index, object.toString().equals("true") ? true : false);
		}
		if (valuetype.equals("Date"))
		{
			Long time = new Long(Integer.parseInt(object.toString())) ;
			if(time ==0 )
			{
				java.util.Date udate = new java.util.Date();
				log.debug("Value: " + new java.sql.Timestamp(udate.getTime())) ;
				ps.setTimestamp(index,new java.sql.Timestamp(udate.getTime())) ;
				
			}else{
				log.debug("Value: " +new java.util.Date(time * 1000)) ;
				ps.setObject(index,new java.util.Date(time * 1000)) ;
			}
		}
		}catch (Exception e)
		{
			e.printStackTrace();
			log.error(e);
		}
	}
	
	
	public void saveErrorLog(Map<Object ,Object> datamap)
	
	{
		
		String typecode = datamap.get("typecode").toString() ;
		if (typecode.equals(ErrorLogUtil.RUNTIME_ERROR_CODE))
		{
			executeAll(ModelSql.getInsertSql(datamap, "error_log"),datamap) ;
			return ;
		}
		Map value  = execueQuery(ModelSql.getErrorlogByCode() ,new Object[]{typecode}) ;
		// if error not exist ;
		
		if ( value == null )
			
		{
			String sql = ModelSql.getInsertSql(datamap, "error_log") ;
			executeAll(sql ,datamap) ;
		}
		else
		{
			Integer throwtimes = (Integer) value.get("throwtimes") ;
			throwtimes += 1; 
			executeSaveOrUpdate(ModelSql.updateErrorLog() ,new Object[]{datamap.get("exceptiondesc") ,throwtimes , new Date() ,value.get("objuid") }) ;
			
		}
	}
	
	public static void main(String[] argvs)
	{
		String sql = "select * from table where tb1 = ?,tb2=?" ;
		String[] p = sql.split(" ");
		for ( int i = 0 ; i < p.length ; i++ )
		{
			if ( p[i].startsWith( "="))
			{
				System.out.println(p[i -1]) ;
			}
			else if (p[i].indexOf("=") > 0)
			{
				System.out.println(p[i].split("=")[0].replace("?", "").replace(",", "")) ;
			}
		}
	}
}
