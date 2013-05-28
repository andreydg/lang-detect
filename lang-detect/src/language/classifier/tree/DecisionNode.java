package language.classifier.tree;

import java.util.ArrayList;
import java.util.List;

import language.classifier.tree.DecisionTreeFeature.Range;

/**
 * Implementation of the node in the Decision Tree it uses two interfaces to
 * create the tree - Feature and Example implementation of these two classes
 * constitutes one algorithm to create the tree
 * 
 * @author Andrey Gusev
 * 
 */
public class DecisionNode<T extends Comparable<T>, K> extends DecisionTreeEntity<T, K> {

	protected DecisionTreeFeature<T>[] featureSet;
	protected int level;
	protected ArrayList<DecisionTreeEntity<T, K>> children;
	protected String label;
	protected DecisionTreeFeature<T> currentFeature;
	protected Comparable<T> lowValue;
	protected Comparable<T> highValue;

	// calculate at instantiation only once
	private double entropyAtNode;

	/**
	 * 
	 * @param data
	 *            - the data to process at this node
	 * @param featureSet
	 *            - features that can be used to split the data
	 * @param level
	 *            - level of the tree at which this node appears, note that root
	 *            is has the highest level value, while children have the lowest
	 * @param label
	 *            - a label that will be used to identify this node in toString
	 *            function
	 */
	public DecisionNode(List<? extends DecisionTreeExample<T, K>> data, DecisionTreeFeature<T>[] featureSet, int level,
			DecisionTreeFeature<T> currentFeature, Comparable<T> lowValue, Comparable<T> highValue, K positiveLabel) {
		super(positiveLabel);
		this.allData = data;
		this.level = level;
		this.featureSet = featureSet;
		this.numPositives = this.getNumberOfPositives(this.allData);
		this.numNegatives = this.allData.size() - this.numPositives;
		this.entropyAtNode = this.getEntropyForData(this.allData);
		this.children = new ArrayList<>();
		this.label = lowValue + " <= " + currentFeature + " <" + highValue;
		this.currentFeature = currentFeature;
		this.lowValue = lowValue;
		this.highValue = highValue;

		// if this is last level, or there are no positive
		// or negative examples or there are no features left
		// determine the label of the majority of
		// the samples and assign this class to leaf
		if (level == 0 || this.numNegatives == 0 || this.numPositives == 0 || this.featureSet.length == 0) {
			createLeaf();
		} else {
			// recurisve construction of the tree
			splitByMaximizingFeature();
		}

	}

	/**
	 * This will examine the data at the node to determine a feature in the list
	 * of features at this node that will maximize the information gain - see
	 * tools of this class for details of this calculation. Once the optimal
	 * feature to split data at this node is determine the node will be split
	 * according to the possible values of this feature and the children will be
	 * attached to this node.
	 * 
	 */

	private void splitByMaximizingFeature() {

		DecisionTreeFeature<T> maxFeature = getMaximizingFeature();

		// remove the maximizing feature
		@SuppressWarnings("unchecked")
		DecisionTreeFeature<T>[] newFeautureSet = new DecisionTreeFeature[featureSet.length - 1];
		int count = 0;
		for (DecisionTreeFeature<T> feature : featureSet) {
			if (!feature.equals(maxFeature)) {
				newFeautureSet[count] = feature;
				count++;
			}
		}

		for (Range<T> range : maxFeature.getRanges()) {
			ArrayList<DecisionTreeExample<T, K>> hasValue = this.getSubsetWithFeatureValue(maxFeature, range.lowBound,
					range.highBound);
			if (hasValue != null && hasValue.size() > 0) {
				children.add(new DecisionNode<>(hasValue, newFeautureSet, this.level - 1, maxFeature, range.lowBound,
						range.highBound, this.positiveLabel));
			}
		}
	}

	/**
	 * Will instantiate a leaf with either positive or negative label and assign
	 * all the data at the node to this leaf, this leaf will be attaced to the
	 * this node as a child. The label will be assigned according to the
	 * majority of the examples at the node
	 */
	private void createLeaf() {

		if (this.allData.size() == 0) {
			return;
		}
		// construct tree node at this place
		this.children.add(new DecisionLeaf<>(allData, ((double) this.numPositives)
				/ (this.numPositives + this.numNegatives), this.positiveLabel));
	}

	public ArrayList<DecisionTreeEntity<T, K>> getChildren() {

		return children;
	}

