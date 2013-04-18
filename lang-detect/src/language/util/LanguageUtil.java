package language.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Set of utils for language algorithms
 * 
 * @author Andrey Gusev
 */
public class LanguageUtil {

	// all possible delimiters
	// note that there are two different spaces here
	private final static String delimeters = " \t\n\r\f.()[]{}^+=|_*&%#\"',-:;/\\?!@~\u201c\u201d\u3000»«";

	private static final int MAX_WORD_LENGTH = 200;

	private static final String WORD_BOUNDARY_CHAR = "$";

	// allows to cache computation of ngrams
	public static final Map<Integer, Map<String, Set<String>>> nGramCache = new HashMap<Integer, Map<String, Set<String>>>();

	/**
	 * computes jaccard coefficient for two sets
	 */
	public static float getJaccardCoefficient(Set<String> set1, Set<String> set2) {
		Set<String> intersection = new HashSet<String>(set1);
		intersection.retainAll(set2);
		Set<String> union = new HashSet<String>(set1);
		union.addAll(set2);
		return ((float) intersection.size()) / union.size();
	}

	/**
	 * 
	 * tokenizes the string using delimeters =
	 * " \t\n\r\f.()[]{}^+=|_*&%#\"',-:;/\\?!@~\u201c\u201d\u3000" and can check
	 * against alphabetPatterns = Pattern.compile("[a-zA-Z]+"); and only returns
	 * only tokens.length()>=minLength
	 * 
	 * @param text
	 *            - text
	 * @param skipAlphabetCheck
	 *            - skips Alphabet Check, if false will check tokens against
	 *            Pattern.compile("[a-zA-Z]+")
	 * @param minLength
	 *            - min length, will not return tokens.length()< minLength, use
	 *            0 for all tokens
	 * 
	 */
	public static List<String> tokenize(String text, int minLength) {
		List<String> retVal = new LinkedList<String>();
		StringTokenizer tokenizer = new StringTokenizer(text, delimeters);
		while (tokenizer.hasMoreTokens()) {
			String word = tokenizer.nextToken();
			word = word.toLowerCase().trim();
			if (word.length() >= minLength && word.length() < MAX_WORD_LENGTH) {
				retVal.add(word);
			}
		}
		return retVal;
	}
	
	public static Set<String> getNgramsForWordCombination(String prevWord, String nextWord, int ngramSize) {
		Set<String> initialNgrams = getNgrams(prevWord + WORD_BOUNDARY_CHAR + nextWord, ngramSize, false);
		Set<String> retVal = new HashSet<String>(initialNgrams.size());
		for (String ngram : initialNgrams) {
			// should containt word boundary character but not at first or last position
			// otherwise we are not capturing elements from both words
			if (ngram.contains(WORD_BOUNDARY_CHAR)
					&& !WORD_BOUNDARY_CHAR.equals(String.valueOf(ngram.charAt(ngramSize - 1)))
					&& !WORD_BOUNDARY_CHAR.equals(String.valueOf(ngram.charAt(0)))) {
				retVal.add(ngram);
			}
		}
		return retVal;
	}

	/**
	 * computes kgrams for a given word
	 */
	public static Set<String> getNgrams(String word, int ngramSize) {
		return getNgrams(word, ngramSize, true);
	}

	/**
	 * computes kgrams for a given word
	 */
	public static Set<String> getNgrams(String word, int ngramSize, boolean addWordBoundaryMarkers) {
		Map<String, Set<String>> wordCache = nGramCache.get(ngramSize);
		if (wordCache == null) {
			wordCache = new HashMap<String, Set<String>>();
			nGramCache.put(ngramSize, wordCache);
		}

		// if the word is null or has fewer than k character
		// return empty set
		// add two for boundary markers
		if (word == null || (word.length() + 2) < ngramSize) {
			return Collections.emptySet();
		}

		if (addWordBoundaryMarkers) {
			word = WORD_BOUNDARY_CHAR + word + WORD_BOUNDARY_CHAR;
		}

		Set<String> existingKGramSet = wordCache.get(word);
		if (existingKGramSet != null) {
			return existingKGramSet;
		}

		Set<String> retSet = new HashSet<String>(word.length() + 2 - ngramSize);
		StringBuilder currentKgram = new StringBuilder();
		int ind = 0;
		while (ind < word.length()) {
			Character chr = word.charAt(ind);

			switch (Character.getType(chr)) {
			case Character.END_PUNCTUATION:
			case Character.DASH_PUNCTUATION:
			case Character.START_PUNCTUATION:
			case Character.CONNECTOR_PUNCTUATION:
			case Character.OTHER_PUNCTUATION:
				// just add whitespace instead of punctuation
				chr = ' ';
				break;
			default:
			}

			currentKgram.append(chr);
			if (currentKgram.length() == ngramSize) {
				String kGram = currentKgram.toString();
				// if after trimming ngram still has the length of
				// 2 which means it didn't contain any whitespace to replace the
				// punctuation
				// we can add it
				kGram = kGram.replaceAll("\\s", "");
				kGram = kGram.replaceAll("\\d", "");
				if (kGram.length() == ngramSize) {
					kGram = kGram.toLowerCase();
					if (ngramSize == 1 && Character.isLetter(kGram.charAt(0))) {
						retSet.add(kGram);
					} else if (ngramSize > 1) {
						retSet.add(kGram);
					}
				}
				currentKgram = new StringBuilder();
				ind -= (ngramSize - 2);
			} else {
				ind++;
			}
		}
		// wrap into unmodifiable set
		wordCache.put(word, Collections.unmodifiableSet(retSet));
		return wordCache.get(word);
	}
}
