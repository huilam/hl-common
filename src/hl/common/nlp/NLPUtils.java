package hl.common.nlp;

import hl.common.nlp.regex.en.GreetingPatterns;
import hl.common.nlp.regex.en.QueryNormalizer;
import hl.common.nlp.regex.en.WordReplacement;

public class NLPUtils {
	
	
	public static String refineQuery(String input) {
		if (input == null || input.isBlank()) {
			return "";
		}
		
		String sRefinedQuery = GreetingPatterns.removeGreetings(input);
		
		sRefinedQuery = WordReplacement.replaceQueryWords(sRefinedQuery);
		
		sRefinedQuery = QueryNormalizer.normalizeQuery(sRefinedQuery);
		
		return sRefinedQuery;
	}
}