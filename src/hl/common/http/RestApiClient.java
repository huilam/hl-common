package hl.common.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hl.common.ZipUtil;

public class RestApiClient {

	public final static String _PROTOCOL_HTTP 			= "http";
	public final static String _PROTOCOL_HTTPS 			= "https";
	public final static String _PROTOCOL_SSL 			= "SSL";
	
	public final static String _HTTPMETHOD_GET 			= "GET";
	public final static String _HTTPMETHOD_POST 		= "POST";
	public final static String _HTTPMETHOD_PUT 			= "PUT";
	public final static String _HTTPMETHOD_DELETE 		= "DELETE";
	
	public final static String HEADER_AUTHORIZATION		= "Authorization";
	
	public final static String UTF_8 					= "UTF-8";
	public final static String HEADER_CONTENT_TYPE 		= "Content-Type";
	public final static String HEADER_CONTENT_ENCODING 	= "Content-Encoding";
	
	public static long DEFAULT_GZIP_THRESHOLD_BYTES 	= -1;

	public final static String TYPE_APP_JSON 		= "application/json";
	public final static String TYPE_TEXT_PLAIN 		= "text/plain";
	public final static String TYPE_ENCODING_GZIP 	= "gzip";
	
	public String basic_auth_uid = null;
	public String basic_auth_pwd = null;
	
	public boolean isAllowAnyHostSSL = false;
	public KeyStore keystoreCustom = null;
	
	private int conn_timeout	 = 5000;	
	
	public void setConnTimeout(int aTimeOutMs)
	{
		this.conn_timeout = aTimeOutMs;
	}
	
	public void setCustomKeyStore(KeyStore aCustomKeyStore)
	{
		this.keystoreCustom = aCustomKeyStore;
	}
	
	public void setAllowAnyHostSSL(boolean aAllowAnyHostSSL)
	{
		this.isAllowAnyHostSSL = aAllowAnyHostSSL;
	}

	public void setBasicAuth(String aUid, String aPwd)
	{
		this.basic_auth_uid = aUid;
		this.basic_auth_pwd = aPwd;
	}
	
