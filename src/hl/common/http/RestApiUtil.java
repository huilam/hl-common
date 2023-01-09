package hl.common.http;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import hl.common.FileUtil;

public class RestApiUtil {
	
	protected final static String TYPE_APP_JSON 	= "application/json"; 
	protected final static String TYPE_PLAINTEXT 	= "text/plain"; 
	protected final static String TYPE_OCTET_STREAM = "octet-stream";
	
	public final static String PROXY_HOST 		= "proxyHost";
	public final static String PROXY_PORT 		= "proxyPort";
	public final static String HTTP 			= "http";
	public final static String HTTPwithSSL 		= "https";
	
	public static String[] defaultStaticHtml 	= new String[] {"index.html", "index.htm"};
	
	private static RestApiClient apiClient = new RestApiClient();
	
	public static void setHttpProxy(String aUrl, String aPort)
	{
		Properties p = System.getProperties();
		p.setProperty(HTTP+"."+PROXY_HOST, aUrl);
		p.setProperty(HTTP+"."+PROXY_PORT, aPort);
		if(p.getProperty(HTTPwithSSL+"."+PROXY_HOST)==null && aUrl!=null)
		{
			setHttpsProxy(aUrl, aPort);
		}
	}
	
	public static void setHttpsProxy(String aUrl, String aPort)
	{
		Properties p = System.getProperties();
		p.setProperty(HTTPwithSSL+"."+PROXY_HOST, aUrl);
		p.setProperty(HTTPwithSSL+"."+PROXY_PORT, aPort);
		if(p.getProperty(HTTP+"."+PROXY_HOST)==null && aUrl!=null)
		{
			setHttpProxy(aUrl, aPort);
		}
	}
	public static void setConnTimeout(int aTimeOutMs)
	{
		apiClient.setConnTimeout(aTimeOutMs);
	}
	
	public static String getReqContent(HttpServletRequest req)
	{
		return apiClient.getReqContent(req);
	}

	public static void processHttpResp(HttpServletResponse res, 
    		int aHttpStatus, JSONObject aJsonContent) throws IOException
	{
		processHttpResp(res, aHttpStatus, RestApiClient.TYPE_APP_JSON, aJsonContent.toString());
	}
	
    public static void processHttpResp(HttpServletResponse res, 
    		int aHttpStatus, String aContentType, String aOutputContent) throws IOException
    {
    	processHttpResp(res, aHttpStatus, aContentType, aOutputContent, 
    			RestApiClient.DEFAULT_GZIP_THRESHOLD_BYTES);
    }
    
    public static void processHttpResp(HttpServletResponse res, 
    		int aHttpStatus, String aContentType, String aOutputContent, long aGzipThresholdBytes) throws IOException
    {
    	apiClient.processHttpResp(res, aHttpStatus, aContentType, aOutputContent, aGzipThresholdBytes);
    }
    	
    public static void processHttpResp(HttpServletResponse res, HttpResp aHttpReq, long aGzipThresholdBytes) throws IOException
    {
    	apiClient.processHttpResp(res, aHttpReq, aGzipThresholdBytes);
    }
    
    public static HttpResp httpSubmit(String aHttpMethod, String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	String sHttpMethod = aHttpMethod;
    	
    	if(sHttpMethod!=null)
    		sHttpMethod = sHttpMethod.toUpperCase();
    	
    	return apiClient.httpSubmit(sHttpMethod, aEndpointURL, aContentType, aContentData);
    }
    
    public static HttpResp httpDelete(String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	return apiClient.httpDelete(aEndpointURL, aContentType, aContentData);
    }
        
    public static HttpResp httpPost(String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	return apiClient.httpPost(aEndpointURL, aContentType, aContentData);
    }
    
    public static HttpResp httpPut(String aEndpointURL, 
    		String aContentType, String aContentData) throws IOException
    {
    	return apiClient.httpPut(aEndpointURL, aContentType, aContentData);
    }
    
    public static HttpResp httpGet(String aEndpointURL, int aTimeOutMs) throws IOException
    {
    	apiClient.setConnTimeout(aTimeOutMs);
    	return apiClient.httpGet(aEndpointURL);
    }
    
    public static HttpResp httpGet(String aEndpointURL) throws IOException
    {
    	return apiClient.httpGet(aEndpointURL);
    }
    
    public static boolean ping(String aEndpointURL, int aPingTimeOutMs) 
    {
    	return apiClient.ping(aEndpointURL, aPingTimeOutMs);
    }

    public static File getWebContentAsFile(HttpServletRequest req)
    {
    	return getWebContentAsFile(req, defaultStaticHtml);
    }
    
    public static File getWebContentAsFile(HttpServletRequest req, String[] defaultHtmlFile)
    {
    	if(req==null || req.getPathTranslated()==null)
    		return null;
    	
    	File file = new File(req.getPathTranslated());
    	
    	if(file!=null && file.isDirectory() && defaultHtmlFile!=null)
    	{
    		String sPath = req.getPathTranslated();
    		
    		for(String sStaticFile : defaultHtmlFile)
    		{
    			File fileHTML = new File(sPath+"/"+sStaticFile);
        		if(fileHTML.isFile())
        		{
        			file = fileHTML;
        			break;
        		}
    		}
    	}
    	
		return (file.isFile()? file : null);
    }
    
    public static boolean serveStaticWeb(HttpServletRequest req, HttpServletResponse res)
    {
    	File file = getWebContentAsFile(req);

    	if(res==null || file==null)
    		return false;
    	
    	
    	String sReqPath 	= req.getPathTranslated();
    	String sStaticPath 	= file.getAbsolutePath();
    	if(!sStaticPath.endsWith(sReqPath))
    	{
    		int iPos = sStaticPath.indexOf(sReqPath);
    		if(iPos>-1)
    		{
	    		String sDefaultPath = sStaticPath.substring(iPos+sReqPath.length());
	    		//auto default
	    		try {
					res.sendRedirect(req.getContextPath()+req.getPathInfo()+sDefaultPath);
					return true;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    	
    	
    	boolean isServed = false;
    	
		byte[] byteFile = null;
		try {
			
			byteFile = FileUtil.getBytes(file);
			
			if(byteFile!=null)
			{
				HttpResp httpResp = new HttpResp();
				httpResp.setHttp_status(HttpServletResponse.SC_OK);
				
				String sMimeType = Files.probeContentType(file.toPath());
				
				if(sMimeType==null)
					sMimeType = "";
					
				boolean isText = sMimeType.startsWith("text");
				if(isText)
				{
					httpResp.setContent_data(new String(byteFile));
				}
				else
				{
					httpResp.setContent_bytes(byteFile);
				}
				
				if(sMimeType==null || sMimeType.trim().length()==0)
				{
					sMimeType = isText ? TYPE_PLAINTEXT: TYPE_OCTET_STREAM;
				}

				httpResp.setContent_type(sMimeType);
				
				RestApiUtil.processHttpResp(res,httpResp, -1);
				isServed = true;
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isServed;
    }
}