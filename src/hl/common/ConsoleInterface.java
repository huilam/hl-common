package hl.common;


public class ConsoleInterface {
	
	private static final String FONT_PREFIX = "\u001B[";
	
	public static final String FONT_RESET    = FONT_PREFIX+"0m";
	//
	public static final String FONT_BLACK    = FONT_PREFIX+"30m";
	public static final String FONT_RED      = FONT_PREFIX+"31m";
	public static final String FONT_GREEN    = FONT_PREFIX+"32m";
	public static final String FONT_YELLOW   = FONT_PREFIX+"33m";
	public static final String FONT_BLUE     = FONT_PREFIX+"34m";
	public static final String FONT_PURPLE   = FONT_PREFIX+"35m"; // or MAGENTA
	public static final String FONT_CYAN     = FONT_PREFIX+"36m";
	public static final String FONT_WHITE    = FONT_PREFIX+"37m";
	
	
}