	public String getReqContent(HttpServletRequest req)
	{
		StringBuffer sb = new StringBuffer();
		BufferedReader rdr = null;
		try {
			
	    	if(req.getCharacterEncoding()==null || req.getCharacterEncoding().trim().length()==0)
	    	{
	    		try {
					req.setCharacterEncoding(UTF_8);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
			
			rdr = req.getReader();
			String sLine = null;
			while((sLine=rdr.readLine())!=null)
			{
				sb.append(sLine);
			}
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		finally
		{
			if(rdr!=null)
			{
				try { rdr.close(); } catch (IOException e) { e.printStackTrace(); }
			}
		}
	}

	
    public void processHttpResp(HttpServletResponse res, 
    		int aHttpStatus, String aContentType, String aOutputContent, long aGzipBytesThreshold) throws IOException
    {
		if(aHttpStatus>0)
			res.setStatus(aHttpStatus);
		
		if(aContentType!=null)
			res.setContentType(aContentType);
		
		if(aOutputContent!=null && aOutputContent.length()>0)
		{
			byte[] byteContent 	= aOutputContent.getBytes(UTF_8);
			long lContentLen 	= byteContent.length;
			
			if(aGzipBytesThreshold>0 && lContentLen >= aGzipBytesThreshold)
			{
				byteContent = ZipUtil.compress(aOutputContent);
				lContentLen = byteContent.length;
				res.addHeader(HEADER_CONTENT_ENCODING, TYPE_ENCODING_GZIP);
			}
			
			if(res.getContentType()==null || res.getContentType().trim().length()==0)
			{
				res.setContentType(TYPE_APP_JSON);
			}
			
			res.setContentLengthLong(lContentLen);
			res.getOutputStream().write(byteContent);
		}

		res.flushBuffer();
    }

    public static HttpURLConnection setBasicAuthHeader(HttpURLConnection aConn, String aUid, String aPwd)
    {
    	return setBasicAuthHeader(aConn, aUid+":"+aPwd);
    }
    
    private static HttpURLConnection setBasicAuthHeader(HttpURLConnection aConn, String aUserInfo)
    {
    	if(aConn==null)
    		return aConn;
    	
   		String sEncodedBasicAuth = "Basic " + new String(Base64.getEncoder().encode(aUserInfo.getBytes()));
		aConn.setRequestProperty (HEADER_AUTHORIZATION, sEncodedBasicAuth);
    	
		return aConn;    	
    }
    
    private HttpURLConnection appendBasicAuth(HttpURLConnection aConn)
    {
    	if(aConn==null)
    		return aConn;
    	
    	URL url = aConn.getURL();
    	
    	String sBasicAuth = null;
    	if(url.getUserInfo()!=null)
		{
			sBasicAuth = url.getUserInfo();
		}
    	else if(this.basic_auth_uid!=null && this.basic_auth_pwd!=null)
    	{
    		sBasicAuth = this.basic_auth_uid+":"+this.basic_auth_pwd;
    	}
    	aConn = setBasicAuthHeader(aConn, sBasicAuth);
   	
		return aConn;
    }
    
    public HttpResp httpSubmit(
    		String aHttpMethod,
    		String aEndpointURL, 
    		String aContentType, 
    		String aContentData) throws IOException
    {
    	HttpResp httpResp 	= new HttpResp();
    	
    	HttpURLConnection conn 	= null;
		try {
			URL url = new URL(aEndpointURL);
			
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setConnectTimeout(conn_timeout);
			conn.setRequestProperty(HEADER_CONTENT_TYPE, aContentType);
			conn.setRequestMethod(aHttpMethod);
			
			conn = appendBasicAuth(conn);
			
			if(url.getProtocol().equalsIgnoreCase(_PROTOCOL_HTTPS))
			{
				HttpsURLConnection httpsconn = (HttpsURLConnection)conn;
				
				try {
					if(this.isAllowAnyHostSSL)
					{
						httpsconn.setSSLSocketFactory(getTrustAnyHostSSLSocketFactory());
					}
					else if (this.keystoreCustom!=null)
					{
						httpsconn.setSSLSocketFactory(getTrustCustomKeystoreSSLSocketFactory());
					}
				} catch (KeyManagementException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (KeyStoreException e) {
					e.printStackTrace();
				}
			}
			
			
			OutputStream out = conn.getOutputStream();
			
			BufferedWriter writer 	= null;
			try {
				writer = new BufferedWriter(new OutputStreamWriter(out, UTF_8));
				writer.write(aContentData);
				out.flush();
			}
			finally
			{
				if(writer!=null)
					writer.close();
			}
			httpResp.setHttp_status(conn.getResponseCode());
			httpResp.setHttp_status_message(conn.getResponseMessage());
			
	    	StringBuffer sb = new StringBuffer();
	    	InputStream in 	= null;
			try {
				BufferedReader reader = null;
				try {

					if(conn.getResponseCode()>=400)
					{
						in = conn.getErrorStream();
					}
					else
					{
						in = conn.getInputStream();
					}
					
					if(in!=null)
					{
						reader = new BufferedReader(new InputStreamReader(in, UTF_8));
						String sLine = null;
						while((sLine = reader.readLine())!=null)
						{
							sb.append(sLine);
						}
					}
				}
				catch(IOException ex)
				{
					//no data
				}
				finally
				{
					if(reader!=null)
						reader.close();
				}
			}
			finally
			{
				if(in!=null)
					in.close();
			}
			
			httpResp.setContent_type(conn.getHeaderField(HEADER_CONTENT_TYPE));
			
			if(sb.length()>0)
			{
				httpResp.setContent_data(sb.toString());
			}
						
		}
		catch(FileNotFoundException ex)
		{
			throw new IOException("Invalid URL : "+aEndpointURL);
		}
 	
    	return httpResp;
    }
    
    private static SSLSocketFactory getTrustAnyHostSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException
    {
    	TrustManager trustmgr = new X509ExtendedTrustManager() {

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType)
					throws CertificateException {}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType)
					throws CertificateException {}
			
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
					throws CertificateException {}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
					throws CertificateException {}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
					throws CertificateException {}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
					throws CertificateException {}
		};
		
		
		SSLContext sc = SSLContext.getInstance(_PROTOCOL_SSL);
		sc.init(null, new TrustManager[]{trustmgr}, new SecureRandom());
		return sc.getSocketFactory();
    }
    
    private SSLSocketFactory getTrustCustomKeystoreSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException
    {
    	TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    	tmf.init(this.keystoreCustom);
		SSLContext sc = SSLContext.getInstance(_PROTOCOL_SSL);
		sc.init(null, tmf.getTrustManagers(), new SecureRandom());
		return sc.getSocketFactory();
    }
    
    public HttpResp httpDelete(String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	return httpSubmit(_HTTPMETHOD_DELETE, aEndpointURL, aContentType, aContentData);
    }
        
    public HttpResp httpPost(String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	return httpSubmit(_HTTPMETHOD_POST, aEndpointURL, aContentType, aContentData);
    }
    
    public HttpResp httpPut(String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	return httpSubmit(_HTTPMETHOD_PUT, aEndpointURL, aContentType, aContentData);
    }
    
    public HttpResp httpGet(String aEndpointURL) throws IOException
    {
    	HttpResp httpResp 		= new HttpResp();
    	
		HttpURLConnection conn 	= null;
		InputStream in 			= null;
		try {
	    	
	    	URL url = new URL(aEndpointURL);
	    	
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(this.conn_timeout);
			conn = appendBasicAuth(conn);
			
			if(url.getProtocol().equalsIgnoreCase(_PROTOCOL_HTTPS))
			{
				HttpsURLConnection httpsconn = (HttpsURLConnection)conn;
				try {
					if(this.isAllowAnyHostSSL)
					{
						httpsconn.setSSLSocketFactory(getTrustAnyHostSSLSocketFactory());
					}
					else if (this.keystoreCustom!=null)
					{
						httpsconn.setSSLSocketFactory(getTrustCustomKeystoreSSLSocketFactory());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				httpsconn.connect();
			}
			
			try {
				
				boolean isGzipEncoded = TYPE_ENCODING_GZIP.equalsIgnoreCase(conn.getHeaderField(HEADER_CONTENT_ENCODING));
				
		    	StringBuffer sb = new StringBuffer();
				try {
					
					BufferedInputStream stream = null;
					ByteArrayOutputStream baos = null;
					try {
						baos = new ByteArrayOutputStream();
						
						if(conn.getResponseCode()>=400)
						{
							in = conn.getErrorStream();
						}
						else
						{
							in = conn.getInputStream();
						}
						stream = new BufferedInputStream(in);

						if(stream.available()>0)
						{
							byte[] byteRead = new byte[4096];
							int iBytes;
							while((iBytes = stream.read(byteRead)) != -1)
							{
								baos.write(byteRead, 0, iBytes);
							}
							
							if(isGzipEncoded)
							{
								sb.append(ZipUtil.decompress(baos.toByteArray()));
							}
							else
							{
								sb.append(baos.toString(UTF_8));
							}
						}
						else
						{
							sb.append(conn.getResponseMessage());
						}
					}
					catch(IOException ex)
					{
						//no data
					}
					finally
					{
						try {
							if(baos!=null)
								baos.close();
						}catch(IOException ex) {}
						
						try {
							if(stream!=null)
								stream.close();
						}catch(IOException ex) {}
					}
				}
				finally
				{
					if(in!=null)
						in.close();
				}
				
				httpResp.setHttp_status(conn.getResponseCode());
				httpResp.setHttp_status_message(conn.getResponseMessage());
				httpResp.setContent_type(conn.getHeaderField(HEADER_CONTENT_TYPE));
				
				if(sb.length()>0)
				{
					httpResp.setContent_data(sb.toString());
				}
			}
			finally
			{
				if(in!=null)
					in.close();
			}
		}
		catch(FileNotFoundException ex)
		{
			throw new IOException("Invalid URL : "+aEndpointURL);
		}
		
		return httpResp;
    }  
    
    public boolean ping(String aEndpointURL, int aPingTimeOutMs) 
    {
		HttpURLConnection conn 	= null;
	    	
	    	URL url = null;
	    	
	    	try {
				url = new URL(aEndpointURL);
			} catch (MalformedURLException e) {
				e.printStackTrace(System.err);
				return false;
			}
	    	
			try {
				conn = (HttpURLConnection) url.openConnection();
			} catch (IOException e) {
				e.printStackTrace(System.err);
				return false;
			}
			
			if(aPingTimeOutMs<=0)
			{
				aPingTimeOutMs = this.conn_timeout;
			}
			conn.setConnectTimeout(aPingTimeOutMs);
			
			try {
				conn.connect();
			} catch (IOException e) {
				e.printStackTrace(System.err);
				return false;
			}
			
			return true;
    }
    
	
    public static void main(String args[]) throws Exception
    {
    	
    }
}