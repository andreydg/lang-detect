package language.model;

import java.util.Locale;

import junit.framework.TestCase;
import language.model.NgramLanguageDetector.ClassificationAlgorithm;

/**
 * Single language test
 * 
 * @author Andrey Gusev
 * 
 */
public abstract class BaseSingleLangTest extends TestCase {
	
	
	private final LanguageDetector detector;
	
	public BaseSingleLangTest(String name) {
		super(name);
		this.detector = NgramLanguageDetectorForTests.get();
	}
	
	// basic check
	protected void _testBasicPhrase(ClassificationAlgorithm type) throws Exception {

		assertEquals("Didn't match language", Locale.ENGLISH, detector.getMostLikelyLanguage(getEnglishString(), type));

		assertEquals("Didn't match language", Locale.FRENCH, detector.getMostLikelyLanguage(getFrenchString(), type));

		assertEquals("Didn't match language", Locale.ITALIAN, detector.getMostLikelyLanguage(getItalianString(), type));

		assertEquals("Didn't match language", Locale.GERMAN, detector.getMostLikelyLanguage(getGermanString(), type));

		assertEquals("Didn't match language", new Locale("es"), detector.getMostLikelyLanguage(getSpanishString(), type));

		assertEquals("Didn't match language", new Locale("pt"), detector.getMostLikelyLanguage(getPortugueseString(), type));

	}

	// check longer strings
	protected void _testLargeEnglishString(ClassificationAlgorithm type) throws Exception {

		StringBuilder sb = new StringBuilder(65536);
		for (int ind = 0; ind < 1000; ind++) {
			sb.append(getEnglishString()).append(" ");
		}

		assertEquals("Didn't match language", Locale.ENGLISH, detector.getMostLikelyLanguage(sb.toString(), type));
	}

	protected static String getEnglishString() {
		return "This is a test string for language detection";
	}

	protected static String getFrenchString() {
		return "Il s'agit d'une chaîne de test pour la détection de la langue";
	}

	protected static String getItalianString() {
		return "Questa è una stringa di prova per il rilevamento della lingua";
	}

	protected static String getGermanString() {
		return "Dies ist ein Test-String für Spracherkennung";
	}

	protected static String getSpanishString() {
		return "Esta es una cadena de prueba para la detección de idioma";
	}

	protected static String getPortugueseString() {
		return "Esta é uma seqüência de teste para detecção de idioma";
	}


}
