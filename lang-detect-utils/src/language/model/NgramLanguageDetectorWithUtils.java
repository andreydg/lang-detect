package language.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import language.model.multiling.BaseBigramLanguageBoundaryDetector;
import language.model.multiling.LanguageBoundaryDetector;
import language.model.multiling.NestedSlidingWindowWithBigramLanguageBoundaryDetector;
import language.model.multiling.OneWordLanguageBoundaryDetector;
import language.model.multiling.SlidingWindowWithBigramLanguageBoundaryDetector;
import language.model.multiling.ThreeWordLanguageBoundaryDetector;
import language.model.multiling.TwoWordLanguageBoundaryDetector;
import language.util.LanguageUtil;
import language.util.Pair;

/**
 * Uses ngram model in Eucledian ngram vector space to detect language of the
 * string at this point only supports one language in string and will likely
 * detect the language that is dominant in a string. The correctness of
 * detection increases with length of a string.
 * 
 * @author Andrey Gusev
 */
public class NgramLanguageDetectorWithUtils extends NgramLanguageDetector {

	private static final String SOURCE_DIR = "modelSource";
	private static final String MULTI_LANG_TEST_DIR = "multiLangTestSet";

	private static final int MAX_MULTI_LING_PHRASES = 30000;
	private static final String MULTI_LING_SEPARATOR = ":::";

	private final int minTrainingSampleLength;
	private final int maxTrainingSampleLength;

	public NgramLanguageDetectorWithUtils(File basePath, int minTrainingSampleLength, int maxTrainingSampleLength) {
		super(basePath);
		this.minTrainingSampleLength = minTrainingSampleLength;
		this.maxTrainingSampleLength = maxTrainingSampleLength;
	}

	public String generateLanguageModels() throws IOException {
		StringBuilder output = new StringBuilder(256);

		String locationBase = basePath.getAbsolutePath() + File.separator + "languagemodels" + File.separator;
		for (int nGramSize : ngramSet) {

			// go through all languages and generate ngram models
			for (Locale locale : locales) {

				String outLog = "\n\n******** Creating " + nGramSize + "-gram model for " + locale.toString()
						+ " ********\n";
				System.out.println(outLog);
				output.append(outLog);

				String sourceLocation = locationBase + SOURCE_DIR + File.separator + locale.toString();

				File fileWithText = new File(sourceLocation);
				if (!fileWithText.exists()) {
					output.append("+++++ Skipping generating ngram model for ").append(locale.toString());
					output.append(" since source file does not exist\n");
					continue;
				}

				NgramModel languageModel = new NgramModel(nGramSize);
				try (BufferedReader br = new BufferedReader(new FileReader(fileWithText))) {
					String s;
					while ((s = br.readLine()) != null) {
						languageModel = this.getNgramModelForText(s, languageModel, false);
					}
				}

				// get the model
				String ngramModel = languageModel.toString();

				String modelLocationDir = locationBase + MODEL_DIR + File.separator;
				File modelLocationDirFile = new File(modelLocationDir);
				if (!modelLocationDirFile.exists()) {
					modelLocationDirFile.mkdir();
				}

				String modelLocation = modelLocationDir + locale.toString() + "_" + nGramSize;

				try (BufferedWriter out = new BufferedWriter(new FileWriter(modelLocation))) {
					out.write(ngramModel);
				}
			}
		}

		return output.toString();
	}

	public String generateTrainingAndTestData() throws IOException {

		StringBuilder output = new StringBuilder(512);

		String locationBase = basePath.getAbsolutePath() + File.separator + "languagemodels" + File.separator;
		for (Locale locale : locales) {

			output.append("\n\n******** Creating training and test sets for ").append(locale.toString());
			output.append(" (").append(minTrainingSampleLength).append("-");
			output.append(maxTrainingSampleLength).append(") ********\n");

			String sourceLocation = locationBase + SOURCE_DIR + File.separator + locale.toString();

			File fileWithText = new File(sourceLocation);
			if (!fileWithText.exists()) {
				output.append("+++++ Skipping generating training and test sets for ").append(locale.toString());
				output.append(" since source file does not exist\n");
				continue;
			}

			List<String> trainingSet = new ArrayList<>();
			List<String> testSet = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new FileReader(fileWithText))) {

				String s;
				StringBuilder sb = new StringBuilder(128);
				int randomLength = generateRandomSampleLength();
				while ((s = br.readLine()) != null) {
					for (String word : LanguageUtil.tokenize(s, 1)) {
						randomLength--;
						if (randomLength < 0) {
							// add 90% of examples to training set and 10% to
							// test set
							if (Math.random() > .1) {
								trainingSet.add(sb.toString().trim());
							} else {
								testSet.add(sb.toString().trim());
							}
							// new random length
							randomLength = generateRandomSampleLength();
							sb.delete(0, sb.length() - 1);
						} else {
							sb.append(word).append(" ");
						}
					}
				}

				testSet.add(sb.toString().trim());
			}

