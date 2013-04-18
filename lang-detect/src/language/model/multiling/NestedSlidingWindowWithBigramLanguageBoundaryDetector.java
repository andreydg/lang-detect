package language.model.multiling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import language.model.NgramLanguageDetector;
import language.model.NgramLanguageDetector.ClassificationAlgorithm;
import language.util.Pair;

public class NestedSlidingWindowWithBigramLanguageBoundaryDetector extends BaseBigramLanguageBoundaryDetector {

	private final int windowSize;
	private static final int NESTED_WINDOW = 3;

	public NestedSlidingWindowWithBigramLanguageBoundaryDetector(ClassificationAlgorithm algorithmToUse,
			NgramLanguageDetector detector, int windowSize) throws IOException {
		super(algorithmToUse, detector);
		assert this.windowSize > NESTED_WINDOW : "Window size needs to be larger than nested window";
		this.windowSize = windowSize;
	}

	@Override
	public List<Pair<String, Locale>> tagStringWithLanguages(String s) throws IOException {
		List<Pair<String, Locale>> retVal = new ArrayList<Pair<String, Locale>>();

		StringTokenizer st = new StringTokenizer(s);
		StringBuilder currentString = new StringBuilder();
		Locale prevLocale = null;
		StringBuilder slidingWindow = new StringBuilder();

		// prevToken[0],prevToken[1],...,prevToken[maxPrevTokens-1]
		String[] prevTokens = new String[windowSize];
		Pair<String, Locale> previousPair = null;
		while (st.hasMoreTokens()) {
			prevTokens[0] = st.nextToken();
			slidingWindow.append(prevTokens[0]).append(" ");
			if (prevTokens[windowSize - 1] != null) {
				currentString.append(prevTokens[windowSize - 1]).append(" ");
			} else {
				// move down the tokens
				for (int ind = windowSize - 1; ind >= 1; ind--) {
					prevTokens[ind] = prevTokens[ind - 1];
				}
				prevTokens[0] = null;
				continue;
			}
			Locale locale = getLanguageWithDefault(slidingWindow.toString().trim());
			// remove the last token
			slidingWindow.delete(0, prevTokens[windowSize - 1].length() + 1);

			// if the locale didn't match we're probably have boundary point
			if (!(locale == null || prevLocale == null || locale.equals(prevLocale))) {
				// now determine which boundary point is better

				// boundaryCounts[0] is the boundary between
				// prevToken[0],prevToken[1], etc.
				Integer[] boundaryCounts = new Integer[windowSize - 1];

				// find the best breaking point by delegating to smaller window
				// size
				int smallestInd = getBreakingPoint(new StringBuilder(prevTokens[windowSize - 1]).append(" ").append(
						slidingWindow.toString()).toString().trim());

				boolean shouldSkipSplit = (smallestInd == -1);

				// neither breaking point is good
				// we're going to assume that detector made a mistake
				// and not break here
				if (!shouldSkipSplit) {
					// adjust current string and sliding window
					for (int ind = boundaryCounts.length - 2; ind >= smallestInd; ind--) {
						currentString.append(prevTokens[ind + 1]).append(" ");
						slidingWindow.delete(0, prevTokens[ind + 1].length() + 1);
						prevTokens[ind + 1] = null;
					}

					String stringToAdd = currentString.toString().trim();
					Pair<String, Locale> thisPair = new Pair<String, Locale>(stringToAdd, this
							.getLanguageWithDefault(stringToAdd));
					// if the languages are the same we can collapse it into one
					// language
					if (previousPair != null && previousPair.getSecond().equals(thisPair.getSecond())) {
						retVal.remove(retVal.size() - 1);
						previousPair = new Pair<String, Locale>(previousPair.getFirst() + " " + stringToAdd,
								previousPair.getSecond());
						retVal.add(previousPair);
					} else {
						retVal.add(thisPair);
						previousPair = thisPair;
					}

					currentString.delete(0, currentString.length());
					// reset locale so that prevLocale will be null for next
					// token
					locale = null;
				}
			}
			prevLocale = locale;

			// move down the tokens
			for (int ind = windowSize - 1; ind >= 1; ind--) {
				prevTokens[ind] = prevTokens[ind - 1];
			}
			prevTokens[0] = null;
		}

		if (currentString.length() > 0 || slidingWindow.length() > 0) {
			String stringToAdd = currentString.toString().trim() + " " + slidingWindow.toString().trim();
			stringToAdd = stringToAdd.trim();
			Pair<String, Locale> thisPair = new Pair<String, Locale>(stringToAdd, this
					.getLanguageWithDefault(stringToAdd));
			// if the languages are the same we can collapse it into one
			// language
			if (previousPair != null && previousPair.getSecond().equals(thisPair.getSecond())) {
				retVal.remove(retVal.size() - 1);
				previousPair = new Pair<String, Locale>(previousPair.getFirst() + " " + stringToAdd, previousPair
						.getSecond());
				retVal.add(previousPair);
			} else {
				retVal.add(thisPair);
				previousPair = thisPair;
			}
		}

		return retVal;
	}

	private int getBreakingPoint(String s) throws IOException {
		LanguageBoundaryDetector smallerDetector = new SlidingWindowWithBigramLanguageBoundaryDetector(
				this.algorithmToUse, this.detector, NESTED_WINDOW);
		List<Pair<String, Locale>> retVal = smallerDetector.tagStringWithLanguages(s);
		// hopefully there was exactly one split - otherwise we'll take the
		// first one
		// if there were no splits then we will return -1 indicating that
		if (retVal.size() == 1) {
			return -1;
		}

		String firstStringSplit = retVal.get(0).getFirst();
		String[] parts = firstStringSplit.split(" ");
		String targetToken = parts[parts.length - 1];

		StringTokenizer st = new StringTokenizer(s);
		int boundary = this.windowSize - 2;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.equals(targetToken)) {
				return boundary;
			}
			boundary--;
		}

		return -1;

	}
}
