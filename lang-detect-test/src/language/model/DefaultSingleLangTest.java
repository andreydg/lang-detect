package language.model;

import language.model.NgramLanguageDetector.ClassificationAlgorithm;

/**
 * Single language test
 * 
 * @author Andrey Gusev
 * 
 */
public class DefaultSingleLangTest extends BaseSingleLangTest {


	public DefaultSingleLangTest(String name) {
		super(name);
	}
	
	// basic check with linear weights
	public void testBasicPhraseLinear() throws Exception {
		_testBasicPhrase(ClassificationAlgorithm.LINEAR_WEIGHTS);
	}
	
	// check longer strings
	public void testLargeEnglishString() throws Exception {
		_testLargeEnglishString(ClassificationAlgorithm.LINEAR_WEIGHTS);
	}

}
