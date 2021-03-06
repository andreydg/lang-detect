package language.classifier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import language.classifier.tree.DecisionNode;
import language.classifier.tree.DecisionTreeExample;
import language.classifier.tree.DecisionTreeFeature;

/**
 * Implements machine learning BaggedDecisionTree classifier algorithm. 
 * 
 * @author Andrey Gusev
 */
public class BaggedDecisionTreeClassifier<T extends Comparable<T>, K, Z extends DecisionTreeExample<T, K>> implements
		Classifier<T, K, Z> {
	
	private static final Random rnd = new Random(1);
	private static final Logger log = Logger.getLogger(BaggedDecisionTreeClassifier.class.getName());

	private final List<DecisionNode<T, K>> decisionTrees;
	private final K positiveLabel;
	private final int numBags;

	private final DecisionTreeFeature<T>[] features;

	public BaggedDecisionTreeClassifier(int numBags, K positiveLabel, DecisionTreeFeature<T>[] features) {
		this.positiveLabel = positiveLabel;
		this.numBags = numBags;
		this.features = features;
		this.decisionTrees = new ArrayList<>(this.numBags);
	}

	/**
	 * Train BaggedDecisionTree classifier
	 * 
	 * @param trainingSet
	 */
	public void train(List<Z> trainingSet) {

		for (int ind = 0; ind < this.numBags; ind++) {
			// train decision tree
			DecisionNode<T, K> root = new DecisionNode<>(getRandomBagOfTrainingSet(trainingSet), features, -1, null,
					null, null, positiveLabel);
			// add tree to list of bagged trees
			this.decisionTrees.add(root);

			log.info("Generated tree for " + positiveLabel + ", bag " + (ind+1));
		}

	}

	/**
	 * Randomly sample training set with replacement
	 * 
	 * @param examples
	 * @return
	 */
	private List<DecisionTreeExample<T, K>> getRandomBagOfTrainingSet(List<Z> trainingSet) {
		List<DecisionTreeExample<T, K>> retVal = new ArrayList<>(trainingSet.size());
		for (int ind = 0; ind < trainingSet.size(); ind++) {
			int index = rnd.nextInt(trainingSet.size());
			retVal.add(trainingSet.get(index));
		}
		return retVal;
	}

	public double getConfidenceLevel(Z example) {

		double confidenceLevel = 0;

		// average confidence level bagged trees
		int ind = 0;
		for (DecisionNode<T, K> decisionTree : decisionTrees) {
			confidenceLevel += decisionTree.predictFor(example);
			ind++;
		}
		confidenceLevel = confidenceLevel / ind;

		return confidenceLevel;
	}

	public void write(DataOutput output) throws IOException {
		throw new UnsupportedOperationException("Writing out BaggedDecicsionTree classifier is not supported");
	}

	public boolean read(DataInput input) throws IOException {
		throw new UnsupportedOperationException("Reading in BaggedDecicsionTree classifier is not supported");
	}
}
