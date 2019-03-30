package hl.common.http;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class HTMLMultiPart {
	
	private static final String BOUNDARY_STR = "[-boudary-separator-]";
	
	private String url	= null;
	private Map<String, String> mapAttrs 	= new HashMap<String, String>();
	private Map<String, File> mapFiles 		= new HashMap<String, File>();
	
	public HTMLMultiPart(String aUrl)
	{
		url = aUrl;
	}
	
	public void addFile(String aFileName, File aFile)
	{
		mapFiles.put(aFileName, aFile);
	}
	
	public void addAttribute(String aAttrName, String aAttrValue)
	{
		mapAttrs.put(aAttrName, aAttrValue);
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
		OutputStream outConn 		= null;
		BufferedWriter wrt	 		= null;
		FileInputStream inputFile	= null;
		
		try {
			url = new URL(this.url);
			urlConn = (HttpURLConnection) url.openConnection();			
			urlConn.setDoOutput(true);
			urlConn.setRequestMethod("POST");
			urlConn.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY_STR);
			 
			StringBuffer sb = new StringBuffer();
			
			sb.append("\n");
			
			for(String sAttrName : mapAttrs.keySet())
			{
				sb.append("\n--").append(BOUNDARY_STR).append("\n");
				sb.append("Content-Disposition: form-data; ");
				sb.append("name=\"").append(sAttrName).append("\"\n\n");
				sb.append(mapAttrs.get(sAttrName));
			}
			
			outConn = urlConn.getOutputStream();
			wrt = new BufferedWriter(new OutputStreamWriter(outConn));
			wrt.write(sb.toString());
			
			int bytesRead;
			byte[] dataBuffer = new byte[4096];
			
			for(String sFileName : mapFiles.keySet())
			{
				File f = mapFiles.get(sFileName);
				//
				sb.setLength(0);
				sb.append("\n--").append(BOUNDARY_STR).append("\n");
				sb.append("Content-Disposition: form-data; ");
				sb.append("name=\"").append("file").append("\"; ");
				sb.append("filename=\"").append(f.getName()).append("\"");
				sb.append("\nContent-Type: text/plain\n\n");
				wrt.write(sb.toString());
				//
				dataBuffer = new byte[4096];
				inputFile = new FileInputStream(f);			 
				while((bytesRead = inputFile.read(dataBuffer)) != -1) {
					outConn.write(dataBuffer, 0, bytesRead);
				}
				outConn.flush();
			}			
			//
			wrt.write("\n--" + BOUNDARY_STR + "--\n");
			wrt.flush();
			//
			
			int iRespCode 	= urlConn.getResponseCode();
			String sRespMsg = urlConn.getResponseMessage();
			
			resp.setHttp_status(iRespCode);
			resp.setHttp_status_message(sRespMsg);
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
			resp.setHttp_status(400);
			resp.setContent_data(jArr.toString());
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
			
			if(outConn!=null)
			{
				try {
					outConn.flush();
					outConn.close();
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
		}
		
		return resp;
	}
	
	
	public static void main(String[] args)
	{
	}
    
}