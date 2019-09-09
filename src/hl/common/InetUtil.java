package hl.common;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class InetUtil {
	
	private static Pattern pattIPv4 = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
	
	public static boolean isIPv4(String aIPAddress)
	{
		Matcher m = pattIPv4.matcher(aIPAddress);
		return m.matches();
	}

	public static String getIPAddress(String aHostName) throws UnknownHostException
	{
		InetAddress inet = InetAddress.getByName(aHostName);
		return inet.getHostAddress();
	}
	
	public static String getClosestMatchIPAddress(
			InetAddress aInetAddress, List<InetAddress> aInetAddressList) throws UnknownHostException
	{
		String sTargetIP = aInetAddress.getHostAddress();
		String[] sTargetIPSegment = sTargetIP.split("\\.");
		
		int iHighestMatch = 0;
		String sMacthedIP = null;
		for(InetAddress inetIP : aInetAddressList)
		{
			String[] sIPSegments = inetIP.getHostAddress().split("\\.");
			if(sIPSegments!=null && sIPSegments.length == sTargetIPSegment.length)
			{
				int iMatchScore = 0;
				for(int i=0; i<sTargetIPSegment.length; i++)
				{
					if(sTargetIPSegment[i].equals(sIPSegments[i]))
						iMatchScore++;
				}
				if(iMatchScore > iHighestMatch)
				{
					iHighestMatch = iMatchScore;
					sMacthedIP = inetIP.getHostAddress();
				}
			}
		}
		
		return sMacthedIP;
	}
	
	public static List<InetAddress> getListOfLocalActiveIPAddress() throws SocketException
	{
		List<InetAddress> listIPAddr = new ArrayList<InetAddress>();
		
		Enumeration<NetworkInterface> e =  NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements()) 
		{
			NetworkInterface ni = e.nextElement();
			if(!ni.isLoopback() 
					&& ni.isUp()
					&& ni.getInterfaceAddresses().size()>0)
			{
				for(InterfaceAddress addr : ni.getInterfaceAddresses()) 
				{
					listIPAddr.add(addr.getAddress());
				}
			}
		}
		
		return listIPAddr;
	}
	
	
	public static void main(String args[]) throws Exception
	{
		List<InetAddress> listIPAddr = getListOfLocalActiveIPAddress();
		InetAddress inetEVAAddress = InetAddress.getByName("172.30.92.16");
		
		
		String sMatchedIP = getClosestMatchIPAddress(inetEVAAddress, listIPAddr);
		System.out.println("Matched IP :"+sMatchedIP);
	}
	
}