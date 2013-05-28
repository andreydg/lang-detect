package language.model.multiling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import language.model.NgramLanguageDetector;
import language.model.NgramLanguageDetector.ClassificationAlgorithm;
import language.util.Pair;

/**
 * 
 * Uses one word language detector to find boundaries of languages in
 * multi-lingual document and tag them with languages
 * 
 * @author Andrey Gusev
 * 
 */
public class OneWordLanguageBoundaryDetector implements LanguageBoundaryDetector {

	private final ClassificationAlgorithm algorithmToUse;
	private final NgramLanguageDetector detector;

	public OneWordLanguageBoundaryDetector(ClassificationAlgorithm algorithmToUse, NgramLanguageDetector detector) {
		this.algorithmToUse = algorithmToUse;
		this.detector = detector;
	}

	/**
	 * This uses simply approach on looking at just one word at a time
	 */
	public List<Pair<String, Locale>> tagStringWithLanguages(String s) throws IOException {

		List<Pair<String, Locale>> retVal = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(s);
		StringBuilder currentString = new StringBuilder();
		Locale prevLocale = null;
		while (st.hasMoreTokens()) {
			String currentToken = st.nextToken();
			Locale locale = detector.getMostLikelyLanguage(currentToken, algorithmToUse);
			if (locale == null || prevLocale == null || locale.equals(prevLocale)) {
				currentString.append(currentToken).append(" ");
			} else {
				retVal.add(new Pair<>(currentString.toString().trim(), prevLocale));
				currentString.delete(0, currentString.length());
				currentString.append(currentToken).append(" ");
			}
			prevLocale = locale;
		}

		if (currentString.length() > 0) {
			retVal.add(new Pair<>(currentString.toString().trim(), prevLocale));
		}
		return retVal;
	}
}
