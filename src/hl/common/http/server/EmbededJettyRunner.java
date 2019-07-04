package hl.common.http.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.webapp.WebAppContext;

public class EmbededJettyRunner {
	
	private Server server 				= null;
	private int listening_port 			= 8080;
	private String folder_warfiles 		= null;
	
	public EmbededJettyRunner()
	{
	}

	public EmbededJettyRunner(int iListeningPort)
	{
		setServerPort(iListeningPort);
	}
	
	public void setServerPort(int iListeningPort)
	{
		this.listening_port = iListeningPort;
	}
	
	public void setWARFolder(String aWarFilesFolder) throws Exception
	{
		this.folder_warfiles = aWarFilesFolder;
		
		if(aWarFilesFolder!=null && aWarFilesFolder.length()>0)
		{
			File f = new File(this.folder_warfiles);
			if(!f.isDirectory())
			{
				throw new Exception("[Error] Invalid WAR file folder : "+aWarFilesFolder);
			}
		}
	}

	public void stopServer() throws Exception
	{
		if(server!=null && server.isStarted())
		{
			server.stop();
		}
	}
	
	public void forceStopServer() throws Exception
	{
		if(server!=null && server.isStarted())
		{
			server.destroy();
		}
	}
	
	public void startServer() throws Exception
	{
		server = new Server(this.listening_port);
		HandlerList handlers = new HandlerList();
		
        //
        
		Map<File, String> mapWARs = new HashMap<File, String>();
		
		if(folder_warfiles!=null)
		{
			File folderWAR = new File(folder_warfiles);
			for(File f : folderWAR.listFiles())
			{
				String FileName = f.getName();
				if(FileName.toLowerCase().endsWith(".war"))
				{
					String sContextRoot = "/"+FileName.substring(0, FileName.length()-4);
					sContextRoot = sContextRoot.replace("#", "/");
	
System.out.println(sContextRoot+" -> "+f.getAbsolutePath());
					
					mapWARs.put(f, sContextRoot);
				}
			}
			
			for(File fileWAR : mapWARs.keySet())
			{
				String sContextRoot = mapWARs.get(fileWAR);
		        WebAppContext webapp = new WebAppContext(fileWAR.getAbsolutePath(), sContextRoot);
		        handlers.addHandler(webapp);
			}
		}
        server.setHandler(handlers);
        server.start();
	}
	
	public static void main(String args[]) throws Exception
	{	
		EmbededJettyRunner j = new EmbededJettyRunner();
		j.setServerPort(8080);
		j.setWARFolder("./war");
		j.startServer();
	}
}
