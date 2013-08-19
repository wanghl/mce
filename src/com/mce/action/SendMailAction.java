package com.mce.action;

import java.util.Date;
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

public class SendMailAction {
	
	public static void sendMail(String receiver ,String content)
	{
		 	Properties props = new Properties();
	        Session sendsession;
	        Transport transport;
	        MimeMessage message = null;
	        BodyPart messageBodyPart = new MimeBodyPart();
	        Multipart multipart = new MimeMultipart();
	        
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
	            
	            message.setSubject("MCE �豸������־")  ;
	            message.setSentDate(new Date()) ;
	            
	            message.setText(content) ;
	            
	            messageBodyPart.setContent(content, "text/html;charset=utf-8");
	            
	            multipart.addBodyPart(messageBodyPart);
	            
	            message.setContent(multipart);

	            //�������e-mail���޸�
	            message.saveChanges();
	            //����Session����Transport����
	            transport = sendsession.getTransport("smtp");
	            //���ӵ�SMTP������
	            transport.connect("smtp.sina.cn", "xbwolf@sina.cn", "whlzcy");
	            //����e-mail
	            transport.sendMessage(message, message.getAllRecipients());
	            //�ر�Transport����
	            transport.close();
	            
	        	  
	        }catch(Exception e){
	        	
	        	e.printStackTrace() ;
	        	
	        }
		
	}
	
	public static void main(String[] argvs)
	{
	       String content = "lalalalallall";
		SendMailAction.sendMail("xbwolf@sina.cn,wanghongliang@zephyr.com.cn", content);
	}

}