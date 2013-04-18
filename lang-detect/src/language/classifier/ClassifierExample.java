package language.classifier;

import java.util.List;

/**
 * 
 * Interface for example used by classifiers
 * 
 * @author Andrey Gusev
 */
public interface ClassifierExample<T extends Comparable<T>, K> {

	/**
	 * 
	 * @return get list of values for positive label for all features
	 */
	List<T> getFeatureValues(K positiveLabel);

	/**
	 * 
	 * @return boolean whether this example cab classified as positive
	 */
	boolean isPositive(K positiveLabel);

}
