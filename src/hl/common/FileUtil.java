package hl.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileUtil {

	private static final int BUFFER_SIZE = 32768;
	
	public static File downloadWebImage(String aImageURL) throws IOException
	{
		File tempFile = File.createTempFile("temp", "");
		tempFile.delete();
		return downloadWebImage(aImageURL, tempFile.getPath());
	}
		
	public static File downloadWebImage(String aImageURL, String aLocalTargetFileName) throws IOException
	{
		URL url = new URL(aImageURL);
		InputStream in = null;
		try{
			
			HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
			httpConn.setRequestMethod("GET");
			
			if(httpConn.getResponseCode()==200)
			{
				inputStreamToFile(httpConn.getInputStream(), aLocalTargetFileName);
			}
			else
			{
				throw new IOException("[download failed] "+aImageURL);
			}
		}
		finally
		{
			if(in!=null)
				in.close();
		}
		File fileImage = new File(aLocalTargetFileName);
		
		if(fileImage.isFile())
			return fileImage;
		else
			return null;
	}
	
    public static void inputStreamToFile(
    		InputStream aInputStream, String aOutputFileName) throws IOException {
    	
        BufferedOutputStream bos = null;
        try{
	        bos = new BufferedOutputStream(new FileOutputStream(aOutputFileName));
	        byte[] bytesIn = new byte[BUFFER_SIZE];
	        int read = 0;
	        while ((read = aInputStream.read(bytesIn)) != -1) {
	            bos.write(bytesIn, 0, read);
	        }
        }finally
        {
	        if(bos!=null)
	        	bos.close();
        }
    }
    
    public static void main(String args[]) throws IOException
    {
    	System.setProperty("https.proxyHost", "proxy.nec.com.sg");
    	System.setProperty("https.proxyPort", "8080");
    	FileUtil.downloadWebImage("https://scontent.xx.fbcdn.net/v/t1.0-0/p180x540/12670721_124148334651352_975683798861057932_n.jpg?oh=a1d7b5a0992976080c611b40a0684374&oe=58972E23");
    }
}
