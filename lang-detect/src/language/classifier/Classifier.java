package language.classifier;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Generic Classifier interface
 * 
 * @author Andrey Gusev
 */
public interface Classifier<T extends Comparable<T>, K, Z extends ClassifierExample<T, K>> {

	/**
	 * Will train classifier to represent certain hypothesis
	 * 
	 * @param trainingSet
	 *            - the set that represents hypothesis, should have examples
	 *            labeled
	 */
	void train(List<Z> trainingSet);

	/**
	 * 
	 * @param example
	 *            - classifier example that we would like to classify
	 * @return
	 */
	double getConfidenceLevel(Z example);

	/**
	 * writes classifier to file
	 */
	void writeToFile(File file) throws IOException;

	/**
	 * read classifier to file
	 * 
	 * @return whether classifier was initialized from file
	 */
	boolean readFromFile(File file) throws IOException;

}
