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

		assertEquals(Locale.ENGLISH, detector.getMostLikelyLanguage("This is a test string for language detection"));

		assertEquals(Locale.FRENCH,
				detector.getMostLikelyLanguage("Il s'agit d'une chaîne de test pour la détection de la langue"));

		assertEquals(Locale.ITALIAN,
				detector.getMostLikelyLanguage("Questa è una stringa di prova per il rilevamento della lingua"));

		assertEquals(Locale.GERMAN, detector.getMostLikelyLanguage("Dies ist ein Test-String für Spracherkennung"));

		assertEquals(new Locale("es"),
				detector.getMostLikelyLanguage("Esta es una cadena de prueba para la detección de idioma"));

		assertEquals(new Locale("pt"),
				detector.getMostLikelyLanguage("Esta é uma seqüência de teste para detecção de idioma"));

	}

}
