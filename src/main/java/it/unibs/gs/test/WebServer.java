package it.unibs.gs.test;

public class WebServer
{
	private static WebServer instance;
		
	public static WebServer getInstance()
	{
		if (instance == null)
			instance = new WebServer();
		
		return instance;
	}
	
	private WebServer()
	{
		
	}
	
}
