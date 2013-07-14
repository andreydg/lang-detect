package language.model;

import language.model.NgramLanguageDetector.ClassificationAlgorithm;

/**
 * Single language test with logistic classifier
 * 
 * @author Andrey Gusev
 * 
 */
public class LogisticRegressionSingleLangTest extends BaseSingleLangTest {

	public LogisticRegressionSingleLangTest(String name) {
		super(name);
	}
	
	// basic check with logistic classifier
	public void testBasicPhrase() throws Exception {
		_testBasicPhrase(ClassificationAlgorithm.LOGISTIC_CLASSIFIER);
	}
	
	// check longer strings
	public void testLargeEnglishString() throws Exception {
		_testLargeEnglishString(ClassificationAlgorithm.LOGISTIC_CLASSIFIER);
	}

}
