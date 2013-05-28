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
 * Uses two word language detector to find boundaries of languages in
 * multi-lingual document and tag them with languages
 * 
 * @author Andrey Gusev
 * 
 */
public class TwoWordLanguageBoundaryDetector implements LanguageBoundaryDetector {

	private final ClassificationAlgorithm algorithmToUse;
	private final NgramLanguageDetector detector;

	public TwoWordLanguageBoundaryDetector(ClassificationAlgorithm algorithmToUse, NgramLanguageDetector detector) {
		this.algorithmToUse = algorithmToUse;
		this.detector = detector;

	}

	public List<Pair<String, Locale>> tagStringWithLanguages(String s) throws IOException {

		List<Pair<String, Locale>> retVal = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(s);
		StringBuilder currentString = new StringBuilder();
		Locale prevLocale = null;
		String prevToken = null;
		StringBuilder slidingWindow = new StringBuilder();
		while (st.hasMoreTokens()) {
			String currentToken = st.nextToken();
			slidingWindow.append(currentToken).append(" ");
			if (prevToken != null) {
				currentString.append(prevToken).append(" ");
			} else {
				// we need two words to do detection
				prevToken = currentToken;
				continue;
			}

			Locale locale = detector.getMostLikelyLanguage(slidingWindow.toString().trim(), algorithmToUse);
			if (!(locale == null || prevLocale == null || locale.equals(prevLocale))) {
				retVal.add(new Pair<>(currentString.toString().trim(), prevLocale));
				currentString.delete(0, currentString.length());
			}
			prevLocale = locale;
			// remove prevToken
			slidingWindow.delete(0, prevToken.length() + 1);

			prevToken = currentToken;

		}

		if (currentString.length() > 0) {
			retVal.add(new Pair<>(currentString.toString().trim(), prevLocale));
		}

		return retVal;
	}
}
