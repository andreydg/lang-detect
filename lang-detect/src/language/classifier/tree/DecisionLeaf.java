package language.classifier.tree;

import java.util.List;

/**
 * This represents a leaf in the Decision Tree, leaf can have either positive or
 * negative label. This class is also responsible for updating statistic on
 * correctness of classification of the available training examples
 * 
 * @author Andrey Gusev
 * 
 */
public class DecisionLeaf<T extends Comparable<T>, K> extends DecisionTreeEntity<T, K> {

	protected final double confidenceLevel;

	public DecisionLeaf(List<? extends DecisionTreeExample<T, K>> data, double confidenceLevel, K positiveLabel) {
		super(positiveLabel, data);
		this.confidenceLevel = confidenceLevel;
	}

	@Override
	protected String toString(int level) {

		StringBuilder sb = new StringBuilder(this.getSpaces(level + 1));
		sb.append("confidence = ");
		sb.append(confidenceLevel);
		sb.append("  (").append(this.allData.size()).append(")");
		sb.append("\n");
		return sb.toString();
	}

	/**
	 * @see DecisionTreeEntity return confidenceLevel
	 */
	@Override
	public double predictFor(DecisionTreeExample<T, K> example) {

		return this.confidenceLevel;

	}
}
