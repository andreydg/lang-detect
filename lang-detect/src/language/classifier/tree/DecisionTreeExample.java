package language.classifier.tree;

import language.classifier.ClassifierExample;

/**
 * Interface used by Decision tree in array of data at the node
 * 
 * @author Andrey Gusev
 * 
 */
public interface DecisionTreeExample<T extends Comparable<T>, K> extends ClassifierExample<T, K> {

	/**
	 * 
	 * @param feat
	 *            - particular feature to be examined
	 * @param value
	 *            - Implementation of the comparable interface to ensure that
	 *            this example's feature can be compared with value
	 * 
	 * @return boolean to indicate whether this example has value specified for
	 *         the feature
	 */
	boolean hasValueForFeature(DecisionTreeFeature<T> feat, Comparable<T> lowValue, Comparable<T> highValue,
			K positiveLabel);

}
