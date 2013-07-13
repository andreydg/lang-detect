package language.model;

import java.io.IOException;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.SortedSet;

import language.model.NgramLanguageDetector.ClassificationAlgorithm;

/**
 * Generic interface for language detection
 * 
 * @author Andrey Gusev
 */
public interface LanguageDetector {

	/**
	 * @param text
	 *            - text for which we will detect language
	 * @return most likely language with default algorithm
	 */
	Locale getMostLikelyLanguage(String text) throws IOException;
	
	
	/**
	 * @param text
	 *            - text for which we will detect language
	 * @return most likely language with specified algorithm
	 */
	Locale getMostLikelyLanguage(String text, ClassificationAlgorithm algorithmToUse) throws IOException;

	/**
	 * returns ordered list of languages that are most similar to given text,
	 * using all nGram sizes specified for language detector
	 */
	SortedSet<Entry<Locale, Double>> detectLanguageWithLinearWeights(String text, boolean ignoreLowScores)
			throws IOException;

}
