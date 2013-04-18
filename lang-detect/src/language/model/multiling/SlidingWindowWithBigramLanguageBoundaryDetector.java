package language.model.multiling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import language.model.NgramLanguageDetector;
import language.model.NgramLanguageDetector.ClassificationAlgorithm;
import language.util.Pair;

/**
 * 
 * Uses sliding window of language confidence values to detect window within
 * which language changes then uses bigram model to determine exact boundaries
 * if there is tie than order of preferences is used
 * 
 * @author Andrey Gusev
 * 
 */
public class SlidingWindowWithBigramLanguageBoundaryDetector extends BaseBigramLanguageBoundaryDetector {

	private final int windowSize;

	// maps window size to order lest of boundary preference for tie breakers
	private static final Map<Integer, int[]> boundaryPreferenceOrder;

	static {
		boundaryPreferenceOrder = new HashMap<Integer, int[]>();
		boundaryPreferenceOrder.put(2, new int[] { 0 });
		boundaryPreferenceOrder.put(3, new int[] { 1, 0 });
		boundaryPreferenceOrder.put(4, new int[] { 1, 2, 0 });
		boundaryPreferenceOrder.put(5, new int[] { 2, 1, 3, 0 });
		boundaryPreferenceOrder.put(6, new int[] { 2, 3, 1, 4, 0 });
	}

	public SlidingWindowWithBigramLanguageBoundaryDetector(ClassificationAlgorithm algorithmToUse,
			NgramLanguageDetector detector, int windowSize) throws IOException {
		super(algorithmToUse, detector);
		int[] orderPreference = boundaryPreferenceOrder.get(windowSize);
		assert orderPreference != null : "Unsupported window size";
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
				boolean isAllPostitive = true;
				// find the smallest count - thus the best
				// breaking point
				int smallestCount = Integer.MAX_VALUE;
				int smallestInd = Integer.MIN_VALUE;
				for (int ind : boundaryPreferenceOrder.get(this.windowSize)) {
					boundaryCounts[ind] = getBigramCount(prevTokens[ind + 1] + " " + prevTokens[ind]);
					isAllPostitive = isAllPostitive && (boundaryCounts[ind] > 0);
					if (boundaryCounts[ind] < smallestCount) {
						smallestCount = boundaryCounts[ind];
						smallestInd = ind;
					}
				}

				// neither breaking point is good
				// we're going to assume that detector made a mistake
				// and not break here
				if (!isAllPostitive) {
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
}
