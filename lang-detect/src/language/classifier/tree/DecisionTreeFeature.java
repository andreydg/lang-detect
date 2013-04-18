package language.classifier.tree;

import java.util.List;

/**
 * Interface this is used Decision Tree to split information available at each
 * node
 * 
 * @author Andrey Gusev
 * 
 */
public interface DecisionTreeFeature<T> {

	/**
	 * 
	 * @return array of implementations of Comparable interface representing
	 *         finite set of values that this feature can take on
	 */
	public List<? extends Comparable<T>> getPossibleValues();

	public List<Range<T>> getRanges();

	public class Range<T> {

		protected final Comparable<T> lowBound;
		protected final Comparable<T> highBound;

		public Range(Comparable<T> lowBound, Comparable<T> highBound) {
			this.lowBound = lowBound;
			this.highBound = highBound;
		}
	}

}
