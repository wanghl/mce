package com.mce.socket.server;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.mce.action.IMCECommandAction;
import com.mce.action.MCECommandAction;
import com.mce.action.SystemConfiguration;
import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.JSONParser;
import com.mce.json.parser.ModelSql;
import com.mce.uitl.Authorization;
import com.mce.uitl.MCECommand;
import com.mce.uitl.MCEStatus;
import com.mce.uitl.MD5Util;

public class MCEProcessHandler extends IoHandlerAdapter {

	private final Logger log = Logger.getLogger(MCEProcessHandler.class);
	DatabaseOperator db = new DatabaseOperator();
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		cause.printStackTrace();
		log.error(cause);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		try {
			if(log.isDebugEnabled())
			{
				log.debug(session) ;
				log.debug(message);
			}
			JSONObject jsonobject = JSON.parseObject(message.toString());
			JSONParser jsonparser = new JSONParser();
			// deviceuid put to session
			String oid = jsonobject.getString("oid");
//			if (oid.startsWith("104"))
//			{
//				if ( session.getAttribute("deviceuid") == null || session.getAttribute("deviceuid").equals("") )
//				{
//					session.write("MOID 901.1.109\n");
//					return ;
//				}
//			}
			if ( ! oid.equals("901.1.2") && ! oid.equals("901.1.12") && ! oid.startsWith("104"))
			{
				if (oid.equals("901.1.109") || session.getAttribute("deviceuid").equals("")) {
					//String model = db.execueQuery(ModelSql.getSerialNoSql(), null).get("jsonNode").toString();
					String model = SystemConfiguration.getProperty("serialno").toString() ;
					String deviceSerialno = jsonparser.getJsonValue(model, jsonobject.getJSONObject("retval")).toString();
					session.setAttribute("deviceuid", deviceSerialno );
					int isDeviceNoUse = deviceInit(deviceSerialno,jsonobject, session) ;
					if(isDeviceNoUse < 0)
						session.close(false) ;
	
				}
				if ( message.toString().contains("MCE Home"))
				{
					message = message.toString().replace("MCE Home", session.getAttribute("deviceuid").toString()) ;
					jsonobject = JSON.parseObject(message.toString());
				}
			}
			// 业务逻辑处理开始
			IMCECommandAction action = new MCECommandAction();
			action.doAction(message.toString(), session.getAttribute("deviceuid").toString(),session);
		} 
		catch (JSONException je)
		{
			log.info("报文解析错误 ： " + message.toString() + " 设备序列号 ：" + session.getAttribute("deviceuid")) ;
			log.info("尝试二次解析 ....") ;
			message = message.toString().replace(" ","").replace("\n", "") ;
			String[] msg = message.toString().split("\\}\\{"); 
			if ( msg.length == 0)
			{
				log.info("二次解析失败！ 向设备下发109查询命令") ;
				session.write(MCECommand.ALL_STATE_109) ;
			}
			else
			{
				String mstr ;
				//处理粘包的情况。如果109 103报文和104设备的控制报文一起发上来 ，尝试解析
				log.info("处理粘包情况：整个包中共解析出 " + msg.length + " 条报文。") ;
				for (int i = 0 ; i < msg.length ;i ++)
				{
					mstr = msg[i];
					if (i == 0)
					{
						mstr += "}" ;
					}
					else
					{
						mstr = "{" + mstr ;
					}
					log.info(mstr) ;
					//if (mstr.indexOf("oid") > 0 && JSON.parseObject(mstr).getString("oid").contains("104"))
					//{
						log.info("二次解析成功! : " + mstr) ;
						IMCECommandAction action = new MCECommandAction();
						action.doAction(mstr, session.getAttribute("deviceuid").toString(),session);
						break;
						
					//}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			log.error("Error:" + e) ;
			log.error("MCE序列号： " + session.getAttribute("deviceuid") + "报文：\n"+ message.toString());
			if(session.getAttribute("deviceuid").toString().equals(""))
			{
				 session.write("MOID 901.1.109\n") ;
			}
			else
			{
				 session.write("MOID 901.1.103\n") ;
			}
			
		}

	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		log.info(status);
	}
	
	/**
	 * @param deviceSerialno 设备序列号
	 * @return 0表示设备初始化正常，-1表示此为停用设备，直接返回 不做数据处理
	 */
	private int deviceInit(String deviceSerialno ,JSONObject jsonobject ,IoSession session)
	{
		
		InetSocketAddress inetSocketAddress = (InetSocketAddress) session.getRemoteAddress();
		String ip =  inetSocketAddress.getAddress().getHostAddress()  ; 
		int port =  inetSocketAddress.getPort() ;
		db.executeSaveOrUpdate(ModelSql.getUpdateConnectionDeviceuidSql(), new Object[]{deviceSerialno,ip ,port}) ;
		
		log.info("收到设备109报文，设备状态初始化开始。序列号：" + deviceSerialno);
		String positionid = null ;
		Map position  = db.execueQuery(ModelSql.getPositionBySerialNoSql(), new Object[]{deviceSerialno}) ;
		JSONParser jsonparser = new JSONParser();
		if (position == null || position.isEmpty())
		{
			log.info("检测到设备为首次连接服务端，准备注册设备信息:") ;
			Map kv = db.execueQuery(ModelSql.getPositionAmount(), null ) ;
			if ( ! Authorization.authorizationChecking((Long)kv.get("positionamount")) )
				
				return -1 ;
			positionid = MD5Util.getObjuid() ;
			//拿901设备描述信息
			//String model = db.execueQuery(ModelSql.getDescriptionSql(), null).get("jsonNode").toString();
			String model = SystemConfiguration.getProperty("description").toString() ;
			String description = jsonparser.getJsonValue(model, jsonobject.getJSONObject("retval")).toString();
			if ( description.equals("MCE Home") || description.indexOf("Home") > 0|| description.indexOf("MCE") > 0)
			{
				description = deviceSerialno ;
			}
			db.executeSaveOrUpdate(ModelSql.getInsertPositionSql(), new Object[]{positionid ,deviceSerialno ,description ,MCEStatus.SUCCESS}) ;
			
		}
		else
		{
			if(position.get("positionstate").toString().equals(MCEStatus.DEACTIVATED))
			{
				log.info("此设备已停用，不做信息处理。");
				return -1 ;
			}
			position  = db.execueQuery(ModelSql.getPositionHasAlarmSql(), new Object[]{deviceSerialno ,0}) ;
		
			if(position != null )
			{
				// db.executeSaveOrUpdate(ModelSql.updatePositionStatus(), new Object[]{MCEStatus.HAS_ALARM ,deviceSerialno}) ;
				db.updatePositionStatus(MCEStatus.HAS_ALARM, deviceSerialno) ;
			}
			else
			{
				//改设备状态为正常
				//db.executeSaveOrUpdate(ModelSql.updatePositionStatus(), new Object[]{MCEStatus.SUCCESS ,deviceSerialno}) ;
				db.updatePositionStatus(MCEStatus.SUCCESS, deviceSerialno) ;
			}
		}
		log.info("设备状态初始化成功! 序列号：" + deviceSerialno);
		return 0 ;
	}
	
	public static void main(String[] argvs)
	{
		File f = new File("D:\\netdisk\\workspace\\mce\\WebContent\\WEB-INF\\lib") ;
		String[] files = f.list() ;
		StringBuffer sb = new StringBuffer() ;
		for ( String name : files)
		{
			sb.append( "/WebContent/WEB_INF/lib/" + name + " " ) ;
		}
		System.out.println( sb.toString() ) ;
	}

}
