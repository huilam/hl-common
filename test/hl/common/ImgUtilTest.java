package hl.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import hl.common.http.RestApiUtil;


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
				
				img = pixelize(img);
				File fileOutput = new File(f.getParent()+"//pixelized_"+f.getName());
				saveAsFile(img, fileOutput);
				
			}
			else
			{
				System.err.println("img == null");
			}
		}
		
		
	}
	
}
