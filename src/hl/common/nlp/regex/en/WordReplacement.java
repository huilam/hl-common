package hl.common.nlp.regex.en;

import java.util.regex.Pattern;

public class WordReplacement {

	private static java.util.Map<Pattern, String> REPLACEMENTS = new java.util.HashMap<>();

	static {
		initializeReplacements();
	}
	
	private static void initializeReplacements() {
		REPLACEMENTS.clear();
		
		REPLACEMENTS.put(Pattern.compile("(?i)\\bwhat do you think(?:\\s+of|\\s+about)?\\b"), "what is");
	    REPLACEMENTS.put(Pattern.compile("(?i)\\bwalk me (?:through|thru)\\b"), "explain");
	    REPLACEMENTS.put(Pattern.compile("(?i)\\bhelp me understand\\b"), "explain");
	    
	    REPLACEMENTS.put(Pattern.compile("(?i)\\bhow\\s+many\\b"), "number of");
	    REPLACEMENTS.put(Pattern.compile("(?i)\\bhow\\s+much\\b"), "amount of");
	    
	    REPLACEMENTS.put(Pattern.compile("(?i)\\b(?:should|I)\\s+(?:should|I)\\b"), "to");
	    
	    REPLACEMENTS.put(Pattern.compile("(?i)\\bHow\\s+can\\s+I\\b"), "can I");
	    REPLACEMENTS.put(Pattern.compile("(?i)\\bHow\\s+do\\s+you\\b"), "do you");
	    
	    //shorten
	    REPLACEMENTS.put(Pattern.compile("(?i)\\bwhat\\s+happen(?:s)?\\s+if\\b"), "what if");
	    REPLACEMENTS.put(Pattern.compile("(?i)\\b(?:tell|show)\\s+me\\s+how\\b"), "how");
	    REPLACEMENTS.put(Pattern.compile("(?i)\\b(?:tell|show)\\s+me\\s+what\\b"), "what");
	   
    }
	
	
	public static String replaceQueryWords(String input) {
		if (input == null || input.isBlank()) {
			return "";
		}
		String normalizedQuery = input.trim();
		
		for (java.util.Map.Entry<Pattern, String> entry : REPLACEMENTS.entrySet()) {
			normalizedQuery = entry.getKey().matcher(normalizedQuery).replaceAll(entry.getValue());
		}
		
		return normalizedQuery.trim();
	}
}