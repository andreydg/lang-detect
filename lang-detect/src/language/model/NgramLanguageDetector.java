package language.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.annotation.concurrent.GuardedBy;

import language.classifier.BaggedDecisionTreeClassifier;
import language.classifier.Classifier;
import language.classifier.LogisticRegressionClassifier;
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
public class NgramLanguageDetector implements LanguageDetector {

	private static final Logger log = Logger.getLogger(NgramLanguageDetector.class.getName());
	private static final Random rnd = new Random(1);

	// path constants
	public static String BASE_MODEL_DIR = "languagemodels";
	public static final String NGRAM_MODEL_DIR = "ngramModel";
	public static final String TRAINING_TEST_DIR = "trainingAndTestSet";
	public static final String LOGISTIC_CLASSFIER_DIR = "logisticClassifier";
	
	public static final String UTF8 = "UTF-8";

	// classifier constants
	private static final Double MIN_SCORE = 0.05;
	private static final int WORD_LENGTH_BOUNDARY = 1;
	private static final int DEFAULT_DECISION_TREE_BAGS = 10;
	private static final ClassificationAlgorithm DEFAULT_CLASSIFIER = ClassificationAlgorithm.LINEAR_WEIGHTS;

	// locales
	protected static final Locale[] LOCALES;
	protected static final Map<String, Locale> LOCALE_MAP;

	// format
	protected static DecimalFormat decimalFormat;
	private static final int scale = 3;
	
	private final static Lock DF = new ReentrantLock();
	private final static Lock LC = new ReentrantLock();

	// cache of trained classifiers
	@GuardedBy("DF")
	private static volatile Map<Locale, Classifier<Double, Locale, LanguageDocumentExample>> DECISION_TREES;
	@GuardedBy("LC")
	private static volatile Map<Locale, Classifier<Double, Locale, LanguageDocumentExample>> LOGISITIC_CLASSIFIERS;

	// main ngram models
	private final Map<Pair<Locale, Integer>, NgramModel> languageNgramModels;
	protected final Integer[] ngramSet;

	protected final File basePath;

	static {
		decimalFormat = new DecimalFormat();
		decimalFormat.setMinimumFractionDigits(scale);
		decimalFormat.setMaximumFractionDigits(scale);
		// EFIGS languages + portuguese
		LOCALES = new Locale[] { Locale.ENGLISH, Locale.FRENCH, Locale.ITALIAN, Locale.GERMAN, new Locale("es"),
				new Locale("pt") };
		
		Map<String, Locale> tempMap = new HashMap<>();
		for (Locale locale : LOCALES) {
			tempMap.put(locale.toString(), locale);
		}
		LOCALE_MAP = Collections.unmodifiableMap(tempMap);
	}

	public NgramLanguageDetector(File basePath) {

		// if you change this set you need to change set of enums for features
		// in NgramLanguageModelFeature
		NgramLanguageModelFeature[] values = NgramLanguageModelFeature.values();
		List<Integer> ngrams = new ArrayList<>();
		for (NgramLanguageModelFeature feature : values) {
			// skip non-standard features
			if (feature.isNonStandard()) {
				continue;
			}
			ngrams.add(feature.getNGramSize());
		}

		this.ngramSet = new Integer[ngrams.size()];
		this.basePath = basePath;
		ngrams.toArray(this.ngramSet);

		// init all the models
		this.languageNgramModels = Collections.unmodifiableMap(populateLanguageModels());

	}

	public final void logQuery(String q) {
		if (q != null && q.length() > 0) {
			log.info("Q:[" + q + "]");
		}
	}

