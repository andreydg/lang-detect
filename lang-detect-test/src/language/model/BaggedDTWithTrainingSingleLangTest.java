package language.model;

import language.model.NgramLanguageDetector.ClassificationAlgorithm;

public class BaggedDTWithTrainingSingleLangTest extends BaseSingleLangTest {
	
	
	public BaggedDTWithTrainingSingleLangTest(String name) {
		super(name);
	}
	
	// basic check with bagged decision tree classifier
	public void testBasicPhrase() throws Exception {
		_testBasicPhrase(ClassificationAlgorithm.BAGGED_DECISION_TREE);
	}
	
	// check longer strings
	public void testLargeEnglishString() throws Exception {
		_testLargeEnglishString(ClassificationAlgorithm.BAGGED_DECISION_TREE);
	}


}
