package hl.common;

import redis.clients.jedis.Jedis;

public class KVStoreClient {

	private Jedis c = null;
	
	public KVStoreClient(String aHostUrl, int aPort, String... others)
	{
		c = new Jedis(aHostUrl, aPort);
	}
	
	public String put(String aKey, String aValue)
	{
		return c.set(aKey, aValue);
	}
	
	public String get(String aKey)
	{
		return c.get(aKey);
	}
	
	public String remove(String aKey)
	{
		String sDeleted = c.get(aKey);
		if(c.del(aKey)>0)
			return sDeleted;
		else
			return null;
	}
	
	public String save()
	{
		return c.save();
	}	
	
	public static void main(String args[])
	{
		KVStoreClient c = new KVStoreClient("127.0.0.1", 6379);
		
		System.out.println(c.get("test1"));
		System.out.println(c.put("test1", "brrrbbbbbdddddd"));
		//c.save();
	}
	

}
