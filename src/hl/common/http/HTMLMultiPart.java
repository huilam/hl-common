package hl.common.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import hl.common.FileUtil;

public class HTMLMultiPart {
	
	private final static String LINE_FEED 		= "\r\n";
	private final static String BOUNDARY_STR 	= "|__(. .)__|";
	
	public final static String _PROTOCOL_HTTPS 	= "https";
	public final static String _PROTOCOL_SSL 	= "SSL";
	public final static String TYPE_APP_JSON 	= "application/json";
	
	private String url	= null;
	private Map<String, String> mapAttrs 			= new HashMap<String, String>();
	private Map<String, String> mapFiles 			= new HashMap<String, String>();
	private Map<String, byte[]> mapFileBytes 		= new HashMap<String, byte[]>();
	private Map<String, String> mapExtContentType 	= new HashMap<String, String>();
	
	private KeyStore keystore_sslcert 		= null;
	private String basic_auth_uid 			= null;
	private String basic_auth_pwd 			= null;
	
	public HTMLMultiPart(String aUrl)
	{
		this.url = aUrl;
		
		mapExtContentType.put("jpg", "image/jpeg");
		mapExtContentType.put("jpeg", "image/jpeg");
		mapExtContentType.put("bmp", "image/bmp");
		mapExtContentType.put("png", "image/png");
	}
	
	private void addFile(String aAttrName, File aFile) throws IOException
	{
		if(!aFile.isFile())
		{
			throw new IOException("Invalid File - "+aFile.getCanonicalPath());
		}
		
		byte[] byteFile = FileUtil.getBytes(aFile);
		addFileBytes(aAttrName, aFile.getName(), byteFile);
	}
	
	public void addFileBytes(String aAttrName, String aFileName, byte[] aBytes) throws IOException
	{
		if(aBytes==null)
		{
			throw new IOException("File aBytes is null !");
		}
		mapFiles.put(aAttrName, aFileName);
		mapFileBytes.put(aFileName, aBytes);
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
	
	public void setSSLKeystore(KeyStore aKeyStore)
	{
		this.keystore_sslcert = aKeyStore;
	}
	
	public void clear()
	{
		mapFileBytes.clear();
		mapAttrs.clear();
	}
	
	public HttpResp post()
	{
		HttpResp resp = new HttpResp();
		
		URL url 					= null;
		HttpURLConnection urlConn 	= null;
		OutputStream outstreamConn 	= null;
		BufferedWriter wrt	 		= null;
		ByteArrayInputStream inputBytes	= null;
		
		try {
			url = new URL(this.url);
			urlConn = (HttpURLConnection) url.openConnection();	
			urlConn.setUseCaches(false);
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			
			if(url.getProtocol().equalsIgnoreCase(_PROTOCOL_HTTPS))
			{
				HttpsURLConnection httpsconn = (HttpsURLConnection)urlConn;
				httpsconn = (HttpsURLConnection)urlConn;
				SSLSocketFactory sslFactory = RestApiClient.getTrustAnyHostSSLSocketFactory();
				if(this.keystore_sslcert!=null)
				{
					sslFactory = RestApiClient.getTrustCustomKeystoreSSLSocketFactory(this.keystore_sslcert);
				}
				httpsconn.setSSLSocketFactory(sslFactory);
				urlConn = httpsconn;
			}
			
			if(this.basic_auth_uid!=null && this.basic_auth_pwd!=null)
			{
				urlConn = RestApiClient.setBasicAuthHeader(urlConn, this.basic_auth_uid, this.basic_auth_pwd);
			}
			urlConn.setRequestMethod("POST");
			urlConn.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY_STR);
			urlConn.connect();
			
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
			
			wrt.write(sb.toString());
			wrt.flush();
			
			int bytesRead;
			byte[] dataBuffer = new byte[4096];
			
			for(String sAttrName : mapFiles.keySet())
			{
				String sFileName 	= mapFiles.get(sAttrName);
				byte[] byteFile 	= mapFileBytes.get(sFileName);
				String sContentType = "text/plain";
				
				if(sFileName==null)
					sFileName = sAttrName+"_"+byteFile.length;
				
				int iPos = sFileName.lastIndexOf(".");
				if(iPos>-1)
				{
					String sFileExt = sFileName.substring(iPos+1).toLowerCase();
					String fileContentType = mapExtContentType.get(sFileExt);
					if(fileContentType!=null)
					{
						sContentType = fileContentType;
					}
				}
				
				//
				sb.setLength(0);
				sb.append(LINE_FEED).append("--").append(BOUNDARY_STR).append(LINE_FEED);
				sb.append("Content-Disposition: form-data; ");
				sb.append("name=\"").append(sAttrName).append("\"; ");
				sb.append("filename=\"").append(sFileName).append("\"");
				sb.append(LINE_FEED).append("Content-Type:").append(sContentType).append(LINE_FEED).append(LINE_FEED);
				
				wrt.write(sb.toString());
				wrt.flush();
				//
				long lTotalRead = 0;
				dataBuffer = new byte[4096];
				try {
					inputBytes = new ByteArrayInputStream(byteFile);			 
					while((bytesRead = inputBytes.read(dataBuffer)) != -1) {
						outstreamConn.write(dataBuffer, 0, bytesRead);
						lTotalRead += bytesRead;
					}
					outstreamConn.flush();
				}finally
				{
					if(inputBytes!=null)
						inputBytes.close();
				}
			}			
			//
			wrt.write(LINE_FEED+"--" + BOUNDARY_STR + "--"+LINE_FEED);
			wrt.flush();
			outstreamConn.flush();

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
			if(inputBytes!=null)
			{
				try {
					inputBytes.close();
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
	

	public static void main(String[] args) throws IOException
	{	
		String sUrl = "http://127.0.0.1:8080/multipart/";
		File f = new File("c:\\New folder\\t e s t.jpg");
		String sFileBase64 = FileUtil.getBase64(f);
		
		HTMLMultiPart mp = new HTMLMultiPart(sUrl);
		mp.addFileBytes("picture", f.getName(), FileUtil.getBytesFromBase64(sFileBase64));
		mp.addAttribute("testfield", "testvalue");
		HttpResp httpRes = mp.post();
		System.out.println(httpRes);
	}
	
}