	protected final NgramModel getNgramModelForText(String text, NgramModel model, boolean adjustValue) {

		if (text == null || text.length() == 0) {
			return model;
		}
		// String prevWord = null;
		for (String word : LanguageUtil.tokenize(text, 1)) {
			int wordLength = word.length();
			// the value of ngram is not simply its count but is adjusted
			// for longer words this way short language representative words
			// have larger effect, when building ngram models over large
			// text corpus we do not adjust the value
			// the value is only adjusted for user input text
			double ngramValue = (!adjustValue || wordLength <= WORD_LENGTH_BOUNDARY) ? 1.0
					: ((double) WORD_LENGTH_BOUNDARY) / ((double) wordLength);
			for (String nGram : model.getNgrams(word)) {
				// only adjust ngram values for text input
				// use original counts for building language model for text
				model.addNgram(nGram, ngramValue);
			}

			// also track ngrams which capture transitions between words
			// if (prevWord != null) {
			// for (String nGram : model.getNgramsForWordCombination(prevWord,
			// word)) {
			// model.addNgram(nGram, 1.0);
			// }
			// }
			//
			// prevWord = word;
		}
		return model;
	}

	public final Integer[] getNgramSet() {
		return this.ngramSet;
	}

	/*
	 * Get list of training example to train classifier
	 */
	protected final List<LanguageDocumentExample> getTrainingExamples(boolean addLinearWeightFeature)
			throws IOException {

		String locationBase = basePath.getAbsolutePath() + File.separator + BASE_MODEL_DIR + File.separator;

		List<LanguageDocumentExample> examples = new ArrayList<>();
		for (Locale positiveLocale : LOCALES) {

			String testSetLocation = locationBase + TRAINING_TEST_DIR + File.separator + positiveLocale.toString()
					+ "_training";

			File file = new File(testSetLocation);
			if (!file.exists()) {
				continue;
			}

			// need to read in UTF-8
			String s;
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF8))) {
				while ((s = br.readLine()) != null) {
					LanguageDocumentExample trainingExample = getExample(s, addLinearWeightFeature, positiveLocale);
					examples.add(trainingExample);
				}
			}
		}

		return examples;
	}

	/*
	 * get Language document example
	 */
	protected final LanguageDocumentExample getExample(String s, boolean addLinearWeightFeature, Locale positiveLocale)
			throws IOException {
		LanguageDocumentExample example = new LanguageDocumentExample(positiveLocale);
		for (int nGram : ngramSet) {
			// calculate all cosine similarities for each language
			Map<Locale, Double> rawSimilarities = getRawCosineSimilarities(s, nGram, true);
			example.addFeatureValue(NgramLanguageModelFeature.getEnumByValue(nGram), rawSimilarities);
		}
		if (addLinearWeightFeature) {
			// add special linear combination feature
			Map<Locale, Double> linearCombination = new HashMap<>();
			for (Entry<Locale, Double> entry : this.detectLanguageWithLinearWeights(s, false)) {
				linearCombination.put(entry.getKey(), entry.getValue());
			}

			example.addFeatureValue(NgramLanguageModelFeature.LINEAR_COMBINATION, linearCombination);
		}

		return example;
	}

	/*
	 * Training logistic classifier
	 */
	protected final Map<Locale, Classifier<Double, Locale, LanguageDocumentExample>> trainLogisiticClassifier()
			throws IOException {

		List<LanguageDocumentExample> examples = getTrainingExamples(true);

		Map<Locale, Classifier<Double, Locale, LanguageDocumentExample>> retVal = new HashMap<>();

		// since training takes a long time we want to train in multiple threads
		// classifier can not be trained in multiple threads
		final int numThreads = 2;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);

		CompletionService<LogisticRegressionClassifier<Locale, LanguageDocumentExample>> completionService = new ExecutorCompletionService<>(
				executor);

		int numSubmitted = 0;
		for (Locale positiveLocale : LOCALES) {
			LanguageDocumentExample someExample = examples.get(0);
			LogisticRegressionClassifier<Locale, LanguageDocumentExample> localeClassifier = new LogisticRegressionClassifier<>(
					someExample.getFeatureValues(positiveLocale).size(), positiveLocale);

			// submit to read or train classifier
			completionService.submit(new LogisticClassifierTrainer(localeClassifier, examples,
					getLogisitcClassifierFile(positiveLocale)));
			numSubmitted++;
			retVal.put(positiveLocale, localeClassifier);
		}

		for (int ind = 0; ind < numSubmitted; ind++) {
			try {
				LogisticRegressionClassifier<Locale, LanguageDocumentExample> localeClassifier = completionService
						.take().get();
				retVal.put(localeClassifier.getPositiveLabel(), localeClassifier);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		return retVal;
	}

	protected static class LogisticClassifierTrainer implements
			Callable<LogisticRegressionClassifier<Locale, LanguageDocumentExample>> {

		private final LogisticRegressionClassifier<Locale, LanguageDocumentExample> localeClassifier;
		private final List<LanguageDocumentExample> examples;
		private final File classifierFile;

		public LogisticClassifierTrainer(
				LogisticRegressionClassifier<Locale, LanguageDocumentExample> localeClassifier,
				List<LanguageDocumentExample> examples, File classifierFile) {
			this.localeClassifier = localeClassifier;
			this.examples = examples;
			this.classifierFile = classifierFile;
		}

		public LogisticRegressionClassifier<Locale, LanguageDocumentExample> call() throws IOException {
			if (!localeClassifier.readFromFile(classifierFile)) {
				localeClassifier.train(examples);
				localeClassifier.writeToFile(classifierFile);
			}
			return localeClassifier;
		}
	}

	protected final File getLogisitcClassifierFile(Locale locale) {

		String configDir = basePath.getAbsolutePath();
		String locationBase = configDir + File.separator + BASE_MODEL_DIR + File.separator;
		String classifierDir = locationBase + LOGISTIC_CLASSFIER_DIR + File.separator;
		File classifierDirFile = new File(classifierDir);
		if (!classifierDirFile.exists()) {
			classifierDirFile.mkdir();
		}

		String logisiticClassifierLocaltion = classifierDir + locale.toString();
		return new File(logisiticClassifierLocaltion);
	}

	/*
	 * Training decision tree
	 */
	protected final Map<Locale, Classifier<Double, Locale, LanguageDocumentExample>> trainDecisionTree(int numBags)
			throws IOException {

		List<LanguageDocumentExample> examples = getTrainingExamples(true);

		Map<Locale, Classifier<Double, Locale, LanguageDocumentExample>> retVal = new HashMap<>();

		for (Locale positiveLocale : LOCALES) {
			BaggedDecisionTreeClassifier<Double, Locale, LanguageDocumentExample> localeBag = new BaggedDecisionTreeClassifier<>(
					numBags, positiveLocale, NgramLanguageModelFeature.values());
			localeBag.train(examples);
			retVal.put(positiveLocale, localeBag);
		}

		return retVal;
	}

	/**
	 * 
	 * @param text
	 *            - the text that needs to classified
	 * @param classifiers
	 *            - the map of one vs. many classifier which will be used for
	 *            classification
	 * @return Locale of most likely language
	 * @throws IOException
	 */
	protected final Locale detectLanguageClassifier(String text,
			Map<Locale, ? extends Classifier<Double, Locale, LanguageDocumentExample>> classifiers,
			boolean addLinearWeightFeature) throws IOException {
		// populate example with feature values
		LanguageDocumentExample example = getExample(text, addLinearWeightFeature, null);

		// now pick highest classifier
		Locale predictedLocale = null;
		double highestConfidence = 0;

		List<Locale> sameConfidenceLocale = new ArrayList<>();
		for (Locale locale : LOCALES) {

			double confidenceLevel = classifiers.get(locale).getConfidenceLevel(example);
			if (confidenceLevel > highestConfidence) {
				highestConfidence = confidenceLevel;
				predictedLocale = locale;
				sameConfidenceLocale.clear();
				sameConfidenceLocale.add(locale);
			} else if (Double.compare(confidenceLevel, highestConfidence) == 0) {
				sameConfidenceLocale.add(locale);
				predictedLocale = null;
			}
		}

		// we have matching highestConfidences return random one
		if (predictedLocale == null) {
			return sameConfidenceLocale.get(rnd.nextInt(sameConfidenceLocale.size()));
		}
		return predictedLocale;
	}

	/**
	 * Will detect most likely language with with bagged decision tree
	 * classifier
	 */
	private final Locale detectLanguageWithDecisionTree(String text) throws IOException {

		// lazy init
		if (DECISION_TREES == null) {
			DF.lock();
			try {
				if (DECISION_TREES != null) {
					DECISION_TREES = Collections.unmodifiableMap(trainDecisionTree(DEFAULT_DECISION_TREE_BAGS));
				}
			} finally {
				DF.unlock();
			}
		}
		return detectLanguageClassifier(text, DECISION_TREES, true);
	}

	/**
	 * Will detect most likely language with logistic classifier
	 */
	private final Locale detectLanguageWithLogisiticClassifier(String text) throws IOException {

		// lazy init
		if (LOGISITIC_CLASSIFIERS == null) {
			LC.lock();
			try {
				if (LOGISITIC_CLASSIFIERS != null) {
					LOGISITIC_CLASSIFIERS = Collections.unmodifiableMap(trainLogisiticClassifier());
				}
			}finally{
				LC.unlock();
			}
		}
		return detectLanguageClassifier(text, LOGISITIC_CLASSIFIERS, true);
	}

	public final Locale getMostLikelyLanguage(String text) throws IOException {
		return getMostLikelyLanguage(text, DEFAULT_CLASSIFIER);
	}

	public final Locale getMostLikelyLanguage(String text, ClassificationAlgorithm algorithmToUse) throws IOException {
		Locale retVal = null;
		switch (algorithmToUse) {
		case BAGGED_DECISION_TREE:
			retVal = this.detectLanguageWithDecisionTree(text);
			break;
		case LINEAR_WEIGHTS:
			retVal = this.getLanguageWithLinerWeights(text);
			break;
		case LOGISTIC_CLASSIFIER:
			retVal = this.detectLanguageWithLogisiticClassifier(text);
			break;
		}
		return retVal;
	}

	/**
	 * @param text
	 *            - text for which we will detect language
	 * @return most likely language
	 */
	public final Locale getLanguageWithLinerWeights(String text) throws IOException {
		SortedSet<Entry<Locale, Double>> set = this.detectLanguageWithLinearWeights(text, true);
		if (set != null && set.size() > 0) {
			return set.first().getKey();
		} else {
			return null;
		}
	}

	/**
	 * returns ordered set of languages that are most similar to given text,
	 * using all nGram sizes specified for language detector
	 */
	public final SortedSet<Entry<Locale, Double>> detectLanguageWithLinearWeights(String text, boolean ignoreLowScores)
			throws IOException {

		List<Map<Locale, Double>> listOfRawCosineSimilaties = new ArrayList<>(ngramSet.length);

		// first get all the raw cosine similarities
		for (int nGramSize : ngramSet) {
			listOfRawCosineSimilaties.add(getRawCosineSimilarities(text, nGramSize, true));
		}

		Map<Locale, Double> retValue = new HashMap<>();

		int numOfModels = listOfRawCosineSimilaties.size();

		// for all raw cosine similarities create overall locale score
		// that combines positional information in each return list of results
		// TODO: maybe adding original scores will reflect matches better
		for (Map<Locale, Double> rawCosineSimilarity : listOfRawCosineSimilaties) {
			int counter = rawCosineSimilarity.size();
			double maxScore = 0;

			for (Entry<Locale, Double> entry : getValueSortedDescendingEntries(rawCosineSimilarity)) {

				Double value = entry.getValue();
				if (ignoreLowScores && value == 0) {
					continue;
				}
				if (counter == rawCosineSimilarity.size()) {
					maxScore = value;
				}

				// ignore the scores that are very low
				if (ignoreLowScores && value < MIN_SCORE) {
					continue;
				}

				// normalize it so that top score is 1.00
				Double currentValue = retValue.get(entry.getKey());
				if (currentValue == null) {
					currentValue = value > 0 ? (value / (maxScore * numOfModels)) : 0;
				} else {
					currentValue += value > 0 ? (value / (maxScore * numOfModels)) : 0;
				}
				retValue.put(entry.getKey(), currentValue);
				counter--;
			}
		}

		return getValueSortedDescendingEntries(retValue);
	}

	private SortedSet<Entry<Locale, Double>> getValueSortedDescendingEntries(Map<Locale, Double> map) {
		SortedSet<Entry<Locale, Double>> retVal = new TreeSet<>(new Comparator<Entry<Locale, Double>>() {

			public int compare(Entry<Locale, Double> a, Entry<Locale, Double> b) {
				// since since we want discening list multiply by -1
				return a.getValue().compareTo(b.getValue()) * -1;
			}

		});
		
		retVal.addAll(map.entrySet());

		return retVal;
	}

	/**
	 * return unsorted locales and their consine similarity to given text in
	 * nGram space
	 */
	public final Map<Locale, Double> getRawCosineSimilarities(String text, int nGramSize, boolean addNgramWeight)
			throws IOException {

		text = text.trim();

		NgramModel textModel = text.length() >= nGramSize ? this.getNgramModelForText(text, new NgramModel(nGramSize),
				true) : null;

		Map<Locale, Double> retVal = new HashMap<>();

		for (Map.Entry<Pair<Locale, Integer>, NgramModel> model : this.languageNgramModels.entrySet()) {
			// skip models that have different ngram size
			if (model.getKey().getSecond() != nGramSize) {
				continue;
			}
			// calculate cosine similarity
			double cosineSimilarity = textModel != null ? model.getValue().calculateCosineSimilarity(textModel) : 0.00;
			if (addNgramWeight) {
				cosineSimilarity *= nGramSize;
			}
			retVal.put(model.getKey().getFirst(), cosineSimilarity);
		}

		return retVal;
	}

	private Map<Pair<Locale, Integer>, NgramModel> populateLanguageModels() {

		Map<Pair<Locale, Integer>, NgramModel> retVal = new HashMap<>(32);

		String configDir = this.basePath.getAbsolutePath();

		String locationBase = configDir + File.separator + BASE_MODEL_DIR + File.separator;

		for (Integer nGramSize : ngramSet) {

			// populate the models and cache them
			for (Locale locale : LOCALES) {
				Pair<Locale, Integer> key = new Pair<>(locale, nGramSize);

				String modelLocation = locationBase + NGRAM_MODEL_DIR + File.separator + locale.toString() + "_"
						+ nGramSize;
				File modelFile = new File(modelLocation);
				if (!modelFile.exists()) {
					log.severe("Could not load model from: " + modelLocation);
					continue;
				}
				try {
					retVal.put(key, readModel(modelFile, locale, nGramSize));
				} catch (IOException e) {
					throw new RuntimeException("Failed to read model with key: " + key, e);
				}
			}

		}

		return retVal;
	}

	private NgramModel readModel(File modeFile, Locale locale, int nSize) throws IOException {

		NgramModel languageModel = new NgramModel(locale, nSize);

		// need to read in UTF-8
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(modeFile), UTF8))) {
			String s;
			while ((s = br.readLine()) != null) {
				String[] parts = s.split(NgramModel.NGRAM_SEPARTOR);
				assert parts != null && parts.length == 2;
				languageModel.addNormalizedNgram(parts[0], Double.valueOf(parts[1]));
			}
		}

		return languageModel;
	}

	public static Locale[] getLocales() {
		return LOCALES;
	}

	public final File getBasePath() {
		return basePath;
	}

	public static enum ClassificationAlgorithm {
		LINEAR_WEIGHTS, BAGGED_DECISION_TREE, LOGISTIC_CLASSIFIER;
	}

	public static enum BoundaryDetectionAlgorithm {
		ONE_WORD, TWO_WORD, THREE_WORD, BASE_BIGRAM, TWO_WORD_BIGRAM, THREE_WORD_BIGRAM, FOUR_WORD_BIGRAM, FIVE_WORD_BIGRAM, SIX_WORD_BIGRAM, FIVE_WORD_NESTED;
	}
}
