package language.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implements logistic regression classifier
 * 
 * @author Andrey Gusev
 */
public class LogisticRegressionClassifier<K, Z extends ClassifierExample<Double, K>> implements
		Classifier<Double, K, Z> {
	
	private static final Logger log = Logger.getLogger(LogisticRegressionClassifier.class.getName());

	private final int numFeatures;
	private final List<Double> featureWeights;
	private final K positiveLabel;

	private static int MAX_ITER = 100;
	private static int MIN_ITER = 10;
	private static double LEARNING_RATE = 10.0;
	private static double SUM_UPDATES_THRESHOLD = 1.0;

	private static boolean SHOW_ADDITIONAL_ITER_OUTPUT = false;
	private static boolean COLLECT_ADDITIONAL_STATS = false;

	private static String WEIGHT_DELIMETER = " ";

	public LogisticRegressionClassifier(int numFeatures, K positiveLabel) {
		this.numFeatures = numFeatures;
		this.featureWeights = new ArrayList<>(this.numFeatures);
		this.positiveLabel = positiveLabel;
	}

	private boolean getClassOf(Z example) {
		return getConfidenceLevel(example) > 0.5;
	}

	public double getConfidenceLevel(Z example) {
		double sum = getSumGivenWeights(example.getFeatureValues(positiveLabel));
		double logisticValue = getLogisticValue(sum);
		return logisticValue;
	}

	public void train(List<Z> trainingData) {
		for (int ind = 0; ind < numFeatures; ind++) {
			// set all to one
			this.featureWeights.add(1.0d);
		}

		int iteration = 0, numMistakes = 0, falsePositives = 0, falseNegatives = 0;
		double sumOfUdpates = 0;
		while (iteration < MIN_ITER
				|| (iteration >= MIN_ITER && iteration < MAX_ITER && sumOfUdpates > SUM_UPDATES_THRESHOLD)) {
			iteration++;
			long startTime = System.currentTimeMillis();
			sumOfUdpates = 0;
			numMistakes = 0;
			falsePositives = 0;
			falseNegatives = 0;
			for (int ind = 0; ind < this.numFeatures; ind++) {
				// update each weight
				double sumOfErrors = 0;
				for (Z trainingExample : trainingData) {

					double labelValue = trainingExample.isPositive(this.positiveLabel) ? 1.0 : 0.0;

					double rawValue = this.getConfidenceLevel(trainingExample);
					double errorMult = labelValue - rawValue;
					// if this is error collect training data statistics
					if (COLLECT_ADDITIONAL_STATS
							&& trainingExample.isPositive(this.positiveLabel) != this.getClassOf(trainingExample)) {
						numMistakes++;
						if (trainingExample.isPositive(this.positiveLabel)) {
							falseNegatives++;
						} else {
							falsePositives++;
						}
					}
					if (Math.abs(errorMult) > 0.000001) {
						sumOfErrors += errorMult * trainingExample.getFeatureValues(this.positiveLabel).get(ind);
					}
				}

				sumOfUdpates += Math.abs(sumOfErrors);
				this.featureWeights.set(ind, this.featureWeights.get(ind) + LEARNING_RATE * sumOfErrors);
			}

			// additional classifier output
			if (SHOW_ADDITIONAL_ITER_OUTPUT) {
				log.info("iter:" + iteration + ", sumOfUdpates: " + sumOfUdpates + ", num mistakes: "
						+ numMistakes + ", positiveLabel :" + this.positiveLabel + ", iter time: "
						+ (System.currentTimeMillis() - startTime) + "ms");
			}
		}

		if (COLLECT_ADDITIONAL_STATS) {
			// print statistics for training
			log.info("Overall classifier error rate on training data " + ((double) numMistakes)
					/ (this.numFeatures * trainingData.size()));
			log.info("False positive classifier error rate on training data " + ((double) falsePositives)
					/ (this.numFeatures * trainingData.size()));
			log.info("False negatives classifier error rate on training data " + ((double) falseNegatives)
					/ (this.numFeatures * trainingData.size()));
		}
		printWeights();
	}

	public void writeToFile(File file) throws IOException {
		// TODO: use persistent storage rather than files
		//

		// BufferedWriter out = new BufferedWriter(new FileWriter(file));
		// out.write(getWeightsAsString());
		// out.close();
	}

	public boolean readFromFile(File file) throws IOException {
		if (!file.exists()) {
			return false;
		}

		String s;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			while ((s = br.readLine()) != null) {
				String[] weights = s.split(WEIGHT_DELIMETER);
				if (weights.length != this.numFeatures) {
					return false;
				}
				for (int ind = 0; ind < this.numFeatures; ind++) {
					this.featureWeights.add(Double.valueOf(weights[ind]));
				}

			}
		}
		return true;
	}

	private double getLogisticValue(double sumValue) {
		return 1.0 / (1.0 + Math.exp(-1.0 * sumValue));
	}

	private double getSumGivenWeights(List<Double> features) {

		double sum = 0;
		for (int ind = 0; ind < this.numFeatures; ind++) {
			sum += features.get(ind) * this.featureWeights.get(ind);
		}

		return sum;

	}

	private void printWeights() {
		StringBuilder sb = new StringBuilder("Feature weights: [");
		sb.append(getWeightsAsString());
		sb.append("]");

		System.out.println(sb.toString());
	}

	private String getWeightsAsString() {
		StringBuilder sb = new StringBuilder(512);
		for (Double weight : this.featureWeights) {
			sb.append(weight).append(WEIGHT_DELIMETER);
		}

		return sb.toString().trim();
	}

	public K getPositiveLabel() {
		return this.positiveLabel;
	}

}
