package hl.common.nlp.regex.en;

public class QueryNormalizer {
	
	private static boolean IS_DEBUG = false;
	private static String REPLACE_PREFIX  = "{2:";
	private static String REPLACE_SUFFIX  = "}";
	private static String REPLACE_WORD    = "$1";
	
	protected static final String regexCasualWrapper_EN = "(?i)(?:" +
	        "(?:give|provide|tell)(?:\\s+me)?(?:\\s+more)?(?:\\s+information|\\s+info)?(?:\\s+about|\\s+for|\\s+on)|" +
	        "(?:can|could|would)\\s+you(?:\\s+(?:(?:clarify|explain|tell|show)(?:\\s+to)?(?:\\s+me)?))?|" +
	        "(?:what\\s+)?do you(?:\\s+think|\\s+know)?(?:\\s+of)?|" +
	        "\\bI would appreciate(?:\\s+it|\\s+if)?|" +
	        "\\bjust checking(?:\\s+out)?|" +
	        
			"\\bbe(?:\\s+so)?\\s+kind(?:\\s+as|\\s+enough)?\\s+to\\b|" +
	        "\\bhelp me(?:\\s+to)?\\s+understand|" +
	        "\\bwalk me\\s+(?:through|thru)|" +
	        "\\bwould you explain|" +
	        "\\bI am looking for|" +
	        "\\bI am wondering|" +
	        "\\bif you (?:could|can)|" +
	        "\\bI need(?:\\s+to(?:\\s+know)?)?|" +
	        "\\bI want(?:\\s+to(?:\\s+know)?)?|" +
	        "\\bjust wondering|" +
	        "\\bare there any|" +
	        "\\bdoes (?:it|this)\\b|" +
	        "\\b(?:would|will) be\\b|" +
	        "\\bWhat is the|" +
	        "\\bI need help|" +
	        "\\bcan I(?:\\s+know)?\\b|" +
	        "\\bwhat(?:'s|\\s+is)|" +
	        "\\b(?:kindly|please)\\b|" +
	        "\\bshow me|" +
	        "\\bis it\\b|" +
	        "\\bstrictly\\b|" +
	        "\\bactually\\b|" +
	        "\\bexactly\\b|" +
	        "\\bsometimes\\b|" +
	        "\\bdoes\\b|" +
	        "\\balso\\b|" +
	        "\\bsome\\b|" +
	        "\\bare\\b|" +
	        "\\bthe\\b|" +
	        "\\bthis\\b|" + // Added implicitly if used earlier, or leave to single words
	        "\\bthat\\b|" +
	        "\\bis\\b|" +
	        "\\ban\\b|" +
	        "\\ba\\b" +
	        ")\\s*";
	
	public static String normalizeQuery(String input) {
		if (input == null || input.isBlank()) {
			return "";
		}
		
		String sReplacement = IS_DEBUG ? REPLACE_PREFIX + REPLACE_WORD + REPLACE_SUFFIX:"";

		String normalizedQuery = input.replaceAll("("+regexCasualWrapper_EN+")", sReplacement);
		
		
		return normalizedQuery;
	}
}