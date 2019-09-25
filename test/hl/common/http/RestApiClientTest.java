package hl.common.http;

public class RestApiClientTest {

	
    public static void main(String args[]) throws Exception
    {
    	
    	RestApiUtil.setHttpProxy("http://proxy.nec.com.sg", "8080");
    	RestApiUtil.setHttpsProxy("http://proxy.nec.com.sg", "8080");

    	HttpResp httpResp = RestApiUtil.httpGet("http://203.127.252.70/scc/data/DEFAULT/MATRIX_VMS/GMT_0/20190925/0748/CAMERA_09/1569397706473.jpg");

		System.out.println(httpResp.getHttp_status());
		System.out.println(httpResp.getContent_type());
		System.out.println("getContent_data():"+httpResp.getContent_data());
		System.out.println("getContent_bytes():"+httpResp.getContentInBytes());
    }
}