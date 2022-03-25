package hl.common.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToLongBiFunction;

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
	}
	public void setContent_data(String content_data) {
		this.content_data = content_data;
		
		if(content_data!=null && isStringContent())
		{
			if(this.content_type==null)
			{
				String sTrimData = content_data.trim().toLowerCase();
				if(sTrimData.startsWith("{") && sTrimData.endsWith("}")
					||sTrimData.startsWith("[") && sTrimData.endsWith("]"))
				setContent_type("application/json");
			}
			else
			{
				setContent_type("text/plain");
			}
		}
	}
	public String getContent_type() {
		return content_type;
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