/*
 Copyright (c) 2020 onghuilam@gmail.com
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 The Software shall be used for Good, not Evil.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 
 */

package hl.common;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

public class SmtpMgr{
	
	private Authenticator smtp_auth = null;
	private boolean enabled_starttls = false;
	
	private String smtp_host 	= null;
	private int smtp_port 		= 25;
	
	private MimeMultipart mimeMultipart = null;
	
	public SmtpMgr()
	{
	}
	
	public void setSmtpServer(String aSmtpHost, int aSmtpPort)
	{
		this.smtp_host = aSmtpHost;
		this.smtp_port = aSmtpPort;
	}
	
	public void setStartTLS(boolean isEnable)
	{
		this.enabled_starttls = isEnable;
	}
	
	public void setSmtpCredential(String uid, String pwd)
	{
		this.smtp_auth =  new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
               return new PasswordAuthentication(uid, pwd);
            }
		};
	}
	
	public Session getSmtpSession()
	{
		Session sess = null;

		Properties props = new Properties();
		props.put("mail.smtp.host", this.smtp_host);
		props.put("mail.smtp.port", this.smtp_port);

		if(this.enabled_starttls)
		{
			props.put("mail.smtp.starttls.enable", "true");  
		}

		if(this.smtp_auth==null)
		{
			sess = Session.getInstance(props);
		}
		else
		{
			props.put("mail.smtp.auth", "true");
			sess = Session.getInstance(props,this.smtp_auth);
		}
		return sess;
	}
	
	private void sendMultipartEmail(
			String aFromAddress, 
			String[] aToAddresses,
			String aEmailSubject,
			MimeMultipart aMimeMultipart) throws MessagingException
	{
		List<Address> listToAddr = new ArrayList<Address>();
		for(String sAddr : aToAddresses)
		{
			listToAddr.add(new InternetAddress(sAddr));
		}
		
		MimeMessage m = new MimeMessage(getSmtpSession());
		m.setFrom(new InternetAddress(aFromAddress));
		m.setRecipients(RecipientType.TO, listToAddr.toArray(new Address[listToAddr.size()]));
		m.setSubject(aEmailSubject);
		m.setContent(aMimeMultipart);
		
		Transport.send(m);
	}
	
	public void send(String aEmailSubject, String aFromAddress, String[] aToAddresses) throws MessagingException
	{
		sendMultipartEmail(
				aFromAddress, aToAddresses, 
				aEmailSubject, this.mimeMultipart);
		
		if(this.mimeMultipart!=null)
		{
			for(int i=0; i<this.mimeMultipart.getCount(); i++)
			{
				this.mimeMultipart.removeBodyPart(i);
			}
		}
	}
	
	public void addHtmlImageBase64(String aImageID, String aImageBase64) throws MessagingException
	{
		BodyPart p = imgBase64ToBodyPart(aImageBase64);
		p.setHeader("Content-ID", "<"+aImageID+">");
		addMimeBodyPart(p);
	}
	
	public void addImageBase64AsFile(String aImageFileName, String aImageBase64) throws MessagingException
	{
		BodyPart p = imgBase64ToBodyPart(aImageBase64);
		p.setFileName(aImageFileName);
		addMimeBodyPart(p);
	}
	
	private void addMimeBodyPart(BodyPart aPart) throws MessagingException
	{
		if(this.mimeMultipart==null)
		{
			this.mimeMultipart = new MimeMultipart();
		}
		
		this.mimeMultipart.addBodyPart(aPart);
	}
	
	private BodyPart imgBase64ToBodyPart(String aImageBase64) throws MessagingException
	{
		BodyPart p = new MimeBodyPart();
		byte[] imgBytes = Base64.getDecoder().decode(aImageBase64);
		DataSource ds = new ByteArrayDataSource(imgBytes, "image/*");
		p.setDataHandler(new DataHandler(ds));
		return p;
	}

	public void addHtmlContent(String aContent) throws MessagingException
	{
		addContent(aContent, "text/html");
	}
	
	public void addContent(String aContent, String ContentType) throws MessagingException
	{
		BodyPart p = new MimeBodyPart();
		p.setContent(aContent, ContentType);
		addMimeBodyPart(p);
	}
	
	public static void main(String args[]) throws Exception
	{
	}
}
