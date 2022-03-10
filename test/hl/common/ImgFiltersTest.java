package hl.common;

import java.awt.image.BufferedImage;
import java.io.File;


public class ImgFiltersTest {
	
	public static void main(String args[]) throws Exception
	{
		String sTestDir = new File(".").getAbsoluteFile()+"//test";
		File fileTestOutputDir = new File("./test/output");
		fileTestOutputDir.mkdirs();
		
		File fileImg1080p = new File(sTestDir+"//X-Men-1920x1080.jpg");
		File fileImg720p = new File(sTestDir+"//X-Men-1080x720.jpg");

		
		System.out.println();
		
		
		File[] files = new File[] {fileImg1080p, fileImg720p};
		
		for(File f : files)
		{
			BufferedImage img =  ImgUtil.loadImage(f.getPath());
			
			System.out.println("img="+img);
			
			//BufferedImage imgAscii =  ImgUtil.resizeImg(img, 0, 0, true);
			int iW = img.getWidth() / 40;
			int iH = img.getHeight() / 40;
			String sImgAscii = ImgFilters.img2Ascii(img, iW, iH);
			System.out.println(sImgAscii);
			
			File fileOutputPath = new File(fileTestOutputDir.getAbsolutePath()+"//"+f.getName());
			fileOutputPath.mkdirs();
			String sOutputPrefixName = fileOutputPath.getAbsolutePath()+"//"+f.getName();
			
			BufferedImage imgTransparent = ImgUtil.adjOpacity(img, 0.50f);
			
			BufferedImage imgThermal = ImgFilters.toThermal(img);
			imgThermal = ImgUtil.adjOpacity(imgThermal, 0.50f);
			
			BufferedImage imgPixelate = ImgFilters.pixelize(img, 0.50f);
			
			for(String sExt : new String[] {"png", "jpg"})
			{
				ImgUtil.saveAsFile(imgTransparent, new File(sOutputPrefixName+"_transparent."+sExt));	
				ImgUtil.saveAsFile(imgThermal, new File(sOutputPrefixName+"_thermal."+sExt));
				ImgUtil.saveAsFile(imgPixelate, new File(sOutputPrefixName+"_pixelate."+sExt));
			}
		}
		
		
	}
	
}
