import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Set;
import java.util.PriorityQueue;
import java.util.Map;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;

class WordSetString implements WordSet.Queueable {
	String content;

	public WordSetString(String s) { content = s; }
	public String toString() { return content; }

	public float distance(String t) {
		return Helper.distance(content, t);
	}

	public void enqueue(WordSet.ResultCollector coll) {
		//System.out.println("distance=" + dist + ", item='" + (WordSetString)item + "'");
	}
}

public class WordSet {
	public interface Queueable {
		public float distance(String s);
		public void enqueue(WordSet.ResultCollector coll);
	}

	public interface ResultCollector {
		public void result(WordSet.Queueable n);
	}

	static Comparator<AbstractMap.SimpleEntry<WordSet.Queueable, Float> > compareByValue =
		new Comparator<AbstractMap.SimpleEntry<WordSet.Queueable, Float> > () {
			public int compare(AbstractMap.SimpleEntry<WordSet.Queueable, Float> a, AbstractMap.SimpleEntry<WordSet.Queueable, Float> b) {
				if (a.getValue() < b.getValue())
					return -1;
				else if (a.getValue() > b.getValue())
					return 1;
				else
					return 0;
			}
		};

	public interface ResultListener {
		public boolean foundResult(String s, float dist);
	}

	WordSetInternalNode root = new WordSetInternalNode();

	void add(String s) {
		root.add(s);
		if (root.needsSplit()) {
			WordSetNode[] newNodes = root.split();
			root.add(newNodes[0]);
			root.add(newNodes[1]);
		}
	}

        public String toString() { return root.toString(); }

	void search(final String s, ResultListener listener) {
		PriorityQueue<AbstractMap.SimpleEntry<WordSet.Queueable, Float> > prioQueue = 
			new PriorityQueue<AbstractMap.SimpleEntry<WordSet.Queueable, Float> >(compareByValue);

		ResultCollector collector = new ResultCollector() {
			public void result(WordSet.Queueable n) {
				float dist = n.distance(s);
				//System.out.println("Push; dist=" + dist); System.out.println(n);
				prioQueue.add(new AbstractMap.SimpleEntry<WordSet.Queueable, Float>(n, dist));
			}
		};

		collector.result(root);
		while (!prioQueue.isEmpty()) {
			AbstractMap.SimpleEntry<WordSet.Queueable, Float> e = prioQueue.poll();
			WordSet.Queueable item = e.getKey();
			float dist = e.getValue().floatValue();
			//System.out.println("\nPop; dist=" + dist); System.out.println(item);
			if (item instanceof WordSetString) {
				if (!listener.foundResult(((WordSetString)item).content, dist))
					break;
			}
			else {
				item.enqueue(collector);
			}
		}
	}
}
