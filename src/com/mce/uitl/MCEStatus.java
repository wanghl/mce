package com.mce.uitl;

public class MCEStatus {
	
	//初始化状态
	public static final String CONNECTION_OPENED = "0" ;
	//正常状态
	public static final String SUCCESS = "1" ;
	//有报警
	public static final String HAS_ALARM = "2" ;
	//连接关闭
	public static final String CONNECTION_CLOSED = "3" ;
	//停用
	public static final String DEACTIVATED = "99" ;
	//通道上一状态为读
	public static final String READ_STATUS = "read_status"  ;
	//通道上一状态为写 
	public static final String WRITE_STATUS = "write_status"  ;
	
	 //客户端超时关闭
	public static final String CONNECTION_IDEL_CLOSED = "4" ;
	
	//服务端主动断开
	public static final String SERVER_SIDE_CLOSE = "5";
	//服务停止运行
	
	public static final String SERVER_CLOSED = "6" ;
	
	

}
