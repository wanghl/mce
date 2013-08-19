package com.mce.socket.server;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class MCEProtocolCodeFactory implements ProtocolCodecFactory{

	public ProtocolDecoder getDecoder(IoSession iosession) throws Exception {
		// TODO Auto-generated method stub
		return new MCEProtocolCodeDecoder();
	}

	public ProtocolEncoder getEncoder(IoSession iosession) throws Exception {
		// TODO Auto-generated method stub
		return new MCEProtocolCodeEncoder();
	}

}
