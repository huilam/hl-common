package hl.common;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class ImgUtilTest extends ImgUtil {
	
	
	private static int byte2int(byte aByte)
	{
		return aByte & 0xff;
	}
	
	protected static String getEncodedData(
			BufferedImage aBufferedImage,
			String aSignature) throws IOException, NoSuchAlgorithmException
	{
		String sEncodedData = null;
		
		if(aBufferedImage!=null)
		{
			byte[] byteSignature = CryptoUtil.getMD5Checksum(aSignature.getBytes());
			byte[] byteData = new byte[byteSignature.length];
			int y1 = 0;
			int y2 = aBufferedImage.getHeight()-1;
			
			for(int i=0; i<byteSignature.length/2; i+=2)
			{
				Color c = new Color(aBufferedImage.getRGB(i, y1));
	        	int v1 = byte2int(byteSignature[i]);
	        	int v2 = byte2int(byteSignature[i+1]);
	        	
	        	if(v1!=c.getRed() || v2!=c.getBlue())
	        	{
	        		System.out.println("break!");
	        		System.out.println("v1="+v1+":"+c.getRed());
	        		System.out.println("v2="+v2+":"+c.getBlue());
	        		return null;
	        	}
	        	
	        	c = new Color(aBufferedImage.getRGB(i, y2));
	        	
	        	byteData[i] = (byte) c.getRed();
	        	byteData[i+1] = (byte) c.getBlue();
	        	
			}
			
			sEncodedData = new String(byteData);
			
		}
		return sEncodedData; 
	}
	
	protected static BufferedImage encode(
			BufferedImage aBufferedImage,
			String aSignature,
			String aData) throws IOException, NoSuchAlgorithmException
	{
		if(aBufferedImage!=null)
		{
			byte[] byteSignature = CryptoUtil.getMD5Checksum(aSignature.getBytes());
			byte[] byteData = CryptoUtil.getMD5Checksum(aData.getBytes());
			
			int y1 = 0;
			int y2 = aBufferedImage.getHeight()-1;
			
			for(int i=0; i<byteSignature.length/2; i+=2)
			{
				Color c = new Color(aBufferedImage.getRGB(i, y1));
	        	
	        	int v1 = byte2int(byteSignature[i]);
	        	int v2 = byte2int(byteSignature[i+1]);
	        	
	        	System.out.println("byteSignature.v1="+v1);
	        	System.out.println("byteSignature.v2="+v2);
	        	
	        	Color c2 = new Color(v1, c.getGreen(), v2);
				aBufferedImage.setRGB(i, y1, c2.getRGB());
				
				////////////////////////////////////
				
				c = new Color(aBufferedImage.getRGB(i, y2));
	        	
	        	v1 = byte2int(byteData[i]);
	        	v2 = byte2int(byteData[i+1]);
	        	//
	        	System.out.println("byteData.v1="+v1);
	        	System.out.println("byteData.v2="+v2);
	        	
	        	c2 = new Color(v1, c.getGreen(), v2);
	        	aBufferedImage.setRGB(i, y2, c2.getRGB());
			}
			
			System.out.println("byteSignature.length="+byteSignature.length);
			System.out.println("byteData.length="+byteData.length);
		}
		return aBufferedImage;
	}
	
	public static void main(String args[]) throws Exception
	{
		
		File fileImg1080p = new File(new File(".").getAbsoluteFile()+"//test//X-Men-1920x1080.jpg");
		File fileImg720p = new File(new File(".").getAbsoluteFile()+"//test//X-Men-1080x720.jpg");

		
		System.out.println();
		
		
		File[] files = new File[] {fileImg1080p, fileImg720p};
		
		for(File f : files)
		{
			BufferedImage img =  ImgUtil.loadImage(f.getPath());
			
			System.out.println("img="+img);
			
			byte[] byteChecksum = ImgUtil.getChecksum(img);
			String sMD5 = CryptoUtil.toHexString(byteChecksum);
			System.out.println("sMD5="+sMD5);
			BufferedImage img2 = encode(img, "-nls-matrix-", sMD5);
			String sData = getEncodedData(img2, "-nls-matrix-");
			
			System.out.println("sData="+sData);
			
			
			
			if(img!=null)
			{
				
				applyWatermark(img);
				
				byteChecksum = getChecksum(img);
				System.out.println();
				for(byte b : byteChecksum)
				{
					int i = (b & 0xff);
					System.out.print("["+i+"]");
				}
				System.out.println();
				
				
					BufferedImage imgPixelized = pixelize(img);
					File fileOutput = new File(f.getParent()+"//pixelized_"+f.getName());
					saveAsFile(imgPixelized, fileOutput);
			
			}
			else
			{
				System.err.println("img == null");
			}
		}
		
		
	}
	
}
