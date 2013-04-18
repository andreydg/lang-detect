package language.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

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
	
	public static final String UTF_ENCODING = "UTF-8";

	private Map<Pair<Locale, Integer>, NgramModel> languageNgramModels;

	protected static final String MODEL_DIR = "ngramModel";
	public static final String TRAINING_TEST_DIR = "trainingAndTestSet";
	private static final String LOGISTIC_CLASSFIER_DIR = "logisticClassifier";

	private static final Double MIN_SCORE = 0.05;
	
	private static final int WORD_LENGTH_BOUNDARY = 1;

	private static final int DEFAULT_DECISION_TREE_BAGS = 10;

	private static final ClassificationAlgorithm DEFAULT_CLASSIFIER = ClassificationAlgorithm.LINEAR_WEIGHTS;

	protected final Integer[] ngramSet;

	private final Set<Integer> populatedModels = new HashSet<Integer>();

	protected static DecimalFormat decimalFormat;
	private static final int scale = 3;

	// can be trained by calling trainDecisionTree
	private static Map<Locale, BaggedDecisionTreeClassifier<Double, Locale, LanguageDocumentExample>> trainedDecisionTrees;
	private static Map<Locale, LogisticRegressionClassifier<Locale, LanguageDocumentExample>> trainedLogisticClassifiers;

	private static Set<Locale> hasTrainingExample;
	protected final File basePath;

	protected static final Locale[] locales;
	protected static final Map<String, Locale> localeMap = new HashMap<String, Locale>();

	static {
		decimalFormat = new DecimalFormat();
		decimalFormat.setMinimumFractionDigits(scale);
		decimalFormat.setMaximumFractionDigits(scale);
		// EFIGS languages + portugese
		locales = new Locale[] { 
				Locale.ENGLISH, 
				Locale.FRENCH, 
				Locale.ITALIAN, 
				Locale.GERMAN, 
				new Locale("es"),
				new Locale("pt") };
		for (Locale locale : locales) {
			localeMap.put(locale.toString(), locale);
		}
	}

	public NgramLanguageDetector(File basePath) {
		// if you change this set you need to change set of enums for features
		// in NgramLanguageModelFeature
		NgramLanguageModelFeature[] values = NgramLanguageModelFeature.values();
		List<Integer> ngrams = new ArrayList<Integer>();
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
//		String prevWord = null;
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
//			if (prevWord != null) {
//				for (String nGram : model.getNgramsForWordCombination(prevWord,
//						word)) {
//					model.addNgram(nGram, 1.0);
//				}
//			}
//			
//			prevWord = word;
		}
		return model;
	}

	public final Integer[] getNgramSet() {
		return this.ngramSet;
	}

	/*
	 * Get list of training example to train classifier
	 */
	protected final List<LanguageDocumentExample> getTrainingExamples(boolean addLinearWeightFeature) throws IOException {

		String locationBase = basePath.getAbsolutePath() + File.separator + "languagemodels" + File.separator;

		List<LanguageDocumentExample> examples = new ArrayList<LanguageDocumentExample>();
		hasTrainingExample = new HashSet<Locale>();
		for (Locale positiveLocale : locales) {

			String testSetLocation = locationBase + TRAINING_TEST_DIR + File.separator + positiveLocale.toString()
					+ "_training";

			File file = new File(testSetLocation);
			if (!file.exists()) {
				continue;
			}

			hasTrainingExample.add(positiveLocale);

			// need to read in UTF-8
			Reader fr = new InputStreamReader(new FileInputStream(file), UTF_ENCODING);
			BufferedReader br = new BufferedReader(fr);
			String s;

			try {
				while ((s = br.readLine()) != null) {
					LanguageDocumentExample trainingExample = getExample(s, addLinearWeightFeature, positiveLocale);
					examples.add(trainingExample);
				}
			}

			finally {

				fr.close();
				br.close();
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
			Map<Locale, Double> linearCombination = new HashMap<Locale, Double>();
			List<Entry<Locale, Double>> list = this.detectLanguageWithLinearWeights(s, false);
			for (Entry<Locale, Double> entry : list) {
				linearCombination.put(entry.getKey(), entry.getValue());
			}

			example.addFeatureValue(NgramLanguageModelFeature.LINEAR_COMBINATION, linearCombination);
		}

		return example;
	}

	/*
	 * Training logistic classifier
	 */
	protected final void trainLogisiticClassifier() throws IOException {

		List<LanguageDocumentExample> examples = getTrainingExamples(true);

		trainedLogisticClassifiers = new HashMap<Locale, LogisticRegressionClassifier<Locale, LanguageDocumentExample>>();

		// since training takes a long time we want to train in multiple threads
		// classifier can not be trained in multiple threads
		final int numThreads = 2;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);

		CompletionService<LogisticRegressionClassifier<Locale, LanguageDocumentExample>> completionService = new ExecutorCompletionService<LogisticRegressionClassifier<Locale, LanguageDocumentExample>>(
				executor);

		int numSubmitted = 0;
		for (Locale positiveLocale : locales) {
			if (!hasTrainingExample.contains(positiveLocale)) {
				continue;
			}
			LanguageDocumentExample someExample = examples.get(0);
			LogisticRegressionClassifier<Locale, LanguageDocumentExample> localeClassifier = new LogisticRegressionClassifier<Locale, LanguageDocumentExample>(
					someExample.getFeatureValues(positiveLocale).size(), positiveLocale);

			// submit to read or train classifier
			completionService.submit(new LogisticClassifierTrainer(localeClassifier, examples,
					getLogisitcClassifierFile(positiveLocale)));
			numSubmitted++;
			trainedLogisticClassifiers.put(positiveLocale, localeClassifier);
		}

		for (int ind = 0; ind < numSubmitted; ind++) {
			try {
				LogisticRegressionClassifier<Locale, LanguageDocumentExample> localeClassifier = completionService
						.take().get();
				trainedLogisticClassifiers.put(localeClassifier.getPositiveLabel(), localeClassifier);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
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
		String locationBase = configDir + File.separator + "languagemodels" + File.separator;
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
	protected final void trainDecisionTree(int numBags) throws IOException {

		List<LanguageDocumentExample> examples = getTrainingExamples(true);

		trainedDecisionTrees = new HashMap<Locale, BaggedDecisionTreeClassifier<Double, Locale, LanguageDocumentExample>>();

		for (Locale positiveLocale : locales) {
			if (!hasTrainingExample.contains(positiveLocale)) {
				continue;
			}
			BaggedDecisionTreeClassifier<Double, Locale, LanguageDocumentExample> localeBag = new BaggedDecisionTreeClassifier<Double, Locale, LanguageDocumentExample>(
					numBags, positiveLocale, NgramLanguageModelFeature.values());
			localeBag.train(examples);
			trainedDecisionTrees.put(positiveLocale, localeBag);
		}
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

		List<Locale> sameConfidenceLocale = new ArrayList<Locale>();
		for (Locale locale : locales) {
			if (!hasTrainingExample.contains(locale)) {
				continue;
			}

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
			return sameConfidenceLocale.get((int) (Math.random() * sameConfidenceLocale.size()));
		}
		return predictedLocale;
	}

	/**
	 * Will detect most likely language with with bagged decision tree
	 * classifier
	 */
	public final Locale detectLanguageWithDecisionTree(String text) throws IOException {

		if (trainedDecisionTrees == null) {
			trainDecisionTree(DEFAULT_DECISION_TREE_BAGS);
		}
		return detectLanguageClassifier(text, trainedDecisionTrees, true);
	}

	/**
	 * Will detect most likely language with logistic classifier
	 */
	public final Locale detectLanguageWithLogisiticClassifier(String text) throws IOException {

		if (trainedLogisticClassifiers == null) {
			trainLogisiticClassifier();
		}
		return detectLanguageClassifier(text, trainedLogisticClassifiers, true);
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
		List<Entry<Locale, Double>> list = this.detectLanguageWithLinearWeights(text, true);
		if (list != null && list.size() > 0) {
			return list.get(0).getKey();
		} else {
			return null;
		}
	}

	/**
	 * returns ordered list of languages that are most similar to given text,
	 * using all nGram sizes specified for language detector
	 */
	public final List<Entry<Locale, Double>> detectLanguageWithLinearWeights(String text, boolean ignoreLowScores)
			throws IOException {

		List<Map<Locale, Double>> listOfRawCosineSimilaties = new ArrayList<Map<Locale, Double>>(ngramSet.length);

		// first get all the raw cosine similarities
		for (int nGramSize : ngramSet) {
			listOfRawCosineSimilaties.add(getRawCosineSimilarities(text, nGramSize, true));
		}

		Map<Locale, Double> retValue = new HashMap<Locale, Double>();

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

	private List<Entry<Locale, Double>> getValueSortedDescendingEntries(Map<Locale, Double> map) {
		List<Entry<Locale, Double>> list = new ArrayList<Entry<Locale, Double>>(map.entrySet());
		Collections.sort(list, new Comparator<Entry<Locale, Double>>() {

			public int compare(Entry<Locale, Double> a, Entry<Locale, Double> b) {
				// since since we want discening list multiply by -1
				return a.getValue().compareTo(b.getValue()) * -1;
			}

		});

		return list;
	}

	/**
	 * return unsorted locales and their consine similarity to given text in
	 * nGram space
	 */
	public final Map<Locale, Double> getRawCosineSimilarities(String text, int nGramSize, boolean addNgramWeight)
			throws IOException {

		text = text.trim();

		NgramModel textModel = text.length() >= nGramSize ? this.getNgramModelForText(text, new NgramModel(nGramSize), true)
				: null;

		Map<Locale, Double> retVal = new HashMap<Locale, Double>();

		populateLanguageModels(nGramSize);

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

	private void populateLanguageModels(int nGramSize) throws IOException {
		// already populated
		if (populatedModels.contains(nGramSize)) {
			return;
		}

		if (this.languageNgramModels == null) {
			this.languageNgramModels = new HashMap<Pair<Locale, Integer>, NgramModel>(32);
		}

		String configDir = this.basePath.getAbsolutePath();

		String locationBase = configDir + File.separator + "languagemodels" + File.separator;

		// populate the models and cache them
		for (Locale locale : locales) {
			Pair<Locale, Integer> key = new Pair<Locale, Integer>(locale, nGramSize);
			if (this.languageNgramModels.containsKey(key)) {
				continue;
			}

			String modelLocation = locationBase + MODEL_DIR + File.separator + locale.toString() + "_" + nGramSize;
			File modelFile = new File(modelLocation);
			if (!modelFile.exists()) {
				log.severe("Could not load model from: " + modelLocation);
				continue;
			}
			this.languageNgramModels.put(key, readModel(modelFile, locale, nGramSize));
		}

		populatedModels.add(nGramSize);
	}

	private NgramModel readModel(File modeFile, Locale locale, int nSize) throws IOException {
		// need to read in UTF-8
		Reader fr = new InputStreamReader(new FileInputStream(modeFile), UTF_ENCODING);
		BufferedReader br = new BufferedReader(fr);
		String s;
		NgramModel languageModel = new NgramModel(locale, nSize);
		while ((s = br.readLine()) != null) {
			String[] parts = s.split(NgramModel.NGRAM_SEPARTOR);
			assert parts != null && parts.length == 2;
			languageModel.addNormalizedNgram(parts[0], Double.valueOf(parts[1]));
		}
		fr.close();
		br.close();

		return languageModel;
	}

	public static Locale[] getLocales() {
		return locales;
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
