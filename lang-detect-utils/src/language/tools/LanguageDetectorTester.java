package language.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import language.model.NgramLanguageDetectorWithUtils;
import language.model.NgramLanguageDetector.BoundaryDetectionAlgorithm;
import language.model.NgramLanguageDetector.ClassificationAlgorithm;
import language.model.multiling.LanguageBoundaryDetector;
import language.model.multiling.SlidingWindowWithBigramLanguageBoundaryDetector;
import language.util.Pair;

/**
 * Command line tester for NgramLanguageDetector
 * 
 * @author Andrey Gusev
 * 
 */
public class LanguageDetectorTester {

	private static final String VERBOSE_PARAM = "-verbose";
	private static final String GEN_MODELS_PARAM = "-genModels";
	private static final String GEN_TRAIN_TEST_SET_PARAM = "-genTrainTest";
	private static final String GEN_MULTI_LING_TEST_SET_PARAM = "-genMultiLingSet";
	private static final String RUN_TEST_SET_PARAM = "-runTestSet";
	private static final String RUN_MULTI_LING_TEST_SET_PARAM = "-runMultiTestSet";
	private static final String BOUNDARY_DETECTOR = "-boundaryDetector";
	private static final String CLASSIFIER_SELECTION_PARAM = "-useClassifier";
	private static final String TEST_STRING_PARAM = "-testString";
	private static final String TEST_MULTI_STRING_PARAM = "-testMultiString";
	private static final String DATA_PATH_PARAM = "-dataPath";

	private static final String MIN_TRAIN_PARAM = "-minTrainSize";
	private static final String MAX_TRAIN_PARAM = "-maxTrainSize";

	private static DecimalFormat decimalFormat;
	private static final int scale = 3;

	static {
		decimalFormat = new DecimalFormat();
		decimalFormat.setMinimumFractionDigits(scale);
		decimalFormat.setMaximumFractionDigits(scale);
	}

