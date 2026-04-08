package com.bank.util;

import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailUtil {
	
	private static final String FROM_MAIL = EnvUtil.get("BANK_ALERT_FROM_EMAIL");
	private static final String APP_PASSWORD = EnvUtil.get("BANK_ALERT_APP_PASSWORD");
	
	public static void sendEmail(String to,String subject,String body){
		if(FROM_MAIL == null || FROM_MAIL.isBlank() || APP_PASSWORD == null || APP_PASSWORD.isBlank()) {
			System.out.println("Skipping email alert because BANK_ALERT_FROM_EMAIL / BANK_ALERT_APP_PASSWORD are not set.");
			return;
		}
		
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
		
		Session session = Session.getInstance(props, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(FROM_MAIL,APP_PASSWORD);
			}
		});
		
		
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(FROM_MAIL));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			message.setSubject(subject);
			message.setText(body);
			
			Transport.send(message);
			
			System.out.println("Email sent to: "+to);
		 
			
		} catch (MessagingException e) {
			 
			System.out.println(e.getMessage());
		}
	}

}
