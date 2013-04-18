package language.model;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

/**
 * Generic interface for language detection
 * 
 * @author Andrey Gusev
 */
public interface LanguageDetector {

	/**
	 * @param text
	 *            - text for which we will detect language
	 * @return most likely language
	 */
	Locale getMostLikelyLanguage(String text) throws IOException;

	/**
	 * returns ordered list of languages that are most similar to given text,
	 * using all nGram sizes specified for language detector
	 */
	List<Entry<Locale, Double>> detectLanguageWithLinearWeights(String text, boolean ignoreLowScores)
			throws IOException;

}
