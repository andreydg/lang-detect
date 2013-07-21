package language.model;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * NgramLanguageDetector with overrides for test
 * 
 * @author Andrey Gusev
 *
 */
public class NgramLanguageDetectorForTests extends NgramLanguageDetector {
	
	private static final Logger log = Logger.getLogger(NgramLanguageDetectorForTests.class.getName());

	private static String RELATIVE_DATA_PATH = "../lang-detect/war/";
	
	private NgramLanguageDetectorForTests(File basePath) {
		super(basePath);
	}
	
	protected static NgramLanguageDetector get() {
		return new NgramLanguageDetectorForTests(new File(RELATIVE_DATA_PATH));
	}
	
	
	/**
	 * Override to to write to file
	 */
	@Override
	protected final DataOutputStream getLogisitcClassifierDataOutput(Locale locale) {

		String location = getLogisticClassifierFileCache(locale);
		try {
			return new DataOutputStream(new FileOutputStream(location));
		} catch (FileNotFoundException e) {
			log.severe("Could not write classifier to: " + location);
			return null;
		}
	}
	
	/**
	 * Sample to speed up test
	 */
	@Override
	protected float getDatasetSampleRatio(){
		return 0.01f;
	}

}
