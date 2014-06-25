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
				log.debug("�ѻ��汨�ģ�" + iosession.getAttribute("jsonstring")); 
				
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
					log.debug("���治�������ġ� ����:" + tmpstr.length());
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
			log.debug("������ȡ�ɹ� �����ݣ�" + tmpstr + " ��ʼУ�鱨�ĺϷ���....") ;
		}
		
		//  У�鱨�ĺϷ��ԡ� ��Ҫ����Ƿ��¼����豸������¼����豸��û�������ڵı���������ᱨ��
		//  �ָ�Ϊ�������¼��豸δ�Ӷ�Ӧ���ݿ�����������������豸��Ϣ�Ĵ���
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
					log.warn("��⵽�������������½ڵ㣨�豸�� ����ϵͳδ��Ӷ��ڵ����ݿ�������豸�����ݽ���������");
					log.warn("�����豸��Ϣ���ڵ����ƣ� " + ent.getKey()  + " �ڵ㱨�����ݣ� " + ent.getValue());
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
