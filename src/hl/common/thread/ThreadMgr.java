package hl.common.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ThreadMgr {
    
	private List<Thread> list_threads = null;
	
	public ThreadMgr() 
	{	
		this.list_threads = new ArrayList<Thread>();
	}
	
	public void addThreads(Collection<Thread> aThreads) 
	{	
		this.list_threads.addAll(aThreads);
	}
	
	public void addThread(Thread aThread) 
	{	
		this.list_threads.add(aThread);
	}
	
	public void clearThreads() 
	{	
		this.list_threads.clear();
	}

	public long startAllThreads()
	{
		int iThreadRan = 0;
		for(Thread t : this.list_threads)
		{
			if(!t.isAlive())
			{
				t.start();
				iThreadRan++;
			}
		}
		return iThreadRan;
	}
	
	public long waitForAllThreadsToComplete()
	{
		return waitForAllThreadsToComplete(50);
	}
	
	public long waitForAllThreadsToComplete(long aSleepMs)
	{
		long lStartTime = System.currentTimeMillis();
		
		boolean running = true;
		while(running)
		{
			running = false;
			for(Thread t : this.list_threads)
			{
				if(t.isAlive())
				{
					running = true;
				}
			}
			
			if(running)
			{
				try {
					Thread.sleep(aSleepMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		return System.currentTimeMillis()-lStartTime;
	}
	
}
