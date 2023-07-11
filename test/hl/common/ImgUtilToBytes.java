package hl.common;

import java.awt.image.BufferedImage;
import java.io.File;


public class ImgUtilToBytes {
	
	public static void main(String args[]) throws Exception
	{
		String sTestDir = new File(".").getAbsoluteFile()+"//test//";
		
		File fileOutput = new File(sTestDir+"/output");
		fileOutput.mkdirs();
		
		File filePng = new File(sTestDir+"///tv-channel-test.png");
		File fileJpg = new File(sTestDir+"//X-Men-1080x720.jpg");
		File fileBmp = new File(sTestDir+"//tv-channel-test.bmp");
		
		File[] files = new File[] {filePng, fileJpg, fileBmp};
		
		for(File f : files)
		{
			BufferedImage img = ImgUtil.loadImage(f.getAbsolutePath());
			
			if(img==null)
			{
				System.err.println("Failed to load "+f.getAbsolutePath());
				continue;
			}
			
			System.out.println("Loaded "+f.getAbsolutePath()+" - "+img.getWidth()+"x"+img.getHeight());

			byte[] byteJPG = ImgUtil.toBytes(img, "JPG");
			byte[] bytePNG = ImgUtil.toBytes(img, "PNG");
			byte[] byteBMP = ImgUtil.toBytes(img, "BMP");
			
			System.out.println("byteJPG="+ImgUtil.getImageTypeByBytes(byteJPG));
			System.out.println("bytePNG="+ImgUtil.getImageTypeByBytes(bytePNG));
			System.out.println("byteBMP="+ImgUtil.getImageTypeByBytes(byteBMP));
			
			String sBase64JPG = ImgUtil.imageToBase64(img, "JPG");
			String sBase64PNG = ImgUtil.imageToBase64(img, "PNG");
			String sBase64BMP = ImgUtil.imageToBase64(img, "BMP");
			
			System.out.println("base64JPG="+ImgUtil.getImageTypeByBase64(sBase64JPG));
			System.out.println("base64PNG="+ImgUtil.getImageTypeByBase64(sBase64PNG));
			System.out.println("base64BMP="+ImgUtil.getImageTypeByBase64(sBase64BMP));
			
			
//			ImgUtil.saveAsFile(img, new File(fileOutput.getAbsoluteFile()+"/"+f.getName()+".png"));
//			ImgUtil.saveAsFile(img, new File(fileOutput.getAbsoluteFile()+"/"+f.getName()+".jpg"));
		}
	
	}
	
}
