package hl.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Random;

public class CryptoUtil{
	
	private static Decoder Base64Decoder = Base64.getDecoder();
	private static Encoder Base64Encoder = Base64.getEncoder();
	
	static
	{
		try {
			MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String Base64Encode(String aString)
	{
		return Base64Encoder.encodeToString(aString.getBytes());
	}
	
	public static String Base64Decode(String aString)
	{
		return new String(Base64Decoder.decode(aString.getBytes()));
	}
	
	public static String obfuscate(String aString)
	{
		//
		if(aString==null || aString.trim().length()==0)
			return null;
		//
		Random r = new Random(26);
		StringBuffer sb = new StringBuffer();
		//
		MessageDigest MD5;
		try {
			MD5 = MessageDigest.getInstance("MD5");
			MD5.update(Base64Encoder.encode(aString.getBytes()));
			String sMessy = toHexString(MD5.digest());
			for(int i=0; i<aString.length(); i++)
			{
				if(i<sMessy.length())
				{
					sb.append(sMessy.charAt(i));
				}
				else
				{
					sb.append((char)('A'+r.nextInt()));
				}
				sb.append(aString.charAt(i));
			}
		} catch (NoSuchAlgorithmException e) {
			// won't happen as already try to init during static
		}
		//
		return new String(Base64Encoder.encode(sb.toString().getBytes()));
	}
	
	public static String deobfuscate(String aString)
	{
		if(aString==null || aString.trim().length()==0)
			return null;
		//
		String sDeobfuscated = new String(Base64Decoder.decode(aString.getBytes()));
		boolean isOdd = false;
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<sDeobfuscated.length(); i++)
		{
			if(isOdd)
			{
				sb.append(sDeobfuscated.charAt(i));
			}
			isOdd = !isOdd;
		}
		//
		return sb.toString();
	}
	
	private static String toHexString(byte[] bytes) {
	    StringBuilder hexString = new StringBuilder();

	    for (int i = 0; i < bytes.length; i++) {
	        String hex = Integer.toHexString(0xFF & bytes[i]);
	        if (hex.length() == 1) {
	            hexString.append('0');
	        }
	        hexString.append(hex);
	    }

	    return hexString.toString();
	}
	
	/*
	public static void main(String args[]) throws Exception
	{
		CryptoUtil crypt = new CryptoUtil();
		String sText = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		System.out.println("Text:"+sText);
		String sObfuscated = crypt.obfusicate(sText);
		System.out.println("Obfuscated:"+sObfuscated);
		String sDeobfuscated = crypt.deobfusicate(sObfuscated);
		System.out.println("Deobfuscated:"+sDeobfuscated);
		
		System.out.println("matched:"+sDeobfuscated.equals(sText));
	}
	*/
}
