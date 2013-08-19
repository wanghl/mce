package com.mce.socket.server;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
		protocoldecoderoutput.write(tmpstr);
		if(log.isDebugEnabled())
		{
			log.debug("������ȡ�ɹ������봦���̡߳����ݣ�" + tmpstr) ;
		}
		tmpstr = "" ;
		iosession.setAttribute("jsonstring", "");
		iosession.setAttribute("receivetimes", 0) ;
		return true;
	}

}
