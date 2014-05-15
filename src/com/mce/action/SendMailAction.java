package com.mce.action;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.mce.db.operate.DatabaseOperator;
import com.mce.uitl.ErrorLogUtil;

public class SendMailAction {
	
	public static void sendMail(String receiver ,String title ,String content)
	{
		 	Properties props = new Properties();
	        Session sendsession;
	        Transport transport;
	        MimeMessage message = null;
	        BodyPart messageBodyPart = new MimeBodyPart();
	        Multipart multipart = new MimeMultipart();
	        DatabaseOperator db = new DatabaseOperator() ;
	        String from = "xbwolf@sina.cn" ;
	        
	        try
	        {
	        	sendsession  = Session.getDefaultInstance(props, null) ;
	        	props.put("mail.smtp.host", "smtp.sina.cn") ;
	        	props.put("mail.smtp.auth", "true");
	        	sendsession.setDebug(true) ;
	        	
	            message = new MimeMessage(sendsession);
	            message.setFrom(new InternetAddress(from, "MCE"));
	            
	            InternetAddress[] toEmail = new InternetAddress().parse(receiver);
	            message.setRecipients(Message.RecipientType.TO,toEmail);
	            
	            message.setSubject(title)  ;
	            message.setSentDate(new Date()) ;
	            
	            message.setText(content) ;
	            
	            messageBodyPart.setContent(content, "text/html;charset=utf-8");
	            
	            multipart.addBodyPart(messageBodyPart);
	            
	            message.setContent(multipart);

	            //保存对于e-mail的修改
	            message.saveChanges();
	            //根据Session生成Transport对象
	            transport = sendsession.getTransport("smtp");
	            //连接到SMTP服务器
	            transport.connect("smtp.sina.cn", "xbwolf@sina.cn", "whlzcy");
	            //发送e-mail
	            transport.sendMessage(message, message.getAllRecipients());
	            //关闭Transport连接
	            transport.close();
	            
	        	  
	        }catch(Exception e){
	        	
				Map datamap = ErrorLogUtil.getErrorInfoMap(ErrorLogUtil.SENDMAIL_ERROR_CODE, e.getMessage(), "邮件发送错误", "执行位置：SendMailAction" + e.getMessage());
				db.saveErrorLog(datamap);
	        }
		
	}
	
	public static void main(String[] argvs)
	{
	       String content = "lalalalallall";
		//SendMailAction.sendMail("xbwolf@sina.cn,wanghongliang@zephyr.com.cn", content);
	}

}
