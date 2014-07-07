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
			// ҵ���߼�����ʼ
			IMCECommandAction action = new MCECommandAction();
			action.doAction(message.toString(), session.getAttribute("deviceuid").toString(),session);
		} 
		catch (JSONException je)
		{
			log.info("���Ľ������� �� " + message.toString() + " �豸���к� ��" + session.getAttribute("deviceuid")) ;
			log.info("���Զ��ν��� ....") ;
			message = message.toString().replace(" ","").replace("\n", "") ;
			String[] msg = message.toString().split("\\}\\{"); 
			if ( msg.length == 0)
			{
				log.info("���ν���ʧ�ܣ� ���豸�·�109��ѯ����") ;
				session.write(MCECommand.ALL_STATE_109) ;
			}
			else
			{
				String mstr ;
				//����ճ������������109 103���ĺ�104�豸�Ŀ��Ʊ���һ������ �����Խ���
				log.info("����ճ��������������й������� " + msg.length + " �����ġ�") ;
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
						log.info("���ν����ɹ�! : " + mstr) ;
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
			log.error("MCE���кţ� " + session.getAttribute("deviceuid") + "���ģ�\n"+ message.toString());
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
	 * @param deviceSerialno �豸���к�
	 * @return 0��ʾ�豸��ʼ��������-1��ʾ��Ϊͣ���豸��ֱ�ӷ��� �������ݴ���
	 */
	private int deviceInit(String deviceSerialno ,JSONObject jsonobject ,IoSession session)
	{
		
		InetSocketAddress inetSocketAddress = (InetSocketAddress) session.getRemoteAddress();
		String ip =  inetSocketAddress.getAddress().getHostAddress()  ; 
		int port =  inetSocketAddress.getPort() ;
		db.executeSaveOrUpdate(ModelSql.getUpdateConnectionDeviceuidSql(), new Object[]{deviceSerialno,ip ,port}) ;
		
		log.info("�յ��豸109���ģ��豸״̬��ʼ����ʼ�����кţ�" + deviceSerialno);
		String positionid = null ;
		Map position  = db.execueQuery(ModelSql.getPositionBySerialNoSql(), new Object[]{deviceSerialno}) ;
		JSONParser jsonparser = new JSONParser();
		if (position == null || position.isEmpty())
		{
			log.info("��⵽�豸Ϊ�״����ӷ���ˣ�׼��ע���豸��Ϣ:") ;
			Map kv = db.execueQuery(ModelSql.getPositionAmount(), null ) ;
			if ( ! Authorization.authorizationChecking((Long)kv.get("positionamount")) )
				
				return -1 ;
			positionid = MD5Util.getObjuid() ;
			//��901�豸������Ϣ
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
				log.info("���豸��ͣ�ã�������Ϣ����");
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
				//���豸״̬Ϊ����
				//db.executeSaveOrUpdate(ModelSql.updatePositionStatus(), new Object[]{MCEStatus.SUCCESS ,deviceSerialno}) ;
				db.updatePositionStatus(MCEStatus.SUCCESS, deviceSerialno) ;
			}
		}
		log.info("�豸״̬��ʼ���ɹ�! ���кţ�" + deviceSerialno);
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
