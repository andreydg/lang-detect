package language.classifier.tree;

import java.util.List;

/**
 * T - comparable value for decision tree node K - positive label
 * 
 * Base abstract entity of Decision Tree, can be extended into leaf or node the
 * tree
 * 
 * @author Andrey Gusev
 * 
 */
public abstract class DecisionTreeEntity<T extends Comparable<T>, K> {

	protected final List<? extends DecisionTreeExample<T, K>> allData;
	protected final int numPositives;
	protected final int numNegatives;
	protected final K positiveLabel;

	public DecisionTreeEntity(K positiveLable, List<? extends DecisionTreeExample<T, K>> allData) {
		this.positiveLabel = positiveLable;
		this.allData = allData;
		this.numPositives = this.allData != null ? this.getNumberOfPositives(this.allData) : 0;
		this.numNegatives = this.allData != null ? this.allData.size() - numPositives : 0;
	}

	/**
	 * 
	 * @param level
	 *            - the level in the tree of this entity
	 * @return String to ensure correct spacing
	 */
	protected String getSpaces(int level) {

		StringBuilder sb = new StringBuilder();
		for (int ind = 0; ind < level; ind++) {
			sb.append("  ");
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param set
	 *            - array of the implementations of Example interface
	 * @return number of examples that were classified as positive
	 */
	protected int getNumberOfPositives(List<? extends DecisionTreeExample<T, K>> set) {

		int retVal = 0;
		for (DecisionTreeExample<T, K> example : set) {
			if (example.isPositive(this.positiveLabel)) {
				retVal++;
			}
		}
		return retVal;
	}

	/**
	 * 
	 * @param <T>
	 * @param value
	 *            value to determine whether this is in range
	 * @param lowerBound
	 * @param upperBound
	 * @return whether the value is within ranges (equal to lowerBound) if
	 *         lowerBound is null then this is lowest range so we just need to
	 *         have it less then upper bound if upperBound is null then this is
	 *         highest range so we just need to have it higher lowerBound
	 */
	public static <T> boolean isValueWithinRange(T value, Comparable<T> lowerBound, Comparable<T> upperBound) {
		assert lowerBound != null || upperBound != null;

		if (lowerBound == null) {
			return upperBound.compareTo(value) > 0;
		} else if (upperBound == null) {
			return lowerBound.compareTo(value) <= 0;
		} else {
			return upperBound.compareTo(value) > 0 && lowerBound.compareTo(value) <= 0;
		}
	}

	protected abstract String toString(int level);

	/**
	 * This will update return classification for the example according to
	 * existing tree
	 */
	public abstract double predictFor(DecisionTreeExample<T, K> example);
}
