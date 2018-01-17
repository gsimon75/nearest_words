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

class WordSetLeafNode extends WordSetNode implements WordSet.Queueable {
	ArrayList<String> words;

	WordSetLeafNode() {
		super();
		words = new ArrayList<String>();
	}

	int size() { return words.size(); }
	boolean needsSplit() { return size() >= 32; }

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Dump.indent);
		sb.append(super.toString());
		sb.append("(");
		Iterator<String> it = words.iterator();
		if (it.hasNext()) {
			sb.append(it.next());
		}
		while (it.hasNext()) {
			sb.append(",");
			sb.append(it.next());
		}
		sb.append(")");
		return sb.toString();
	}

	WordSetNode[] explode() {
		int nItems = words.size();
		WordSetLeafNode[] e = new WordSetLeafNode[nItems];
		//System.out.println("explode; nItems=" + nItems);
		for (int i = nItems - 1; i >= 0; i--) {
			//System.out.println("explode; i=" + i);
			e[i] = new WordSetLeafNode();
			e[i].add(words.remove(i));
			//System.out.println(Dump.indent + "Explode " + i + ":  " + e[i]);
		}
		//System.out.println("explode; done");
		boundary.clear();
		return e;
	}

	public void add(WordSetNode n) {
		super.add(n);
		words.addAll(((WordSetLeafNode)n).words);
	}

	public void add(String s) {
		super.add(s);
		words.add(s);
	}

	public void enqueue(WordSet.ResultCollector coll) {
		for (Iterator<String> it = words.iterator(); it.hasNext(); ) 
			coll.result(new WordSetString(it.next()));
	}
}