	/**
	 * @see DecisionTreeEntity
	 */
	@Override
	public double predictFor(DecisionTreeExample<T, K> example) {

		for (DecisionTreeEntity<T, K> decisionTreeEntity : children) {
			if (decisionTreeEntity instanceof DecisionNode) {
				DecisionNode<T, K> childNode = (DecisionNode<T, K>) decisionTreeEntity;
				if (example.hasValueForFeature(childNode.currentFeature, childNode.lowValue, childNode.highValue,
						this.positiveLabel)) {
					return childNode.predictFor(example);
				}
			} else if (decisionTreeEntity instanceof DecisionLeaf) {
				DecisionLeaf<T, K> childLeaf = (DecisionLeaf<T, K>) decisionTreeEntity;
				return childLeaf.predictFor(example);
			}
		}

		// in case the tree doesn't have a branch for the value of
		// feature that this example has, we will return no concrete prediction

		return 0.5;
	}

	@Override
	public String toString() {

		return toString(-1);
	}

	@Override
	protected String toString(int level) {

		StringBuilder sb = new StringBuilder();
		if (level >= 0) {
			sb.append(this.getSpaces(level));
			sb.append("if feature ");
			sb.append(this.label).append("\n");
		}
		for (DecisionTreeEntity<T, K> treeEntity : children) {
			sb.append(treeEntity.toString(level + 1));
		}
		return sb.toString();
	}

	/**
	 * Tools for dtermining the best attribute to split data
	 */

	/**
	 * 
	 * @param data
	 *            - an array of implementations of Example interface
	 * @return will return entropy of this data
	 */
	private double getEntropyForData(List<? extends DecisionTreeExample<T, K>> data) {

		int postives = this.getNumberOfPositives(data);
		return this.getEntropy(postives, data.size() - postives);
	}

	/**
	 * Implementation of entropy calculation
	 * 
	 * @param numberOfPostives
	 *            - number of positives examples
	 * @param numerberOfNegatives
	 *            - number of negative examples
	 * @return entropy of this data with positives and negative examples
	 */
	protected double getEntropy(int numberOfPostives, int numerberOfNegatives) {

		if (numberOfPostives == 0 || numerberOfNegatives == 0) {
			return 0;
		}

		double positives = numberOfPostives;
		double negatives = numerberOfNegatives;

		double total = positives + negatives;

		return -(positives / total) * (log2(positives / total)) - (negatives / total) * (log2(negatives / total));

	}

	/**
	 * 
	 * @param feature
	 *            - Implementation of the feature interface For a particular
	 *            feature data and entropy of the node will be used to calculate
	 *            information gain if the data would be split for all possible
	 *            values of this feature
	 * 
	 * @return information gain of this feature
	 */
	private double getInformationGain(DecisionTreeFeature<T> feature) {

		double gain = this.entropyAtNode;
		for (Range<T> range : feature.getRanges()) {
			// create array list for examples in data that have
			// value
			ArrayList<DecisionTreeExample<T, K>> hasValue = this.getSubsetWithFeatureValue(feature, range.lowBound,
					range.highBound);

			double Sv = hasValue.size();
			double S = allData.size();
			double ent = this.getEntropyForData(hasValue);
			double ratio = Sv / S;
			gain -= ratio * ent;

		}
		return gain;
	}

	/**
	 * 
	 * @param feature
	 *            - a Feature to test
	 * @param value
	 *            - value examined for the feature, should be implementation of
	 *            the Comparable interface
	 * @return ArrayList of implementation of examples in the data at the node
	 *         that for a feature tested have specified value
	 */
	private ArrayList<DecisionTreeExample<T, K>> getSubsetWithFeatureValue(DecisionTreeFeature<T> feature,
			Comparable<T> lowValue, Comparable<T> highValue) {

		ArrayList<DecisionTreeExample<T, K>> hasValue = new ArrayList<>();
		for (DecisionTreeExample<T, K> example : this.allData) {
			if (example.hasValueForFeature(feature, lowValue, highValue, this.positiveLabel)) {
				hasValue.add(example);
			}
		}
		return hasValue;
	}

	/**
	 * 
	 * @return for all available features available in this node a feature that
	 *         maximized information gain will be returned
	 */
	private DecisionTreeFeature<T> getMaximizingFeature() {

		double maxGain = 0;
		DecisionTreeFeature<T> maxFeature = featureSet[0];
		for (DecisionTreeFeature<T> feature : featureSet) {
			double currentGain = getInformationGain(feature);
			if (currentGain > maxGain) {
				maxGain = currentGain;
				maxFeature = feature;
			}
		}
		return maxFeature;
	}

	/**
	 * 
	 * @param input
	 *            a number to take log of
	 * @return log base 2 of the number
	 */
	private double log2(double input) {

		return Math.log(input) / Math.log(2.0);
	}
}
