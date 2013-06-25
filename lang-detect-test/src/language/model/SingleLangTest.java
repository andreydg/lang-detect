package language.model;

import java.util.Locale;

import junit.framework.TestCase;

/**
 * Single language test
 * 
 * @author Andrey Gusev
 * 
 */
public class SingleLangTest extends TestCase {

	public SingleLangTest(String name) {
		super(name);
	}

	// basic check
	public void testBasicPhrase() throws Exception {

		LanguageDetector detector = NgramLanguageDetector.getForTests();

		assertEquals("Didn't match language", Locale.ENGLISH, detector.getMostLikelyLanguage(getEnglishString()));

		assertEquals("Didn't match language", Locale.FRENCH, detector.getMostLikelyLanguage(getFrenchString()));

		assertEquals("Didn't match language", Locale.ITALIAN, detector.getMostLikelyLanguage(getItalianString()));

		assertEquals("Didn't match language", Locale.GERMAN, detector.getMostLikelyLanguage(getGermanString()));

		assertEquals("Didn't match language", new Locale("es"), detector.getMostLikelyLanguage(getSpanishString()));

		assertEquals("Didn't match language", new Locale("pt"), detector.getMostLikelyLanguage(getPortugueseString()));

	}

	// check longer strings
	public void testLargeEnglishString() throws Exception {

		StringBuilder sb = new StringBuilder(65536);
		for (int ind = 0; ind < 1000; ind++) {
			sb.append(getEnglishString()).append(" ");
		}

		LanguageDetector detector = NgramLanguageDetector.getForTests();
		assertEquals("Didn't match language", Locale.ENGLISH, detector.getMostLikelyLanguage(sb.toString()));
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
