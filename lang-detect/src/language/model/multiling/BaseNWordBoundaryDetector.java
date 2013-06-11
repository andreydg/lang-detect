package language.model.multiling;

import language.model.NgramLanguageDetector;
import language.model.NgramLanguageDetector.ClassificationAlgorithm;


/**
 * Base LanguageBoundaryDetector with common state
 * 
 * @author Andrey Gusev
 *
 */
public abstract class BaseNWordBoundaryDetector implements LanguageBoundaryDetector {
	
	protected final ClassificationAlgorithm algorithmToUse;
	protected final NgramLanguageDetector detector;

	public BaseNWordBoundaryDetector(ClassificationAlgorithm algorithmToUse, NgramLanguageDetector detector) {
		this.algorithmToUse = algorithmToUse;
		this.detector = detector;
	}
}
