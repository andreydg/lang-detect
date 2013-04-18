package language.model;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import language.util.LanguageUtil;

/**
 * Represents language ngram model
 * 
 * @author Andrey Gusev
 */
public class NgramModel {

	private int BASE_TOP_NGRAMS = 50;

	private final int actualTopNGrams;

	private static DecimalFormat decimalFormat;
	private static final int scale = 3;
	private Double lengthNorm;

	private Map<String, Double> rawNgramFrequency;
	private TreeMap<NGram, Double> sortedNgrams;
	// not null when NgramModel represents specific language
	private Locale languageDefinition;

	public final Map<String, Set<String>> nGramCache = new HashMap<String, Set<String>>();

	private final int ngramSize;

	static {
		decimalFormat = new DecimalFormat();
		decimalFormat.setMinimumFractionDigits(scale);
		decimalFormat.setMaximumFractionDigits(scale);
	}

	public NgramModel(int ngramSize) {
		this(null, ngramSize);
	}

	public NgramModel(Locale languageDefinition, int ngramSize) {
		this.rawNgramFrequency = new HashMap<String, Double>(64);
		this.languageDefinition = languageDefinition;
		// if this is already normalized vector we don't have to compute length
		// norm again
		if (this.languageDefinition != null) {
			lengthNorm = 1.0;
		}
		this.ngramSize = ngramSize;
		// the longer the ngram = the more top ngrams we collect
		this.actualTopNGrams = Math.min(BASE_TOP_NGRAMS * this.ngramSize, 150);
	}

	public Set<String> getNgrams(String word) {
		return LanguageUtil.getNgrams(word, this.ngramSize);
	}
	
	public Set<String> getNgramsForWordCombination(String prevWord, String nextWord) {
		// minimum word combination size for ngram is 3
		// since there is word boundary character
		// and two characters from each word
		if (ngramSize >= 3) {
			return LanguageUtil.getNgramsForWordCombination(prevWord, nextWord, this.ngramSize);
		} else {
			return Collections.<String> emptySet();
		}
	}

	/**
	 * 
	 * This should be called to add ngrams for test document
	 * 
	 * @param nGram
	 *            - the ngram string
	 */
	public void addNgram(String nGram) {
		this.addNgram(nGram, 1.0);
	}

	/**
	 * 
	 * This should be called to add ngrams for test document
	 * 
	 * @param nGram
	 *            - the ngram string
	 * 
	 * @param value
	 *            - allows to adjust value for long words
	 */
	public void addNgram(String nGram, Double value) {
		if (this.sortedNgrams != null && this.lengthNorm != null) {
			throw new RuntimeException(
					"Can't add ngrams after lengthNorm and sortedNgrams have been formed in getngramsSortedByFrequency");
		}

		Double current = this.rawNgramFrequency.get(nGram);
		if (current == null) {
			this.rawNgramFrequency.put(nGram, value);
		} else {
			this.rawNgramFrequency.put(nGram, current + value);
		}
	}

	/**
	 * This should only be called to populate ngram model from normalized
	 * language model
	 * 
	 * @param nGram
	 *            - the ngram string
	 * @param value
	 *            - normalized frequency value
	 */
	public void addNormalizedNgram(String nGram, Double value) {
		if (this.lengthNorm == null || this.lengthNorm != 1.0 || this.languageDefinition == null) {
			throw new RuntimeException("Can't add noramlized ngrams if this is not language model");
		}

		if (this.sortedNgrams == null) {
			// used tree map to keep NGram sorted according to frequency
			this.sortedNgrams = new TreeMap<NGram, Double>();
		}

		this.sortedNgrams.put(new NGram(nGram, value), value);
	}

	private Map<NGram, Double> getNgramsSortedByFrequency() {
		if (this.sortedNgrams == null) {
			// used tree map to keep NGram sorted according to frequency
			this.sortedNgrams = new TreeMap<NGram, Double>();

			for (Map.Entry<String, Double> entry : this.rawNgramFrequency.entrySet()) {
				this.sortedNgrams.put(new NGram(entry.getKey(), entry.getValue()), entry.getValue());
			}
		}
		return sortedNgrams;
	}

	public double calculateCosineSimilarity(NgramModel anotherModel) {
		assert this.languageDefinition != null || anotherModel.languageDefinition != null : "one of the ngram models need to be language model";

		NgramModel languageModel = this.languageDefinition != null ? this : anotherModel;

		double lengthNormThis = this.getLengthNorm(this.actualTopNGrams);
		double lengthNormAnother = anotherModel.getLengthNorm(this.actualTopNGrams);
		if (lengthNormThis == 0 || lengthNormAnother == 0) {
			return 0;
		}

		double cosineSimilarity = 0.0;
		int counter = 0;
		for (NGram ngram : languageModel.getNgramsSortedByFrequency().keySet()) {
			if (anotherModel.rawNgramFrequency.containsKey(ngram.nGramValue)) {
				// note that we are looking up this up in raw map since sorted
				// map is a tree map we
				// uses comparator to lookup
				cosineSimilarity += ngram.frequency * anotherModel.rawNgramFrequency.get(ngram.nGramValue);
			}
			counter++;
			if (counter > this.actualTopNGrams) {
				break;
			}

		}

		return cosineSimilarity / (lengthNormThis * lengthNormAnother);
	}

	public static final String NGRAM_SEPARTOR = ":";

	/**
	 * returns euclidean distance normalized vector for this language model
	 */
	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder(128);
		double lengthNormThis = this.getLengthNorm(this.actualTopNGrams);
		int counter = 0;
		for (NGram ngram : getNgramsSortedByFrequency().keySet()) {
			sb.append(ngram.nGramValue).append(NGRAM_SEPARTOR).append(
					decimalFormat.format(ngram.frequency / lengthNormThis));
			sb.append("\n");
			counter++;
			if (counter > this.actualTopNGrams) {
				break;
			}

		}

		return sb.toString();
	}

	private double getLengthNorm(int maxNgrams) {
		if (this.lengthNorm != null) {
			return this.lengthNorm;
		}

		double lenghNorm = 0;
		int counter = 0;
		// will be order since sortedNgrams returns TreeMap
		for (NGram ngram : this.getNgramsSortedByFrequency().keySet()) {
			lenghNorm += Math.pow(ngram.frequency, 2);
			counter++;
			if (counter > maxNgrams) {
				break;
			}
		}

		this.lengthNorm = Math.sqrt(lenghNorm);
		return this.lengthNorm;
	}

	/**
	 * Reprsents a view of this ngram and frequency
	 * 
	 * @author agusev
	 * 
	 */
	private static class NGram implements Comparable<NGram> {

		private final String nGramValue;
		private final Double frequency;

		public NGram(String nGram, Double frequency) {
			this.nGramValue = nGram;
			this.frequency = frequency;
		}

		// we're doing reverse sorting so switch the order in compare to
		// operator
		public int compareTo(NGram anotherNGram) {
			int original = anotherNGram.frequency.compareTo(this.frequency);
			if (original == 0) {
				return anotherNGram.nGramValue.compareTo(this.nGramValue);
			} else {
				return original;
			}
		}

		@Override
		public String toString() {
			return this.nGramValue + ":" + this.frequency;
		}

		@Override
		public int hashCode() {
			return this.nGramValue.hashCode();
		}

		@Override
		public boolean equals(Object nGram) {
			if (!(nGram instanceof NGram)) {
				return false;
			}
			return this.nGramValue.equals(((NGram) nGram).nGramValue);
		}
	}
}
