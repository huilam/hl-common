package hl.common.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private static Logger defaultlogger = Logger.getLogger(RestApiClient.class.getName());
	
	private Logger logger = null;
	
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
	
	public final static String HTTP 		= "http";
	public final static String HTTPwithSSL 	= "https";
	
	public String basic_auth_uid = null;
	public String basic_auth_pwd = null;
	
	public boolean isAllowAnyHostSSL 		= false;
	public KeyStore keystoreCustom 			= null;
	private Map<String, String> mapHeaders 	= new HashMap<String, String>();
	private static Object objectSyn 		= new Object(); 
	
	private static Map<KeyStore, SSLContext> mapKeystoreSSLContext = new HashMap<KeyStore, SSLContext>();
	private static SSLContext anyHostSSLContext 	= null;
	
	private static String[] strBinaryContentTypes 	= new String[] {"octet-stream","image","audio"};
	
	private int conn_timeout	 	= 5000;
	private int read_timeout	 	= 30000;
	
	public void setConnTimeout(int aTimeOutMs)
	{
		this.conn_timeout = aTimeOutMs;
	}
	
	public void setReadTimeout(int aTimeOutMs)
	{
		this.read_timeout = aTimeOutMs;
	}
	
	public void setCustomLogger(Logger aLogger)
	{
		logger = aLogger;
	}

	public Logger getLogger()
	{
		return logger;
	}

	public KeyStore getCustomKeyStore()
	{
		return this.keystoreCustom;
	}
	
	public void setCustomKeyStore(KeyStore aCustomKeyStore)
	{
		this.keystoreCustom = aCustomKeyStore;
	}
	
	public void setAllowAnyHostSSL(boolean aAllowAnyHostSSL)
	{
		this.isAllowAnyHostSSL = aAllowAnyHostSSL;
	}
	
	public void addHeaders(HttpServletRequest req)
	{
		Enumeration<String> headers = req.getHeaderNames();
		while(headers.hasMoreElements())
		{
			String sHeader = headers.nextElement();
			String sValue = req.getHeader(sHeader);
			mapHeaders.put(sHeader, sValue);
		}
	}


	
	public void setBasicAuth(String aUid, String aPwd)
	{
		this.basic_auth_uid = aUid;
		this.basic_auth_pwd = aPwd;
	}
	
	public String getBasic_auth_uid() {
		return basic_auth_uid;
	}

	public String getBasic_auth_pwd() {
		return basic_auth_pwd;
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
    	HttpResp httpResp = new HttpResp();
    	httpResp.setHttp_status(aHttpStatus);
    	httpResp.setContent_type(aContentType);
    	httpResp.setContent_data(aOutputContent);
    	processHttpResp(res, httpResp, aGzipBytesThreshold);
    }
    	
	
	
	
    public void processHttpResp(HttpServletResponse res, HttpResp aHttpResp, long aGzipBytesThreshold) throws IOException
    {
		if(aHttpResp.getHttp_status()>0)
			res.setStatus(aHttpResp.getHttp_status());
		
		if(aHttpResp.getContent_type()!=null)
			res.setContentType(aHttpResp.getContent_type());
		
		if(aHttpResp.getContent_data()!=null)
		{
			byte[] byteContent 	= null;
			
			if(aHttpResp.isBytesContent())
			{
				byteContent = aHttpResp.getContentInBytes();
			}
			else
			{
				byteContent = aHttpResp.getContent_data().getBytes(UTF_8);
			}
			
			if(aGzipBytesThreshold>0 
					&& (byteContent.length) >= aGzipBytesThreshold)
			{
				byteContent = ZipUtil.compress(byteContent);
				res.addHeader(HEADER_CONTENT_ENCODING, TYPE_ENCODING_GZIP);
			}
			
			if(mapHeaders!=null && mapHeaders.size()>0)
			{
				for(String sHeaderKey : mapHeaders.keySet())
				{
					String sHeaderVal = mapHeaders.get(sHeaderKey);
					res.addHeader(sHeaderKey, sHeaderVal);
				}
			}
			
			if(res.getContentType()==null || res.getContentType().trim().length()==0)
			{
				res.setContentType(TYPE_APP_JSON);
			}
			
			res.setContentLengthLong(byteContent.length);
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
    	if(aConn==null || aUserInfo==null)
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
    	long rid = System.nanoTime();
    	
    	log(Level.FINE, "[START] "+"rid:("+rid+") "+aHttpMethod+" "+ aEndpointURL+" content-type:"+aContentType+" content-data:"+aContentData);

    	HttpResp httpResp 	= new HttpResp();
    	
    	HttpURLConnection conn 	= null;
		try {
			URL url = new URL(aEndpointURL);
			
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setConnectTimeout(this.conn_timeout);
			conn.setReadTimeout(this.read_timeout);
			conn.setRequestProperty(HEADER_CONTENT_TYPE, aContentType);
			conn.setRequestMethod(aHttpMethod);
			
			if(mapHeaders!=null && mapHeaders.size()>0)
			{
				for(String sHeaderKey : mapHeaders.keySet())
				{
					String sHeaderVal = mapHeaders.get(sHeaderKey);
					conn.setRequestProperty(sHeaderKey, sHeaderVal);
				}
			}
			
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
						httpsconn.setSSLSocketFactory(getTrustCustomKeystoreSSLSocketFactory(this.keystoreCustom));
					}
				} catch (KeyManagementException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (KeyStoreException e) {
					e.printStackTrace();
				}
			}
			
			if(aContentData!=null && aContentData.trim().length()>0)
			{
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
			}
			httpResp.setRequest_url(conn.getURL().toString());
			httpResp.setServer_ip(conn.getURL().getHost());
			
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
		
    	log(Level.FINE, "[END] "+"rid:("+rid+") elapsed:"+calcNanoElapsedInWords(rid)+" "+aHttpMethod+" "+ aEndpointURL+" : "+httpResp);

    	return httpResp;
    }
    
    public static boolean isBinaryContentType(String aContentType)
    {
    	boolean isBinary = false;
    	for(int i=0; i<strBinaryContentTypes.length && !isBinary; i++)
    	{
    		if(strBinaryContentTypes[i].indexOf(aContentType)>-1)
    		{
    			isBinary = true;
    		}
    	}
    	return isBinary;
    }
    
    public static SSLSocketFactory getTrustAnyHostSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException
    {
    	SSLContext sc = anyHostSSLContext;
    	if(sc==null)
    	{
    		sc = anyHostSSLContext;
    		synchronized(objectSyn)
    		{
    	    	if(sc==null)
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
					
					sc = SSLContext.getInstance(_PROTOCOL_SSL);
					sc.init(null, new TrustManager[]{trustmgr}, new SecureRandom());
					anyHostSSLContext = sc;
    	    	}
    		}
    	}
		return sc.getSocketFactory();
    }
    
    public static SSLSocketFactory getTrustCustomKeystoreSSLSocketFactory(KeyStore aKeyStore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException
    {
    	SSLContext sc = mapKeystoreSSLContext.get(aKeyStore);
    	if(sc==null)
    	{
    		synchronized(objectSyn)
    		{
    			sc = mapKeystoreSSLContext.get(aKeyStore);
    	    	if(sc==null)
    	    	{
			    	TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			    	tmf.init(aKeyStore);
					sc = SSLContext.getInstance(_PROTOCOL_SSL);
					sc.init(null, tmf.getTrustManagers(), new SecureRandom());
					mapKeystoreSSLContext.put(aKeyStore, sc);
    	    	}
    		}
    	}
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
    	long rid = System.nanoTime();
    	log(Level.FINE, "[START] rid:("+rid+") GET "+ aEndpointURL);
    	
    	HttpResp httpResp 		= new HttpResp();
    	setAllowAnyHostSSL(true);
    	
		HttpURLConnection conn 	= null;
		InputStream in 			= null;
		try {
	    	
	    	URL url = new URL(aEndpointURL);
	    	
			conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(true);
			conn.setConnectTimeout(this.conn_timeout);
			conn.setReadTimeout(this.read_timeout);
			
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
						httpsconn.setSSLSocketFactory(getTrustCustomKeystoreSSLSocketFactory(this.keystoreCustom));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if(conn.usingProxy())
				{
					System.out.println("Using Proxy");
				}
				httpsconn.connect();
			}
			else
			{
				conn.connect();
				if(conn.getResponseCode()>=300 && conn.getResponseCode()<400)
				{
					//redirect
					String sNewUrl = conn.getHeaderField("Location");
					String sCookies = conn.getHeaderField("Set-Cookie");

					// open the new connnection again
					conn = (HttpURLConnection) new URL(sNewUrl).openConnection();
					conn.setRequestProperty("Cookie", sCookies);
					
					conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
					conn.addRequestProperty("User-Agent", "Mozilla");
					conn.addRequestProperty("Referer", "google.com");
					
					conn.connect();
				}
			}
			
			
			try {
				
				boolean isGzipEncoded = TYPE_ENCODING_GZIP.equalsIgnoreCase(conn.getHeaderField(HEADER_CONTENT_ENCODING));
				
		    	//StringBuffer sb = new StringBuffer();
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
						
						//# conn.getContentLength()==-1 unknown size
						if(in!=null && (conn.getContentLength()!=0))
						{
							byte[] byteRead = new byte[4096];
							int iBytes;
							while((iBytes = stream.read(byteRead)) != -1)
							{
								baos.write(byteRead, 0, iBytes);
							}
							baos.flush();
							
							if(baos.size()>0)
							{
								if(isGzipEncoded)
								{
									byte[] byteDecompressed = ZipUtil.decompress(baos.toByteArray());
									baos.reset();
									baos.write(byteDecompressed, 0, byteDecompressed.length);
									baos.flush();
								}
								
								
								String sContentType = conn.getHeaderField(HEADER_CONTENT_TYPE);
								if(sContentType==null)
									sContentType = "";
								
								byte[] byteContent = baos.toByteArray();
								baos.reset();
								
								if(byteContent.length>0)
								{
									if(isBinaryContentType(sContentType))
									{
										httpResp.setContent_bytes(byteContent);
									}
									else
									{
										httpResp.setContent_data(new String(byteContent));
									}
								}
							}
							
						}
						else
						{
							httpResp.setContent_data(conn.getResponseMessage());
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
				httpResp.setRequest_url(conn.getURL().toString());
				httpResp.setServer_ip(conn.getURL().getHost());
				
				httpResp.setHttp_status(conn.getResponseCode());
				httpResp.setHttp_status_message(conn.getResponseMessage());
				httpResp.setContent_type(conn.getHeaderField(HEADER_CONTENT_TYPE));
			}
			finally
			{
				if(in!=null)
					in.close();
				
				if(conn!=null)
				{
					conn.disconnect();
				}
			}
		}
		catch(FileNotFoundException ex)
		{
			throw new IOException("Invalid URL : "+aEndpointURL);
		}
    	log(Level.FINE, "[END] rid:("+rid+") elapsed:"+calcNanoElapsedInWords(rid)+" GET "+ aEndpointURL+" : "+httpResp);
		return httpResp;
    }  
    
    public static String calcNanoElapsedInWords(long aNanoStart)
    {
    	return time2Words(System.nanoTime()-aNanoStart);
    }
    
    public static String time2Words(long aElapsed)
    {
    	if(aElapsed<=0)
    		return "0ms";
    	
    	StringBuffer sb = new StringBuffer();
    	
    	long elapsedms = aElapsed;
    	
    	if(aElapsed>=1000000)
    	{
    		//convert nano to milisecs
    		elapsedms = aElapsed/1000000;
    	}
    	
    	if(elapsedms>=60000)
    	{
	    	long elapsedMins = elapsedms/60000;
	    	sb.append(elapsedMins).append("m ");
	    	
	    	elapsedms = elapsedms % 60000;
    	}

    	if(elapsedms>=1000)
    	{
	    	long elapsedSecs = elapsedms/1000;
	    	sb.append(elapsedSecs).append("s ");
	    	
	    	elapsedms = elapsedms % 1000;
    	}
    	    	
    	sb.append(elapsedms).append("ms ");
    	
    	return sb.toString();
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
    
	public Certificate getSSLCert(String aCertUrl) throws CertificateException, IOException
	{
    	Certificate cert 	= null;
		HttpResp res 		= null;
    	InputStream in 		= null;
    	
		RestApiClient apiClient = new RestApiClient();
		apiClient.setAllowAnyHostSSL(true);
		res = apiClient.httpGet(aCertUrl);
		apiClient = null;
		if(res.isSuccess())
		{
			String sRespData = null;
	    	try {
	    		
	    		sRespData = res.getContent_data();
	    		
	    		in = new ByteArrayInputStream(sRespData.getBytes());
	    		CertificateFactory X509CertFactory = CertificateFactory.getInstance("X509");
	    		if(in!=null && X509CertFactory!=null)
	    		{
	    			if(in.available()>100)
	    			{
		    			cert = X509CertFactory.generateCertificate(in);
	    			}
	    			else
	    			{
	    				log(Level.SEVERE, "Invalid certificate content: ["+res.getContent_data()+"]");
	    			}
	    		}
	    	}
	    	catch(Exception ex)
	    	{
	    		System.out.println(sRespData+" - "+ex.getMessage());
	    		throw ex;
	    	}
	    	finally
	    	{
	    		if(in!=null)
	    			in.close();
	    	}
		}
		else
		{
			log(Level.SEVERE, "Fail to get cert - "+res.getHttp_status()+" content: ["+res.getContent_data()+"]");
		}
    	return cert;
	}
	
	private void log(Level aLogLevel, String aLogMessage)
	{
		if(logger==null)
		{
			logger = defaultlogger;
		}
		
		logger.log(aLogLevel, aLogMessage);
	}
	
    public static void main(String args[]) throws Exception
    {
    }
}