package com.mce.socket.server;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mce.action.SystemConfiguration;
import com.mce.uitl.MCEStatus;
import com.mce.uitl.MCEUtil;

public class MCEProtocolCodeDecoder extends CumulativeProtocolDecoder {
	Logger log = Logger.getLogger(MCEProtocolCodeDecoder.class);
	
	String tmpstr = "" ;
	@Override
	protected boolean doDecode(IoSession iosession, IoBuffer iobuffer,
			ProtocolDecoderOutput protocoldecoderoutput) throws Exception {
		
		Charset ch  = Charset.forName("GBK") ;
		CharsetDecoder decoder = ch.newDecoder() ;
		String receive  ;
		tmpstr = iosession.getAttribute("jsonstring").toString();
		while (iobuffer.hasRemaining())
		{
			receive = iobuffer.getString(decoder) ;
	
			if (log.isDebugEnabled())
			{
				log.debug("已缓存报文：" + iosession.getAttribute("jsonstring")); 
				
			}
			tmpstr = iosession.setAttribute("jsonstring").toString() ;
			tmpstr += receive ;
//			try{
//				JSON.parseObject(tmpstr);
//				break ;
//			}
//			catch(Exception e)
//			{
//				iosession.setAttribute("jsonstring", tmpstr);			
//				return false ;
//			}
			if(MCEUtil.getCharCount(tmpstr, '{') == MCEUtil.getCharCount(tmpstr, '}'))
			{
				break ;
			}
			else
			{
				if (log.isDebugEnabled())
				{
					log.debug("缓存不完整报文。 长度:" + tmpstr.length());
				}
				Integer reTimes = (Integer) iosession.getAttribute("receivetimes") ;
				if ( (reTimes + 1) >= 10)
				{
					iosession.close(true) ;
				}
				else
				{
					iosession.setAttribute("receivetimes" ,reTimes + 1) ;
				}
				iosession.setAttribute("jsonstring", tmpstr);
				
				return false ;
			}
			
		}
		if ( tmpstr.contains("901.1.103"))
		{
			iosession.setAttribute("status" ,MCEStatus.READ_STATUS) ;
		}
		if(log.isDebugEnabled())
		{
			log.debug("报文收取成功 ，内容：" + tmpstr + " 开始校验报文合法性....") ;
		}
		
		//  校验报文合法性。 主要检查是否新加了设备。如果新加了设备而没创建对于的表，后续处理会报错。
		//  现改为：对于新加设备未加对应数据库表的情况　，放弃新设备信息的处理。
		JSONObject mainJson = JSON.parseObject(tmpstr);
		JSONObject jsonobject = mainJson.getJSONObject("retval");
		if( mainJson.getString("oid").equals("901.1.109") || mainJson.getString("oid").equals("901.1.103"))
		{
			Map<String, JSONObject> map = jsonobject.parseObject(jsonobject.toJSONString(),
					HashMap.class);
			for ( Entry<String, JSONObject> ent : map.entrySet() )
		
			{
				if ( ent.getKey() .equals( "911"))
					continue ;
				if ( ! SystemConfiguration.isDeviceTableExists(  "device" + ent.getKey() + "_p" ) )
				{
					mainJson.getJSONObject("retval").remove(ent.getKey()) ;
					log.warn("检测到报文中增加了新节点（设备） ，但系统未添加对于的数据库表做该设备的数据解析存贮。");
					log.warn("忽略设备信息。节点名称： " + ent.getKey()  + " 节点报文内容： " + ent.getValue());
				}
			}
			protocoldecoderoutput.write( mainJson.toJSONString());
		}
		else {
			protocoldecoderoutput.write( tmpstr );
		}
		tmpstr = "" ;
		iosession.setAttribute("jsonstring", "");
		iosession.setAttribute("receivetimes", 0) ;
		return true;
	}

}
