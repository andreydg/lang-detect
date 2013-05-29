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

	private static final List<Double> POSSIBLE_VALUES = new ArrayList<>();
	private static final List<Range<Double>> RANGES = new ArrayList<>();

	private static final double RANGE_START = 0.00;
	private static final double RANGE_INCREMENT = 0.10;
	private static final double RANGE_END = 1.00;
	
	private final int nGramSize;
	private final boolean nonStandard;

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
			POSSIBLE_VALUES.add(value);
		}

		// value ranges
		RANGES.add(new Range<>(null, POSSIBLE_VALUES.get(0)));
		for (int ind = 0; ind < POSSIBLE_VALUES.size() - 1; ind++) {
			RANGES.add(new Range<>(POSSIBLE_VALUES.get(ind), POSSIBLE_VALUES.get(ind + 1)));
		}
		RANGES.add(new Range<>(POSSIBLE_VALUES.get(POSSIBLE_VALUES.size() - 1), null));
	}

	public List<? extends Comparable<Double>> getPossibleValues() {
		return POSSIBLE_VALUES;
	}

	public int getNGramSize() {
		return this.nGramSize;
	}

	public boolean isNonStandard() {
		return this.nonStandard;
	}

	public List<Range<Double>> getRanges() {
		return RANGES;
	}

	public static NgramLanguageModelFeature getEnumByValue(int value) {
		return enumMapping.get(value);
	}

}