	public static void main(String[] args) throws IOException {

		Map<String, String> argValues = simpleCommandLineParser(args);

		boolean verbose = argValues.containsKey(VERBOSE_PARAM);
		boolean genModels = argValues.containsKey(GEN_MODELS_PARAM);
		boolean genTrainingAndTestSet = argValues.containsKey(GEN_TRAIN_TEST_SET_PARAM);
		boolean genMultiLingualSet = argValues.containsKey(GEN_MULTI_LING_TEST_SET_PARAM);
		boolean runTestSet = argValues.containsKey(RUN_TEST_SET_PARAM);
		boolean runMultiLingTestSet = argValues.containsKey(RUN_MULTI_LING_TEST_SET_PARAM);
		BoundaryDetectionAlgorithm boundaryDetectionAlgorithm = BoundaryDetectionAlgorithm.ONE_WORD;
		String boundaryDetectorEnum = argValues.get(BOUNDARY_DETECTOR);
		if (boundaryDetectorEnum != null) {
			try {
				boundaryDetectionAlgorithm = BoundaryDetectionAlgorithm.valueOf(boundaryDetectorEnum);
			} catch (Exception e) {
				System.out.println("Error parsing BoundaryDetectionAlgorithm from -boundaryDetector param");
				System.exit(1);
			}
		}

		int minTrainingSampleSize = argValues.containsKey(MIN_TRAIN_PARAM) ? Integer.valueOf(argValues
				.get(MIN_TRAIN_PARAM)) : 4;
		int maxTrainingSampleSize = argValues.containsKey(MAX_TRAIN_PARAM) ? Integer.valueOf(argValues
				.get(MAX_TRAIN_PARAM)) : 8;
		// 1 - only linear classifier
		// 2 - bagged decision tree
		// 4 - logisitic classifier
		// for example 7 selects all of them
		int classifierSelection = argValues.containsKey(CLASSIFIER_SELECTION_PARAM) ? Integer.valueOf(argValues
				.get(CLASSIFIER_SELECTION_PARAM)) : 1;

		String basePath = argValues.get(DATA_PATH_PARAM);
		if (basePath == null) {
			basePath = "";
		}

		File baseFilePath = new File(basePath);
		assert (baseFilePath.exists());

		NgramLanguageDetectorWithUtils detector = new NgramLanguageDetectorWithUtils(baseFilePath, minTrainingSampleSize,
				maxTrainingSampleSize);

		String testString = null;
		if (argValues.containsKey(TEST_STRING_PARAM)) {
			System.out.print("Enter your test string: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				testString = br.readLine();
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your string!");
				System.exit(1);
			}
		}

		String testMultiString = null;
		if (argValues.containsKey(TEST_MULTI_STRING_PARAM)) {
			 do {
				System.out.print("\nEnter your test multi language string(no string to exit): ");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				try {
					testMultiString = br.readLine();
				} catch (IOException ioe) {
					System.out.println("IO error trying to read your string!");
					System.exit(1);
				}

				if (testMultiString != null && testMultiString.length() > 0) {
					LanguageBoundaryDetector boundaryDetector = new SlidingWindowWithBigramLanguageBoundaryDetector(
							ClassificationAlgorithm.LINEAR_WEIGHTS, detector, 4);

					List<Pair<String, Locale>> retVal = boundaryDetector.tagStringWithLanguages(testMultiString);
					System.out.println("---------- Language Tagged String----------\n");
					for (Pair<String, Locale> pair : retVal) {
						System.out.println(pair.getFirst() + " -> " + pair.getSecond());
					}
				}
			} while (testMultiString != null && testMultiString.length() > 0);
		}

		// generate models from source text
		if (genModels) {
			String output = detector.generateLanguageModels();
			printIfVerbose(verbose, output);
		}

		// generate training and test data
		if (genTrainingAndTestSet) {
			String output = detector.generateTrainingAndTestData();
			printIfVerbose(verbose, output);
		}

		// generate multilingual test set
		if (genMultiLingualSet) {
			String output = detector.generateMultiLingualTestData();
			printIfVerbose(verbose, output);
		}

		// run test set with multiple languages
		if (runMultiLingTestSet) {
			String output = null;
			if ((classifierSelection & 4) > 0) {
				System.out.println("---------- Logisitic classifier results----------\n");
				output = detector.runMultiLingualTestSet(ClassificationAlgorithm.LOGISTIC_CLASSIFIER,
						boundaryDetectionAlgorithm);
				printIfVerbose(verbose, output);
			}

			if ((classifierSelection & 2) > 0) {
				System.out.println("---------- Decision tree results----------\n");
				output = detector.runMultiLingualTestSet(ClassificationAlgorithm.BAGGED_DECISION_TREE,
						boundaryDetectionAlgorithm);
				printIfVerbose(verbose, output);
			}

			if ((classifierSelection & 1) > 0) {
				System.out.println("---------- Linear weight results----------\n");
				output = detector.runMultiLingualTestSet(ClassificationAlgorithm.LINEAR_WEIGHTS,
						boundaryDetectionAlgorithm);
				printIfVerbose(verbose, output);
			}
		}

		// run test set with single language
		if (runTestSet) {

			String output = null;
			if ((classifierSelection & 4) > 0) {
				System.out.println("---------- Logistic classifier results----------\n");
				output = detector.runTestSet(ClassificationAlgorithm.LOGISTIC_CLASSIFIER);
				printIfVerbose(verbose, output);
			}

			if ((classifierSelection & 2) > 0) {
				System.out.println("---------- Decision tree results----------\n");
				output = detector.runTestSet(ClassificationAlgorithm.BAGGED_DECISION_TREE);
				printIfVerbose(verbose, output);
			}

			if ((classifierSelection & 1) > 0) {
				System.out.println("---------- Linear weight results----------\n");
				output = detector.runTestSet(ClassificationAlgorithm.LINEAR_WEIGHTS);
				printIfVerbose(verbose, output);
			}
		}

		// detect the most likely language for given string
		if (testString != null && testString.length() > 0) {

			List<Map<Locale, Double>> listOfNGramResults = new ArrayList<Map<Locale, Double>>();
			for (int nGram : detector.getNgramSet()) {
				Map<Locale, Double> cosineSimilarity = detector.getRawCosineSimilarities(testString, nGram, true);
				listOfNGramResults.add(cosineSimilarity);
			}

			// System.out.println("Detecting language of '" + testString + "'");
			System.out.println("---------- NGram Cosine Similarity Language Detection----------\n");
			System.out.println("Lang\tScore\t1-gram\t2-gram\t3-gram\t4-gram\t5-gram ");

			// now print out overall scores
			for (Entry<Locale, Double> entry : detector.detectLanguageWithLinearWeights(testString, true)) {

				StringBuilder sb = new StringBuilder(128);
				for (Map<Locale, Double> cosineSimilarity : listOfNGramResults) {
					sb.append(" \t ").append(decimalFormat.format(cosineSimilarity.get(entry.getKey())));
				}

				System.out.println(entry.getKey() + " \t " + decimalFormat.format(entry.getValue()) + sb.toString());
			}
		}
	}

	private static void printIfVerbose(boolean verbose, String output) {
		if (verbose) {
			System.out.println(output);
		}
	}

	/**
	 * Simple method which turns an array of command line arguments into a map,
	 * where each token starting with a '-' is a key and the following non '-'
	 * initial token, if there is one, is the value. For example, '-size 5
	 * -verbose' will produce a map with entries (-size, 5) and (-verbose,
	 * null).
	 * 
	 * @author Dan Klein
	 */
	private static Map<String, String> simpleCommandLineParser(String[] args) {
		Map<String, String> map = new HashMap<String, String>();
		for (int i = 0; i <= args.length; i++) {
			String key = (i > 0 ? args[i - 1] : null);
			String value = (i < args.length ? args[i] : null);
			if (key == null || key.startsWith("-")) {
				if (value != null && value.startsWith("-"))
					value = null;
				if (key != null || value != null)
					map.put(key, value);
			}
		}
		return map;
	}

}
