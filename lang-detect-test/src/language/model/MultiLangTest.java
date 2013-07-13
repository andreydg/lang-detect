package language.model;

import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;
import language.model.NgramLanguageDetector.ClassificationAlgorithm;
import language.model.multiling.LanguageBoundaryDetector;
import language.model.multiling.SlidingWindowBigramBoundaryDetector;
import language.util.Pair;

/**
 * Test multi-lang strings
 * 
 * @author Andrey Gusev
 */
public class MultiLangTest extends TestCase {

	public MultiLangTest(String name) {
		super(name);
	}

	// only checks the order, doesn't check the boundaries
	public void testMultiLangOrder() throws Exception {

		LanguageBoundaryDetector boundaryDetector = new SlidingWindowBigramBoundaryDetector(
				ClassificationAlgorithm.LINEAR_WEIGHTS, NgramLanguageDetector.getForTests(), 4);

		StringBuilder sb = new StringBuilder();
		sb.append(DefaultSingleLangTest.getEnglishString());
		sb.append(DefaultSingleLangTest.getFrenchString());
		sb.append(DefaultSingleLangTest.getItalianString());
		sb.append(DefaultSingleLangTest.getGermanString());
		sb.append(DefaultSingleLangTest.getSpanishString());
		sb.append(DefaultSingleLangTest.getPortugueseString());
		List<Pair<String, Locale>> tags = boundaryDetector.tagStringWithLanguages(sb.toString());

		assertEquals("Should have returned 6 phrases", 6, tags.size());

		assertEquals("Didn't match language", Locale.ENGLISH, tags.get(0).getSecond());
		assertEquals("Didn't match language", Locale.FRENCH, tags.get(1).getSecond());
		assertEquals("Didn't match language", Locale.ITALIAN, tags.get(2).getSecond());
		assertEquals("Didn't match language", Locale.GERMAN, tags.get(3).getSecond());
		assertEquals("Didn't match language", new Locale("es"), tags.get(4).getSecond());
		assertEquals("Didn't match language", new Locale("pt"), tags.get(5).getSecond());

	}

	// for larger string only checks the order, doesn't check the boundaries
	public void testLargeMultiLangOrder() throws Exception {

		LanguageBoundaryDetector boundaryDetector = new SlidingWindowBigramBoundaryDetector(
				ClassificationAlgorithm.LINEAR_WEIGHTS, NgramLanguageDetector.getForTests(), 4);

		final int numRepetitions = 100;

		StringBuilder sb = new StringBuilder(65536);
		for (int ind = 0; ind < numRepetitions; ind++) {
			sb.append(DefaultSingleLangTest.getEnglishString());
			sb.append(DefaultSingleLangTest.getFrenchString());
			sb.append(DefaultSingleLangTest.getItalianString());
			sb.append(DefaultSingleLangTest.getGermanString());
			sb.append(DefaultSingleLangTest.getSpanishString());
			sb.append(DefaultSingleLangTest.getPortugueseString());
		}
		List<Pair<String, Locale>> tags = boundaryDetector.tagStringWithLanguages(sb.toString());

		assertEquals("Should have returned " + (6 * numRepetitions) + "phrases", 6 * numRepetitions, tags.size());

		for (int ind = 0; ind < numRepetitions; ind++) {
			assertEquals("Didn't match language", Locale.ENGLISH, tags.get(ind * 6).getSecond());
			assertEquals("Didn't match language", Locale.FRENCH, tags.get(ind * 6 + 1).getSecond());
			assertEquals("Didn't match language", Locale.ITALIAN, tags.get(ind * 6 + 2).getSecond());
			assertEquals("Didn't match language", Locale.GERMAN, tags.get(ind * 6 + 3).getSecond());
			assertEquals("Didn't match language", new Locale("es"), tags.get(ind * 6 + 4).getSecond());
			assertEquals("Didn't match language", new Locale("pt"), tags.get(ind * 6 + 5).getSecond());
		}

	}

}
