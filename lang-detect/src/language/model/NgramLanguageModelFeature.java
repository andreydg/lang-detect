package language.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import language.classifier.tree.DecisionTreeFeature;

/**
 * Represents a set of features for ngram model
 * 
 * @author Andrey Gusev
 * 
 */
public enum NgramLanguageModelFeature implements DecisionTreeFeature<Double> {

	LINEAR_COMBINATION(0, true), _1GRAM(1), _2GRAM(2), _3GRAM(3), _4GRAM(4), _5GRAM(5), _6GRAM(6);

	private final int nGramSize;
	private final boolean nonStandard;
	private static final List<Double> possibleValues = new ArrayList<>();
	private static final List<Range<Double>> ranges = new ArrayList<>();

	private static final double RANGE_START = 0.00;
	private static final double RANGE_INCREMENT = 0.10;
	private static final double RANGE_END = 1.00;

	private NgramLanguageModelFeature(int nGramSize) {
		this(nGramSize, false);
	}

	private NgramLanguageModelFeature(int nGramSize, boolean nonStandard) {
		this.nGramSize = nGramSize;
		this.nonStandard = nonStandard;
	}

	private static Map<Integer, NgramLanguageModelFeature> enumMapping;
	static {
		enumMapping = new HashMap<>();

		for (NgramLanguageModelFeature feature : values()) {
			enumMapping.put(feature.nGramSize, feature);
		}
		enumMapping = Collections.unmodifiableMap(enumMapping);

		// instead of using constant intervals we will use
		// more values that are closer max value since we would like to split on
		// those values
		for (double value = RANGE_START; value < RANGE_END; value += RANGE_INCREMENT) {
			possibleValues.add(value);
		}

		// value ranges
		ranges.add(new Range<>(null, possibleValues.get(0)));
		for (int ind = 0; ind < possibleValues.size() - 1; ind++) {
			ranges.add(new Range<>(possibleValues.get(ind), possibleValues.get(ind + 1)));
		}
		ranges.add(new Range<>(possibleValues.get(possibleValues.size() - 1), null));
	}

	public List<? extends Comparable<Double>> getPossibleValues() {
		return possibleValues;
	}

	public int getNGramSize() {
		return this.nGramSize;
	}

	public boolean isNonStandard() {
		return this.nonStandard;
	}

	public List<Range<Double>> getRanges() {
		return ranges;
	}

	public static NgramLanguageModelFeature getEnumByValue(int value) {
		return enumMapping.get(value);
	}

}
