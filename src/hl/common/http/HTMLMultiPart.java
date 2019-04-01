package hl.common.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class HTMLMultiPart {
	
	private final static String LINE_FEED 		= "\r\n";
	private final static String BOUNDARY_STR 	= "[ \\_(. .)_// ]";
	public final static String TYPE_APP_JSON 	= "application/json";
	
	private String url	= null;
	private Map<String, String> mapAttrs 	= new HashMap<String, String>();
	private Map<String, File> mapFiles 		= new HashMap<String, File>();
	
	private String basic_auth_uid 	= null;
	private String basic_auth_pwd 	= null;
	
	public HTMLMultiPart(String aUrl)
	{
		url = aUrl;
	}
	
	public void addFile(String aAttrName, File aFile)
	{
		mapFiles.put(aAttrName, aFile);
	}
	
	public void addAttribute(String aAttrName, String aAttrValue)
	{
		mapAttrs.put(aAttrName, aAttrValue);
	}
	
	public void setBasicAuth(String aUid, String aPwd)
	{
		this.basic_auth_uid = aUid;
		this.basic_auth_pwd = aPwd;
	}
	
	public void clear()
	{
		mapFiles.clear();
		mapAttrs.clear();
	}
	
	public HttpResp post()
	{
		HttpResp resp = new HttpResp();
		
		URL url 					= null;
		HttpURLConnection urlConn 	= null;
		OutputStream outstreamConn 	= null;
		BufferedWriter wrt	 		= null;
		FileInputStream inputFile	= null;
		
		try {
			url = new URL(this.url);
			urlConn = (HttpURLConnection) url.openConnection();	
			urlConn.setUseCaches(false);
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			if(this.basic_auth_uid!=null && this.basic_auth_pwd!=null)
			{
				urlConn = setBasicAuthHeader(urlConn, this.basic_auth_uid, this.basic_auth_pwd);
			}
			urlConn.setRequestMethod("POST");
			urlConn.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY_STR);
System.out.println("Finish preparing header ...");
			
System.out.println("Getting outputstream ...");			
			outstreamConn = urlConn.getOutputStream();
			wrt = new BufferedWriter(new OutputStreamWriter(outstreamConn));
			
			StringBuffer sb = new StringBuffer();
			sb.append(LINE_FEED);
			for(String sAttrName : mapAttrs.keySet())
			{
				sb.append(LINE_FEED).append("--").append(BOUNDARY_STR).append(LINE_FEED);
				sb.append("Content-Disposition: form-data; ");
				sb.append("name=\"").append(sAttrName).append("\"").append(LINE_FEED).append(LINE_FEED);
				sb.append(mapAttrs.get(sAttrName));
			}
			
System.out.println("Writing attributes ...");
			wrt.write(sb.toString());
			
			int bytesRead;
			byte[] dataBuffer = new byte[4096];
			
			for(String sAttrName : mapFiles.keySet())
			{
				File f = mapFiles.get(sAttrName);
				//
				sb.setLength(0);
				sb.append(LINE_FEED).append("--").append(BOUNDARY_STR).append(LINE_FEED);
				sb.append("Content-Disposition: form-data;");
				sb.append("name=\"").append(sAttrName).append("\"; ");
				sb.append("filename=\"").append(f.getName()).append("\"");
				sb.append(LINE_FEED).append("Content-Type: text/plain").append(LINE_FEED).append(LINE_FEED);
				
System.out.println("Writing '"+sAttrName+"' ["+f.getName()+"] ...");

				wrt.write(sb.toString());
				//
				dataBuffer = new byte[4096];
				try {
					inputFile = new FileInputStream(f);			 
					while((bytesRead = inputFile.read(dataBuffer)) != -1) {
						outstreamConn.write(dataBuffer, 0, bytesRead);
					}
					outstreamConn.flush();
				}finally
				{
					if(inputFile!=null)
						inputFile.close();
				}
				
			}			
			//
			wrt.write(LINE_FEED+"--" + BOUNDARY_STR + "--"+LINE_FEED);
			wrt.flush();
			wrt.close();
			outstreamConn.flush();
			outstreamConn.close();
			//
System.out.println("Completed sending data.");

			int iRespCode 	= urlConn.getResponseCode();
			String sRespMsg = urlConn.getResponseMessage();
			
			resp.setHttp_status(iRespCode);
			
			if(sRespMsg!=null)
			{
				resp.setHttp_status_message(sRespMsg);
			}
			
			if(resp.isSuccess())
			{
				resp = getUrlConnResp(resp, urlConn);
			}
		}
		catch(Exception ex)
		{
			String sErrMsg = ex.getMessage();
			if(ex.getCause()!=null)
			{
				sErrMsg = ex.getCause().getMessage();
			}
			//
			JSONObject jsonError = new JSONObject();
			jsonError.put(ex.getClass().getSimpleName(), sErrMsg);
			//
			JSONArray jArr = new JSONArray();
			jArr.put(jsonError);
			//
			try {
				resp = getUrlConnResp(resp, urlConn);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//
			
			if(resp.getHttp_status()==500)
			{
				resp.setHttp_status(400);
				resp.setContent_type(TYPE_APP_JSON);
				resp.setContent_data(jArr.toString());			
			}

		}
		finally
		{
			if(inputFile!=null)
			{
				try {
					inputFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(outstreamConn!=null)
			{
				try {
					outstreamConn.flush();
					outstreamConn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(wrt!=null)
			{
				try {
					wrt.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(urlConn!=null)
			{
				urlConn.disconnect();
			}
		}
		
		return resp;
	}
	
	
	public HttpResp getUrlConnResp(HttpResp aHttpResp, HttpURLConnection aURLConn) throws IOException
	{
		if(aURLConn==null)
			return null;
		
		BufferedReader reader = null;
		StringBuffer sb = new StringBuffer();
		try{
			reader = new BufferedReader(new InputStreamReader(aURLConn.getInputStream()));
			
            String line 	= null;
            while ((line = reader.readLine()) != null) {
            	if(sb.length()>0)
            	{
            		sb.append("\n");
            	}
            	sb.append(line);
            }
		}finally
		{
			if(reader!=null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
		if(aHttpResp==null)
			aHttpResp = new HttpResp();
			
		aHttpResp.setContent_type(aURLConn.getContentType());
		aHttpResp.setContent_data(sb.toString());

		return aHttpResp;
	}
	
	//===
    public static HttpURLConnection setBasicAuthHeader(HttpURLConnection aConn, String aUid, String aPwd)
    {
    	return setBasicAuthHeader(aConn, aUid+":"+aPwd);
    }
    
    private static HttpURLConnection setBasicAuthHeader(HttpURLConnection aConn, String aUserInfo)
    {
    	if(aConn==null)
    		return aConn;
    	
   		String sEncodedBasicAuth = "Basic " + new String(Base64.getEncoder().encode(aUserInfo.getBytes()));
		aConn.setRequestProperty ("Authorization", sEncodedBasicAuth);
    	
		return aConn;    	
    }
	//===
    
	public static void main(String[] args) throws IOException
	{
	}
    
}