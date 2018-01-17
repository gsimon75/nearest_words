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

class WordSetInternalNode extends WordSetNode implements WordSet.Queueable {
	ArrayList<WordSetNode> children;

	WordSetInternalNode() {
		super();
		children = new ArrayList<WordSetNode>();
	}

	int size() { return children.size(); }
	boolean needsSplit() { return size() >= 16; }

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Dump.indent);
		sb.append(super.toString());
		sb.append("{\n");
		Dump.inc();
		Iterator<WordSetNode> it = children.iterator();
		if (it.hasNext()) {
			sb.append(it.next());
		}
		while (it.hasNext()) {
			sb.append(",\n");
			sb.append(it.next());
		}
		sb.append("\n");
		Dump.dec();
		sb.append(Dump.indent);
		sb.append("}");
		return sb.toString();
	}

	WordSetNode[] explode() {
		int nItems = children.size();
		WordSetInternalNode[] e = new WordSetInternalNode[nItems];
		for (int i = nItems - 1; i >= 0; i--) {
			e[i] = new WordSetInternalNode();
			e[i].add(children.remove(i));
		}
		boundary.clear();
		return e;
	}

	public void add(WordSetNode newChild) {
		super.add(newChild);
		children.add(newChild);
	}

	public void add(String s) {
		super.add(s);

		float leastAreaIncrease = Float.POSITIVE_INFINITY;
		float leastArea = Float.POSITIVE_INFINITY;
		WordSetNode bestFit = null;

		for (Iterator<WordSetNode> it = children.iterator(); it.hasNext(); ) {
			WordSetNode n = it.next();
			float d = n.boundary.areaIncreaseIfAdded(s);
			//System.out.println("WordSetNode.add; candidate=" + n + ", areaInc=" + d);
			if ((d < leastAreaIncrease) ||
			    (( d == leastAreaIncrease) && (n.size() < leastArea))) {
				leastAreaIncrease = d;
				leastArea = n.size();
				bestFit = n;
			}
		}
		if (bestFit != null) {
			bestFit.add(s);
			if (bestFit.needsSplit()) {
				WordSetNode[] newNodes = bestFit.split();
				children.remove(bestFit);
				children.add(newNodes[0]);
				children.add(newNodes[1]);
			}
		}
		else {
			WordSetLeafNode n = new WordSetLeafNode();
			n.add(s);
			children.add(n);
		}
	}

	public void enqueue(WordSet.ResultCollector coll) {
		for (Iterator<WordSetNode> it = children.iterator(); it.hasNext(); ) 
			coll.result(it.next());
	}
}
