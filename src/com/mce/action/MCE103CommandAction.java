package com.mce.action;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mce.db.operate.DatabaseOperator;
import com.mce.json.parser.ModelObject;
import com.mce.json.parser.ModelSql;
import com.mce.socket.server.SessionManager;
import com.mce.uitl.MCEStatus;

public class MCE103CommandAction extends MCECommandAction {
	private static final Logger log = Logger.getLogger(MCE103CommandAction.class);
	
	public MCE103CommandAction(){
		commandAction = new MCEReceiveMessageAction();
	}

	@SuppressWarnings( { "unchecked", "static-access" })
	public void doAction(String message, String deviceserialno,IoSession session) throws Exception {
		JSONObject jsonobject = JSON.parseObject(message);
		if (jsonobject.getString("oid").equals("901.1.103"))
		{
			//test restart mce...
			
			
			//首先检查设备是否停用状态
			int retval = checkDeviceStatus(deviceserialno);
			if(retval == -1 )
			{
				log.info("此设备为停用状态，不做后续处理") ;
				return ;
			}
			//如果设备不存在，则直接发109报文
			if(retval == -2 )
			{
				SessionManager.write(deviceserialno, "MOID 901.1.109\n") ;
			}
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
					deviceuid = deviceserialno;
					valuemap = JSON.parseObject(entry.getValue().toJSONString(), HashMap.class);
					for (Entry<String, JSONObject> set : valuemap.entrySet())
					{
						devicenumber = set.getKey();
						deviceuid += "." + devicename + "." + devicenumber;
						// 根据deviceuid查device_config
						paras = new Object[] { deviceuid };
						datamap = db.execueQuery(ModelSql.selectDeviceByDeviceuidSql(), paras);
						
						if(datamap != null)
						{
						//update current ;
						boolean rev = updateCurrentNode(datamap.get("objuid").toString() ,devicename, devicenumber ,datamap.get("objuid").toString() ,jsonobject ,deviceserialno) ;
						// alarm
						/*if (rev)
						{
							
							processAlarmMessage(set.getValue() ,devicename ,datamap.get("objuid").toString(),deviceserialno) ;
						}*/
						
						}else
						{
							
							SessionManager.write(deviceserialno, "MOID 901.1.109\n") ;
							
						}
					}
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				log.error(e);
				throw e ;
			}
		} else
		{
			commandAction.doAction(message, deviceserialno,session) ;
		}

	}
	
	/**
	 *  检测设备是否停用 。另如果收到103报文后 ，设备还处于连接断开状态， 将其改为正常。
	 * 
	 * @param serialno
	 * @return 0 正常  -1 设备停用 -2 设备不存在
	 */
	public int checkDeviceStatus(String serialno)
	{
		int value =  0 ;
		Map map = db.execueQuery(ModelSql.getPositionBySerialNoSql(), new Object[]{serialno}) ;
		
		if(map != null)
		{
			if(map.get("positionstate").toString().equals(MCEStatus.DEACTIVATED))
			{
				value = -1  ;
			}
			else if ( map.get("positionstate").toString().equals(MCEStatus.CONNECTION_CLOSED))
			{
				db.executeSaveOrUpdate(ModelSql.updatePositionStatus(), new Object[]{MCEStatus.SUCCESS ,serialno}) ;
			}
		}
		else
		{
			value = -2 ;
		}
		
		return value ;
		
	}
	
}
