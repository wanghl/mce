package com.mce.socket.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import com.alibaba.fastjson.JSON;

public class SocketClient {
	
	
	public static void main(String[] argvs) throws UnknownHostException, IOException
	
	{
		Socket socket  = new Socket( "127.0.0.1", 10101 );
		String tmp ;
		
		socket.getOutputStream().write(getJsonMessage("d:/109.txt").getBytes()) ;
//		try {
//			Thread.sleep(20000000) ;
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		socket.setKeepAlive(true) ;
		BufferedReader reader  = new BufferedReader(new InputStreamReader(socket.getInputStream())) ;
		while( (tmp = reader.readLine()) != null)
		{
			
			if (tmp.equals("MOID 901.1.103"))
			{
				socket.getOutputStream().write(getJsonMessage("d:/103.txt").getBytes()) ;
			}
			else if (tmp.equals("MOID 901.1.109"))
			{
				socket.getOutputStream().write(getJsonMessage("d:/109.txt").getBytes()) ;
			}
			else if (tmp.equals("MOID 104.1.2"))
			{
				socket.getOutputStream().write(getJsonMessage("d:/kt.txt").getBytes()) ;
			}
			else if (tmp.equals("MOID 104.2.2"))
			{
				socket.getOutputStream().write(getJsonMessage("d:/kt02.txt").getBytes()) ;
			}
			else if (tmp.equals("MOID 104.2.12"))
			{
				System.out.println("104.2.12 " +tmp) ;
			}
			else if (tmp.equals("MOID 104.1.12"))
			{
				System.out.println("104.1.12 " +tmp) ;
			}
		}
	}
	
	public static String getJsonMessage(String filename) throws IOException
	{
		BufferedReader reader  = new BufferedReader(new InputStreamReader(new FileInputStream(filename) ,"utf8")) ;
		String tmp ;
		String jsonstring = "" ;
		while((tmp = reader.readLine()) != null)
		{
			jsonstring += tmp ;
			
		}
		reader.close() ;
		return jsonstring ;
	}

}
