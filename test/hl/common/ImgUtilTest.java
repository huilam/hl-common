package hl.common;

import java.awt.image.BufferedImage;
import java.io.File;


public class ImgUtilTest {
	
	public static void main(String args[]) throws Exception
	{
		String sTestDir = new File(".").getAbsoluteFile()+"//test//";
		
		File fileImg1080p = new File(sTestDir+"//X-Men-1920x1080.jpg");
		File fileImg720p = new File(sTestDir+"//X-Men-1080x720.jpg");

		
		System.out.println();
		
		
		File[] files = new File[] {fileImg1080p, fileImg720p};
		
		for(File f : files)
		{
			BufferedImage img =  ImgUtil.loadImage(f.getPath());
			
			System.out.println("img="+img);
			
			String sImgAscii = ImgUtil.img2Ascii(img, 192, 108);
			System.out.println(sImgAscii);
						
			BufferedImage imgTransparent = ImgUtil.adjOpacity(img, 0.20f);
			ImgUtil.saveAsFile(imgTransparent, new File(sTestDir+"//"+f.getName()+"_transparent.jpg"));
			ImgUtil.saveAsFile(imgTransparent, new File(sTestDir+"//"+f.getName()+"_transparent.png"));
			
		}
		
		
	}
	
}
