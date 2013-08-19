package com.mce.socket.server;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class MCEProtocolCodeEncoder  implements ProtocolEncoder {
	Logger log = Logger.getLogger(MCEProtocolCodeEncoder.class);

	public void dispose(IoSession iosession) throws Exception {
		// TODO Auto-generated method stub
		log.info("dispose" + iosession);
	}

	public void encode(IoSession iosession, Object obj, ProtocolEncoderOutput protocolencoderoutput)
			throws Exception {
		// TODO Auto-generated method stub
		
		String value = obj.toString();
		IoBuffer buf = IoBuffer.allocate(length(value)); 
		buf.put(value.getBytes("GBK"));
		buf.setAutoExpand(true);
		buf.flip();
		protocolencoderoutput.write(buf);
		
	}
	
	public static int length(String value) {
        int valueLength = 0;
        String chinese = "[\u0391-\uFFE5]";
        /* ��ȡ�ֶ�ֵ�ĳ��ȣ�����������ַ�����ÿ�������ַ�����Ϊ2������Ϊ1 */
        for (int i = 0; i < value.length(); i++) {
            /* ��ȡһ���ַ� */
            String temp = value.substring(i, i + 1);
            /* �ж��Ƿ�Ϊ�����ַ� */
            if (temp.matches(chinese)) {
                /* �����ַ�����Ϊ2 */
                valueLength += 2;
            } else {
                /* �����ַ�����Ϊ1 */
                valueLength += 1;
            }
        }
        return valueLength;
    }
	
}
