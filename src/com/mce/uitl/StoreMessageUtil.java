package com.mce.uitl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Date;

import org.apache.mina.core.session.IoSession;

public class StoreMessageUtil {
	
	private static String getConfiguration() throws IOException {
		InputStream in = new FileInputStream ( Authorization.class.getResource("/configuration.properties").getFile() ) ;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String jsonstring = reader.readLine();
		reader.close();
		return jsonstring;
	}
	
	public static void storeMessage2LogFile( IoSession iosession ,String message ) throws IOException
	{
		String cfg = getConfiguration() ;
		if ( ! cfg.contains("&") || ! cfg.contains("on"))
		{
			return ;
		}
		String path = cfg.split("&")[0] ;
		InetSocketAddress inetSocketAddress = (InetSocketAddress) iosession.getRemoteAddress();
		String ip =  inetSocketAddress.getAddress().getHostAddress().replace(".", "");
		File file = new File( path +  ip + "-" + MCEUtil.getCurrentDate() + ".log" ); 
		FileWriter  fw = new FileWriter(file, true);
		
		PrintWriter pw = new PrintWriter(fw);
		
		pw.println( new Date() + "\n" + message + "\n\n\n===========================================");
		pw.flush(); 
		fw.flush();
		pw.close(); 
		fw.close(); 
	}

}
