package hl.common.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class HttpResp {
	//
	private int http_status 			= 0;
	private String http_status_message 	= null;
	//
	private Object content_data 		= null;
	private String content_type 		= null;
	//
	private String request_url 			= null;
	private String server_ip 			= null;
	//
	public final String TYPE_IMAGE_JPG 	= "image/jpeg";
	public final String TYPE_IMAGE_GIF 	= "image/gif";
	public final String TYPE_IMAGE_PNG 	= "image/png";
	public final String TYPE_IMAGE_BMP 	= "image/bmp";
	
	public final String TYPE_VIDEO_MP4 	= "video/mp4";
	public final String TYPE_AUDIO_MP3 	= "audio/mpeg";
	
	public final String TYPE_APPL_JSON 	= "application/json";
	public final String TYPE_APPL_PDF 	= "application/pdf";
	public final String TYPE_TEXT_HTML 	= "text/html";
	//-- default
	public final String TYPE_PLAINTEXT 	= "text/plain";
	public final String TYPE_BINARY 	= "application/octet-stream";
	//
	private Map<String, String> mapErrors = new HashMap<String, String>();
	
	public boolean isSuccess()
	{
		int iStatus = getHttp_status();
		return (iStatus>=200 && iStatus<300);
	}
	
	public int getHttp_status() {
		return http_status;
	}
	public void setHttp_status(int http_status) {
		this.http_status = http_status;
	}
	public String getHttp_status_message() {
		return http_status_message;
	}
	public void setHttp_status_message(String http_status_message) {
		this.http_status_message = http_status_message;
	}
	
	public boolean isStringContent() {
		return(this.content_data instanceof String);
	}
	
	public boolean isBytesContent() {
		return(this.content_data instanceof byte[]);
	}

	public String getContent_data() {
		if(isStringContent())
		{
			return (String) this.content_data;
		}
		else if(isBytesContent())
		{
			return new String((byte[])this.content_data);
		}
		else
			return null;
	}
	
	public byte[] getContentInBytes() 
	{
		if(isBytesContent())
		{
			return (byte[]) this.content_data;
		}
		else if(isStringContent())
		{
			return ((String)this.content_data).getBytes();
		}
		else {
			return null;
		}
	}
	public void setContent_bytes(byte[] content_data) {
		this.content_data = content_data;
		setContent_type_as_BinaryStream();
	}
	
	public void setContent_data(JSONObject content_data) {
		this.content_data = content_data.toString();
		setContent_type_as_Json();
	}
	
	public void setContent_data(String content_data) {
		
		this.content_data = content_data;
		
		if(this.content_data!=null && this.content_type==null)
		{
			if(isStringContent())
			{
				String sTrimData = content_data.trim().toLowerCase();
				
				boolean isJsonObj 	= sTrimData.startsWith("{") && sTrimData.endsWith("}");
				boolean isJsonArray = sTrimData.startsWith("[") && sTrimData.endsWith("]");
				if(isJsonObj || isJsonArray)
				{
					setContent_type_as_Json();
				}
			}
		}
	}
	public String getContent_type() {
		return content_type;
	}
	
	public void setContent_type_as_Json() {
		setContent_type(TYPE_APPL_JSON);
	}
	
	public void setContent_type_as_BinaryStream() {
		setContent_type(TYPE_BINARY);
	}
	
	public void setContent_type_as_ImageJPG() {
		setContent_type(TYPE_IMAGE_JPG);
	}
	
	public void setContent_type_as_ImagePNG() {
		setContent_type(TYPE_IMAGE_PNG);
	}
	
	public void setContent_type_as_PDF() {
		setContent_type(TYPE_APPL_PDF);
	}
	
	public void setContent_type_as_Plaintext() {
		setContent_type(TYPE_PLAINTEXT);
	}
	
	public void setContent_type_as_VIDEO_MP4() {
		setContent_type(TYPE_VIDEO_MP4);
	}
	
	public void setContent_type_as_AUDIO_MP3() {
		setContent_type(TYPE_AUDIO_MP3);
	}
	
	public void setContent_type(String content_type) {
		this.content_type = content_type;
	}	
	
	public String getRequest_url() {
		return request_url;
	}

	public void setRequest_url(String request_url) {
		this.request_url = request_url;
	}

	public String getServer_ip() {
		return server_ip;
	}

	public void setServer_ip(String server) {
		
		InetAddress address;
		try {
			address = InetAddress.getByName(server);
			server 	= address.getHostAddress();
		} catch (UnknownHostException e) {
			//ignore
		}
		
		this.server_ip = server;
	}

	public boolean hasErrors()
	{
		return this.mapErrors.size()>0;
	}
	
	public Map<String, String> getErrorMap()
	{
		return this.mapErrors;
	}
	
	public void clearErrorMap()
	{
		this.mapErrors.clear();
	}

	public void addToErrorMap(String aErrorID, String aErrorMessage)
	{
		this.mapErrors.put(aErrorID, aErrorMessage);
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("\n").append("server-ip:").append(getServer_ip());
		sb.append("\n").append("request-url:").append(getRequest_url());
		sb.append("\nstatus:").append(getHttp_status()).append(" ").append(getHttp_status_message());
		sb.append("\n").append("content-type:").append(getContent_type());
		sb.append("\n").append("body:").append(this.content_data!=null?this.content_data.getClass().getSimpleName():"").append(this.content_data);
		sb.append("\n").append("hasError:").append(hasErrors());
		if(hasErrors())
		{
			for(String sErrKey : getErrorMap().keySet())
			{
				sb.append("\n").append("   - ").append(sErrKey).append("=").append(getErrorMap().get(sErrKey));
			}
			
		}
		return sb.toString();
		
	}
}