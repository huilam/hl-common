package hl.common;

import java.awt.image.BufferedImage;
import java.io.File;


public class ImgUtilToBytes {
	

	
	public static void main(String args[]) throws Exception
	{
		String sTestDir = new File(".").getAbsoluteFile()+"//test//";
		
		File fileOutput = new File(sTestDir+"/output");
		fileOutput.mkdirs();
		
		File filePng = new File(sTestDir+"//X-Men-1080x720.jpg_transparent.png");
		File fileJpg = new File(sTestDir+"//X-Men-1080x720.jpg_transparent.jpg");
		File[] files = new File[] {filePng, fileJpg};
		
		for(File f : files)
		{
			BufferedImage img = ImgUtil.loadImage(f.getAbsolutePath());
			
			if(img==null)
			{
				System.err.println("Failed to load "+f.getAbsolutePath());
				continue;
			}
			
			System.out.println("Loaded "+f.getAbsolutePath()+" - "+img.getWidth()+"x"+img.getHeight());
			
			ImgUtil.saveAsFile(img, new File(fileOutput.getAbsoluteFile()+"/"+f.getName()+".png"));
			ImgUtil.saveAsFile(img, new File(fileOutput.getAbsoluteFile()+"/"+f.getName()+".jpg"));
		}
	
	}
	
}
