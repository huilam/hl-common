package hl.common.http;

public class RestApiClientTest {

	
	public static void wjtest() throws Exception
    {
		String url1 = "http://203.127.252.55/image-privacy-tools/demokit/img/Demo01_FHD_foreground.jpg";
		String url2 = "http://203.127.252.29/image-privacy/image-privacy-tools/demokit/img/Demo01_FHD_foreground.jpg";
		
		RestApiClient client = new RestApiClient();
		System.out.println("Downloading image from "+url1+"....");
		HttpResp img1Resp = client.httpGet(url1);
		String img1Data= img1Resp.getContent_data();
		if (img1Data == null) {
			System.err.println("Image 1 Data is NULL");
			System.out.println(img1Resp);
		}else {
			System.out.println("Completed downloading image 1.");
		}
    }
	

    public static void test1() throws Exception
    {
    	
    	//RestApiUtil.setHttpProxy("http://proxy.nec.com.sg", "8080");
    	//RestApiUtil.setHttpsProxy("http://proxy.nec.com.sg", "8080");

    	HttpResp httpResp = RestApiUtil.httpGet(
    			"http://203.127.252.29/image-privacy/image-privacy-tools/demokit/img/Demo01_FHD_foreground.jpg");

		System.out.println(httpResp.getHttp_status());
		System.out.println(httpResp.getContent_type());
		System.out.println("getContent_data().length():"+httpResp.getContent_data().length());
		//System.out.println("getContent_bytes():"+httpResp.getContentInBytes());
    }
	
    public static void main(String args[]) throws Exception
    {
    	for(int i=0; i<10; i++)
    	{
    		System.out.println("Test #"+(i+1));
    		wjtest();
    		System.out.println();
    	}
    }
    
}