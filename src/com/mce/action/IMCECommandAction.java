package com.mce.action;

import org.apache.mina.core.session.IoSession;


public interface IMCECommandAction {
	
	
	/**
	 * ���������ӿ�
	 * @param message �յ���json����
	 */
	public void doAction(String message ,String deviceserialno, IoSession session) throws Exception;

}