			// write test set and training set
			String trainingTestSetLocation = locationBase + TRAINING_TEST_DIR + File.separator;
			File trainingTestSetLocationFile = new File(trainingTestSetLocation);
			if (!trainingTestSetLocationFile.exists()) {
				trainingTestSetLocationFile.mkdir();
			}

			try (BufferedWriter outForTraining = new BufferedWriter(new FileWriter(trainingTestSetLocation
					+ locale.toString() + "_training"));) {
				for (String training : trainingSet) {
					outForTraining.write(training + "\n");
				}
			}

			try (BufferedWriter outForTesting = new BufferedWriter(new FileWriter(trainingTestSetLocation
					+ locale.toString() + "_test"));) {
				for (String test : testSet) {
					outForTesting.write(test + "\n");
				}
			}
		}

		return output.toString();
	}

	public String generateMultiLingualTestData() throws IOException {

		StringBuilder output = new StringBuilder(512);

		String locationBase = basePath.getAbsolutePath() + File.separator + "languagemodels" + File.separator;

		BufferedReader sourceFiles[] = new BufferedReader[locales.length];

		// first open buffered reader into all files
		for (int ind = 0; ind < locales.length; ind++) {

			Locale locale = locales[ind];
			String sourceLocation = locationBase + TRAINING_TEST_DIR + File.separator + locale.toString() + "_test";

			File fileWithText = new File(sourceLocation);
			if (!fileWithText.exists()) {
				output.append("+++++ Skipping generating multilingual test sets for ").append(locale.toString());
				output.append(" since source test set file does not exist\n");
				continue;
			}

			sourceFiles[ind] = new BufferedReader(new FileReader(fileWithText));
		}

		output.append("\n\n******** Creating multilingual test set from existing test sets ********\n");

		// write test set and training set
		String multilingTestSetLocation = locationBase + MULTI_LANG_TEST_DIR + File.separator;
		File trainingTestSetLocationFile = new File(multilingTestSetLocation);
		if (!trainingTestSetLocationFile.exists()) {
			trainingTestSetLocationFile.mkdir();
		}

		try (BufferedWriter outForTest = new BufferedWriter(new FileWriter(multilingTestSetLocation + "testSet"));) {
			// total of MAX_MULTILNGUAL_PHRASES phrase in different languages
			// their length will be identical to length of phrases generated in
			// original test set files
			for (int ind = 0; ind < MAX_MULTI_LING_PHRASES; ind++) {
				String line = null;
				// get a line and write to test file
				while (line == null) {
					int random = (int) (Math.random() * sourceFiles.length);
					Locale locale = locales[random];
					BufferedReader br = sourceFiles[random];
					line = br != null ? br.readLine() : null;
					if (line != null) {
						outForTest.write(line + MULTI_LING_SEPARATOR + locale.toString() + "\n");
					}
				}
			}
		} finally {
			// close all the readers
			for (int ind = 0; ind < sourceFiles.length; ind++) {
				BufferedReader br = sourceFiles[ind];
				if (br != null) {
					br.close();
				}
			}
		}

		return output.toString();

	}

	private int generateRandomSampleLength() {
		return (int) (Math.random() * (maxTrainingSampleLength - minTrainingSampleLength) + minTrainingSampleLength);
	}

	public String runTestSet(ClassificationAlgorithm algorithmToUse) throws IOException {

		StringBuilder output = new StringBuilder(512);

		String locationBase = basePath.getAbsolutePath() + File.separator + "languagemodels" + File.separator;
		Map<Locale, Integer> localeErrorCount = new HashMap<>(locales.length * 2);
		Map<Locale, Integer> localeTotalCount = new HashMap<>(locales.length * 2);
		int totalCount = 0;
		int totalErrorCount = 0;
		for (Locale locale : locales) {

			String testSetLocation = locationBase + TRAINING_TEST_DIR + File.separator + locale.toString() + "_test";

			// need to read in UTF-8
			File file = new File(testSetLocation);
			if (!file.exists()) {
				continue;
			}

			String s;

			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF_ENCODING));) {
				while ((s = br.readLine()) != null) {
					incrementLocaleCounts(locale, localeTotalCount);
					Locale detectedLanguage = getMostLikelyLanguage(s, algorithmToUse);
					if (!locale.equals(detectedLanguage)) {
						incrementLocaleCounts(locale, localeErrorCount);
					}
				}
			}

			if (localeTotalCount.get(locale) != null && localeTotalCount.get(locale) > 0) {
				totalCount += localeTotalCount.get(locale);
				Integer errorCount = localeErrorCount.get(locale);
				if (errorCount == null) {
					errorCount = 0;
				}
				totalErrorCount += errorCount;
				output.append("Correct rate for ")
						.append(locale.toString())
						.append(": ")
						.append(decimalFormat.format(((double) (localeTotalCount.get(locale) - errorCount))
								/ localeTotalCount.get(locale))).append(" (").append(localeTotalCount.get(locale))
						.append(")\n");
			}

			// System.out.println(output.toString());

		}

		if (totalCount > 0) {
			output.append("Total rate:")
					.append(decimalFormat.format(((double) (totalCount - totalErrorCount)) / totalCount))
					.append(", total test set size:").append(totalCount).append("\n");

		}

		return output.toString();
	}

	public String runMultiLingualTestSet(ClassificationAlgorithm algorithmToUse,
			BoundaryDetectionAlgorithm boundaryDetector) throws IOException {

		StringBuilder output = new StringBuilder(512);

		String locationBase = basePath.getAbsolutePath() + File.separator + "languagemodels" + File.separator;
		String multilingTestSetLocation = locationBase + MULTI_LANG_TEST_DIR + File.separator + "testSet";

		File fileWithText = new File(multilingTestSetLocation);
		if (!fileWithText.exists()) {
			output.append("+++++ Error: can't open multilingual test file at ").append(multilingTestSetLocation);
			return output.toString();
		}

		Map<Locale, Integer> localeErrorCount = new HashMap<>(locales.length * 2);
		Map<Locale, Integer> localeTotalCount = new HashMap<>(locales.length * 2);

		Map<String, Locale> allLanguageStrings = new HashMap<>();

		String s;
		// this will accumulate entire document and
		StringBuilder entireDocument = new StringBuilder();
		int numDuplicates = 0;
		StringBuilder actualLanguageTags = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new FileReader(fileWithText))) {
			Locale prevLocale = null;
			StringBuilder prevString = new StringBuilder();
			while ((s = br.readLine()) != null) {
				String parts[] = s.split(MULTI_LING_SEPARATOR);
				assert (parts.length == 2);
				String testString = parts[0].trim();
				Locale locale = localeMap.get(parts[1]);
				incrementLocaleCounts(locale, localeTotalCount);

				if (prevLocale == null || locale.equals(prevLocale)) {
					prevString.append(testString).append(" ");
				} else {
					String stringToAdd = prevString.toString().trim();
					actualLanguageTags.append(stringToAdd).append(MULTI_LING_SEPARATOR);
					actualLanguageTags.append(prevLocale).append("\n");
					if (allLanguageStrings.containsKey(stringToAdd)) {
						numDuplicates++;
					} else {
						allLanguageStrings.put(stringToAdd, prevLocale);
					}
					prevString.delete(0, prevString.length());
					prevString.append(testString).append(" ");

				}
				prevLocale = locale;

				entireDocument.append(testString).append(" ");
				Locale detectedLanguage = getMostLikelyLanguage(testString, algorithmToUse);
				if (!locale.equals(detectedLanguage)) {
					incrementLocaleCounts(locale, localeErrorCount);
				}
			}
			String stringToAdd = prevString.toString().trim();
			if (stringToAdd.length() > 0) {
				actualLanguageTags.append(stringToAdd).append(MULTI_LING_SEPARATOR);
				actualLanguageTags.append(prevLocale).append("\n");
			}
		}

		int totalCount = 0;
		int totalErrorCount = 0;
		for (Locale locale : locales) {

			if (localeTotalCount.get(locale) != null && localeTotalCount.get(locale) > 0) {
				totalCount += localeTotalCount.get(locale);
				Integer errorCount = localeErrorCount.get(locale);
				if (errorCount == null) {
					errorCount = 0;
				}
				totalErrorCount += errorCount;
				output.append("Base correct rate for ")
						.append(locale.toString())
						.append(": ")
						.append(decimalFormat.format(((double) (localeTotalCount.get(locale) - errorCount))
								/ localeTotalCount.get(locale))).append(" (").append(localeTotalCount.get(locale))
						.append(")\n");
			}
		}

		if (totalCount > 0) {
			output.append("Base total rate:")
					.append(decimalFormat.format(((double) (totalCount - totalErrorCount)) / totalCount))
					.append(", total test set size:").append(totalCount).append("\n");

		}

		// now use boundary detection to determine samples
		LanguageBoundaryDetector detector = null;
		switch (boundaryDetector) {
		case ONE_WORD:
			detector = new OneWordLanguageBoundaryDetector(algorithmToUse, this);
			break;
		case TWO_WORD:
			detector = new TwoWordLanguageBoundaryDetector(algorithmToUse, this);
			break;
		case THREE_WORD:
			detector = new ThreeWordLanguageBoundaryDetector(algorithmToUse, this);
			break;
		case BASE_BIGRAM:
			detector = new BaseBigramLanguageBoundaryDetector(algorithmToUse, this);
			break;
		case TWO_WORD_BIGRAM:
			detector = new SlidingWindowWithBigramLanguageBoundaryDetector(algorithmToUse, this, 2);
			break;
		case THREE_WORD_BIGRAM:
			detector = new SlidingWindowWithBigramLanguageBoundaryDetector(algorithmToUse, this, 3);
			break;
		case FOUR_WORD_BIGRAM:
			detector = new SlidingWindowWithBigramLanguageBoundaryDetector(algorithmToUse, this, 4);
			break;
		case FIVE_WORD_BIGRAM:
			detector = new SlidingWindowWithBigramLanguageBoundaryDetector(algorithmToUse, this, 5);
			break;
		case SIX_WORD_BIGRAM:
			detector = new SlidingWindowWithBigramLanguageBoundaryDetector(algorithmToUse, this, 6);
			break;
		case FIVE_WORD_NESTED:
			detector = new NestedSlidingWindowWithBigramLanguageBoundaryDetector(algorithmToUse, this, 5);
			break;
		}
		List<Pair<String, Locale>> languageTags = detector.tagStringWithLanguages(entireDocument.toString());

		int incorrectlyClassified = 0;
		int incorrectlyFound = 0;
		int correctPhrases = 0;

		StringBuilder runOutput = new StringBuilder();
		for (Pair<String, Locale> pair : languageTags) {
			runOutput.append(pair.getFirst()).append(MULTI_LING_SEPARATOR);
			runOutput.append(pair.getSecond()).append("\n");
			Locale trueLocale = allLanguageStrings.get(pair.getFirst());
			if (trueLocale == null) {
				incorrectlyFound++;
			} else {
				Locale taggedLocale = pair.getSecond();
				if (taggedLocale == null || !taggedLocale.equals(trueLocale)) {
					incorrectlyClassified++;
				} else {
					correctPhrases++;
				}
			}
		}

		// write the results of the run for analysis
		String multilingOutput = locationBase + MULTI_LANG_TEST_DIR + File.separator + "runOutput";
		try (BufferedWriter out = new BufferedWriter(new FileWriter(multilingOutput));) {
			out.write(runOutput.toString());
		}

		// write the results of the run for analysis
		String multilingExactSet = locationBase + MULTI_LANG_TEST_DIR + File.separator + "exactSetOutput";
		try (BufferedWriter out = new BufferedWriter(new FileWriter(multilingExactSet));) {
			out.write(actualLanguageTags.toString());
		}

		output.append("\n---------- Boundary detection results----------\n");
		output.append("\nNumber of phrases found:").append(languageTags.size()).append("\n");
		output.append("Number of incorrect phrases found:").append(incorrectlyFound).append("\n");
		output.append("Number of incorrectly tagged phrases:").append(incorrectlyClassified).append("\n");
		output.append("Number of correctly tagged phrases:").append(correctPhrases).append("\n");
		int totalNumberOfPhrase = allLanguageStrings.size() + numDuplicates;
		output.append("Actual number of phrases:").append(totalNumberOfPhrase).append("\n");
		double recall = ((double) correctPhrases) / totalNumberOfPhrase;
		output.append("Correct rate(recall):").append(decimalFormat.format(recall)).append("\n");
		double precision = ((double) correctPhrases) / languageTags.size();
		output.append("Correct found rate(precision):").append(decimalFormat.format(precision)).append("\n");
		output.append("F1:").append(decimalFormat.format(2 * precision * recall / (precision + recall))).append("\n");

		return output.toString();
	}

	private void incrementLocaleCounts(Locale locale, Map<Locale, Integer> map) {
		Integer currentCount = map.get(locale);
		if (currentCount == null) {
			map.put(locale, 1);
		} else {
			map.put(locale, currentCount + 1);
		}
	}
}
