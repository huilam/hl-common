package hl.common.http;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
	
	private static final String BOUNDARY_STR = "[ ¯\\_(ツ)_/¯ ]";
	
	private String url	= null;
	private Map<String, String> mapAttrs 	= new HashMap<String, String>();
	private Map<String, File> mapFiles 		= new HashMap<String, File>();
	
	private String basic_auth_uid 	= null;
	private String basic_auth_pwd 	= null;
	
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
		OutputStream outConn 		= null;
		BufferedWriter wrt	 		= null;
		FileInputStream inputFile	= null;
		
		try {
			url = new URL(this.url);
			urlConn = (HttpURLConnection) url.openConnection();			
			urlConn.setDoOutput(true);
			if(this.basic_auth_uid!=null && this.basic_auth_pwd!=null)
			{
				urlConn = setBasicAuthHeader(urlConn, this.basic_auth_uid, this.basic_auth_pwd);
			}
			urlConn.setRequestMethod("POST");
			urlConn.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY_STR);
			
System.out.println("Finish preparing header ...");
			StringBuffer sb = new StringBuffer();
			
			sb.append("\n");
			
			for(String sAttrName : mapAttrs.keySet())
			{
				sb.append("\n--").append(BOUNDARY_STR).append("\n");
				sb.append("Content-Disposition: form-data; ");
				sb.append("name=\"").append(sAttrName).append("\"\n\n");
				sb.append(mapAttrs.get(sAttrName));
			}
			
System.out.println("getting outputstream ...");			
			outConn = urlConn.getOutputStream();
			wrt = new BufferedWriter(new OutputStreamWriter(outConn));
			
System.out.println("Writing attributes ...");
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
				
System.out.println("Writing ["+f.getName()+"] ...");

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
			wrt.close();
			outConn.flush();
			outConn.close();
			//
System.out.println("Completed sending data.");

			int iRespCode 	= urlConn.getResponseCode();
			String sRespMsg = urlConn.getResponseMessage();
			
			resp.setHttp_status(iRespCode);
			resp.setHttp_status_message(sRespMsg);
			
			if(urlConn.getContent()!=null)
			{
				resp.setContent_type(urlConn.getContentType());
				resp.setContent_data(String.valueOf(urlConn.getContent()));
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
			resp.setHttp_status(400);
			resp.setContent_type("application/json");
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
        String url = "https://203.127.252.43:21439/persons/7/pictures";
        
        //String img_path = "C:\\Xinlai\\Installation\\TestFiles\\NeoCenter POI Thumbnail\\Christopher LAM.jpg";
        String img_path = new File(".").getCanonicalPath()+"\nls-chris-lam.jpg";
        File img_file = new File(img_path);

        HTMLMultiPart multipart = new HTMLMultiPart(url);
        multipart.setBasicAuth("rootuser", "rootuser");
		multipart.addFile("image", img_file);
		HttpResp resp = multipart.post();
		
		System.out.println(resp);		
	}
    
}