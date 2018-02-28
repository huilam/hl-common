package hl.common.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;


public class RestApiUtil {

	public final static String HEADER_CONTENT_TYPE 	= "content-type";
	public final static String TYPE_APP_JSON 		= "application/json";
	public final static String TYPE_TEXT_PLAIN 		= "text/plain";
	
	private static int conn_timeout = 5000;
	
	public static void setConnTimeout(int aTimeOutMs)
	{
		conn_timeout = aTimeOutMs;
	}
	
	public static String getReqContent(HttpServletRequest req)
	{
		StringBuffer sb = new StringBuffer();
		BufferedReader rdr = null;
		try {
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
	
    
	public static void processHttpResp(HttpServletResponse res, 
    		int aHttpStatus, JSONObject aJsonContent) throws IOException
	{
		processHttpResp(res, aHttpStatus, TYPE_APP_JSON, aJsonContent.toString());
	}
	
    public static void processHttpResp(HttpServletResponse res, 
    		int aHttpStatus, String aContentType, String aOutputContent) throws IOException
    {
		if(aHttpStatus>0)
			res.setStatus(aHttpStatus);
		
		if(aContentType!=null)
			res.setContentType(aContentType);
		
		if(aOutputContent!=null && aOutputContent.length()>0)
		{
			res.getWriter().println(aOutputContent);
		}

		res.flushBuffer();
    }
	
    public static HttpResp httpDelete(String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	return httpSubmit("DELETE", aEndpointURL, aContentType, aContentData);
    }
        
    public static HttpResp httpPost(String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	return httpSubmit("POST", aEndpointURL, aContentType, aContentData);
    }
    
    public static HttpResp httpPut(String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	return httpSubmit("PUT", aEndpointURL, aContentType, aContentData);
    }
    
    private static HttpResp httpSubmit(
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
			conn.setRequestMethod(aHttpMethod);
			conn.setRequestProperty(HEADER_CONTENT_TYPE, aContentType);
			
			OutputStream out 		= conn.getOutputStream();
			BufferedWriter writer 	= null;
			
			try {
				writer = new BufferedWriter(new OutputStreamWriter(out));
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
			
			if(conn.getResponseCode()>=200 || conn.getResponseCode()<300)
			{
		    	StringBuffer sb = new StringBuffer();
		    	InputStream in 	= null;
				try {
					BufferedReader reader = null;
					try {
						in = conn.getInputStream();
						reader = new BufferedReader(new InputStreamReader(in));
						String sLine = null;
						while((sLine = reader.readLine())!=null)
						{
							sb.append(sLine);
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
			
		}
		catch(FileNotFoundException ex)
		{
			throw new IOException("Invalid URL : "+aEndpointURL);
		}
 	
    	return httpResp;
    }
    
    public static HttpResp httpGet(String aEndpointURL) throws IOException
    {
    	return httpGet(aEndpointURL, 0);
    }
    
    public static HttpResp httpGet(String aEndpointURL, int aTimeOutMs) throws IOException
    {
    	HttpResp httpResp 		= new HttpResp();
    	
		HttpURLConnection conn 	= null;
		InputStream in 			= null;
		try {
	    	
	    	URL url = new URL(aEndpointURL);
	    	
			conn = (HttpURLConnection) url.openConnection();
			if(aTimeOutMs<=0)
			{
				aTimeOutMs = conn_timeout;
			}
			
			conn.setConnectTimeout(aTimeOutMs);
			
			try {
				
		    	StringBuffer sb = new StringBuffer();
				try {
					BufferedReader reader = null;
					try {
						in = conn.getInputStream();
						reader = new BufferedReader(new InputStreamReader(in));
						String sLine = null;
						while((sLine = reader.readLine())!=null)
						{
							sb.append(sLine);
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
    
    public static boolean isSuccess(HttpResp aHttpResp)
    {
    	if(aHttpResp==null)
    		return false;
    	
    	int iHttpStatus = aHttpResp.getHttp_status();
    	return (iHttpStatus>=200) && (iHttpStatus<300);
    }
    
    public static boolean ping(String aEndpointURL, int aPingTimeOutMs) 
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
				aPingTimeOutMs = conn_timeout;
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
}