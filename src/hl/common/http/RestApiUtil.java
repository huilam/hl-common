package hl.common.http;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

public class RestApiUtil {
	
	private static RestApiClient apiClient = new RestApiClient();
	
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
    
}