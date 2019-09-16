package hl.common;

import java.awt.image.BufferedImage;
import java.io.File;


public class ImgUtilTest extends ImgUtil {
	
	
	public static void main(String args[]) throws Exception
	{
		
		File fileImg1080p = new File(new File(".").getAbsoluteFile()+"//test//X-Men-1920x1080.jpg");
		File fileImg720p = new File(new File(".").getAbsoluteFile()+"//test//X-Men-1080x720.jpg");

		
		File[] files = new File[] {fileImg1080p, fileImg720p};
		
		for(File f : files)
		{
			BufferedImage img =  ImgUtil.loadImage(f.getPath());
			if(img!=null)
			{
				byte[] byteChecksum = getChecksum(img);
				
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
