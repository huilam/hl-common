package hl.common.nlp.regex.en;

public class GreetingPatterns {
	
	private static boolean IS_DEBUG = false;
	private static String REPLACE_PREFIX  = "{1:";
	private static String REPLACE_SUFFIX  = "}";
	private static String REPLACE_WORD    = "$1";
	
    // =====================================================================
    // ENGLISH (EN)
    // =====================================================================

	// 1. Casual Greetings
	protected static final String regexCasualGreeting_EN = "(?i)\\b(hi|hey|hello|greetings|yo)\\b(?:\\s+\\w+\\s*[,.!]+|[,.!\\s]*)\\s*";
    // 2. Time-of-Day Greetings
    protected static final String regexTimeOfDayGreeting_EN = "(?i)\\b(?:good\\s+)?(morning|afternoon|even(?:i)?ng|night)\\b(?:\\s+\\w+)*[,.!\\s]*";
    
    
    // 3. Catches polite words and polite request prefixes at the start of a sentence
    protected static final String regexPrefixPolite_EN = "(?i)^(?:" +
            // 1. Hybrid polite request wrappers & intent phrases (Longest phrases first)
            "i(?:'d|\\s+would)\\s+like\\s+to\\s+know|" +
            "i(?:'d|\\s+would)\\s+appreciate(?:\\s+it)?|" +
            "can\\s+you\\s+please|could\\s+you\\s+please|would\\s+you\\s+please|" +
            "please\\s+tell\\s+me|please\\s+show\\s+me|please\\s+help\\s+me|please\\s+explain|" +
            
            // 2. Polite phrases with gratitude/advance notice (Extended with "so much for")
            "th(?:ank(?:s)?|x)(?:\\s+so\\s+much(?:\\s+for)?|\\s+a\\s+lot|\\s+in\\s+advance)?|" +
            "thank\\s+you(?:\\s+so\\s+much(?:\\s+for)?)?|" +
            
            // 3. Standalone starting polite words (supports optional preceding punctuation like ", please !")
            "(?:[,.!]+\\s*)?please|(?:[,.!]+\\s*)?kindly" +
            ")\\b\\s*[,.!]*\\s*";
    
    // 4. Catches ending polite phrases using a unified root group for thank/thx
    protected static final String regexEndingPolite_EN = "(?i)\\s*[,.]?\\s*(?:th(?:ank(?:s)?|x)(?:\\s+a\\s+lot|\\s+so\\s+much|\\s+in\\s+advance)?|please)\\s*[,.!]*$";
    
    // =====================================================================
    // =====================================================================
    // =====================================================================
    public static String removeGreetings(String input) {
		if (input == null || input.isBlank()) {
			return "";
		}
		
		String sReplacement = IS_DEBUG ? REPLACE_PREFIX + REPLACE_WORD + REPLACE_SUFFIX:"";
		
		
		String cleanedInput = input.trim();
		cleanedInput = cleanedInput.replaceAll("("+regexCasualGreeting_EN+"|"+regexTimeOfDayGreeting_EN+")", sReplacement);
		cleanedInput = cleanedInput.replaceAll("("+regexPrefixPolite_EN+"|"+regexEndingPolite_EN+")", sReplacement);
		
		return cleanedInput;
	}
}