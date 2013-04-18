package language.model.multiling;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import language.util.Pair;


/**
 * Generic interface for language boundary detection
 * 
 * @author Andrey Gusev
 * 
 */
public interface LanguageBoundaryDetector {

	public List<Pair<String, Locale>> tagStringWithLanguages(String s) throws IOException;
}
