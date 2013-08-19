package com.mce.action;

import org.apache.mina.core.session.IoSession;


public interface IMCECommandAction {
	
	
	/**
	 * 各个命令处理接口
	 * @param message 收到的json报文
	 */
	public void doAction(String message ,String deviceserialno, IoSession session) throws Exception;

}
