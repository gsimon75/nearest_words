import java.lang.reflect.Field;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;

class WordSetString implements WordSet.Queueable {
	String content;
	long id;

	public WordSetString(String s) {
		content = s;
		try {
			WordSet.qNewWord.setLong(1, WordSet.langId);
			WordSet.qNewWord.setString(2, s);
			id = -1;
			if (WordSet.qNewWord.executeUpdate() == 1) {
				ResultSet keys = WordSet.qNewWord.getGeneratedKeys();
				if (keys.next())
					id = keys.getLong(1);
			}
			if (id < 0)
				throw new SQLException("Creating word failed, no ID obtained.");
		}
		catch (SQLException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	public String toString() { return content; }

	public float distance(String t) {
		return Helper.distance(content, t);
	}

	public void enqueue(WordSet.ResultCollector coll) {
		//System.out.println("distance=" + dist + ", item='" + (WordSetString)item + "'");
	}
}

public class WordSet {
	static Connection db;
	static PreparedStatement qNewWord;
	static long langId;

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

	public int levels = 1;

	public interface ResultListener {
		public boolean foundResult(String s, float dist);
	}

	WordSetInternalNode root = new WordSetInternalNode();

	public WordSet(String dbFileName, String langCode) {
		try {
			db = DriverManager.getConnection("jdbc:sqlite:" + dbFileName);
			PreparedStatement qLang = db.prepareStatement("INSERT INTO lang_t (isocode) VALUES (?1)", Statement.RETURN_GENERATED_KEYS);
			qLang.setString(1, langCode);
			langId = -1;
			if (qLang.executeUpdate() == 1) {
				ResultSet keys = qLang.getGeneratedKeys();
				if (keys.next())
					langId = keys.getLong(1);
			}
			if (langId < 0)
				throw new SQLException("Creating lang failed, no ID obtained.");

			qNewWord = db.prepareStatement("INSERT INTO word_t (lang, value) VALUES (?1, ?2)", Statement.RETURN_GENERATED_KEYS);
		}
		catch (SQLException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	void dumpSizes() {
		root.dumpSizes();
		System.out.println();
	}

	void add(String s) {
		WordSetString ws = new WordSetString(s);
		root.add(ws);
		if (root.needsSplit()) {
			WordSetNode[] newNodes = root.split();
			root.add(newNodes[0]);
			root.add(newNodes[1]);
			levels++;
		}
	}

        public String toString() { return root.toString(); }

	void search(final String s, ResultListener listener) {
		PriorityQueue<AbstractMap.SimpleEntry<WordSet.Queueable, Float> > prioQueue = 
			new PriorityQueue<AbstractMap.SimpleEntry<WordSet.Queueable, Float> >(compareByValue);

		ResultCollector collector = new ResultCollector() {
			public void result(WordSet.Queueable n) {
				float dist = n.distance(s);
				//System.out.println("Push; dist=" + dist + ", hash=" + n.hashCode()); //System.out.println(n);
				prioQueue.add(new AbstractMap.SimpleEntry<WordSet.Queueable, Float>(n, dist));
			}
		};

		collector.result(root);
		while (!prioQueue.isEmpty()) {

			PriorityQueue<AbstractMap.SimpleEntry<WordSet.Queueable, Float> > tq = 
				new PriorityQueue<AbstractMap.SimpleEntry<WordSet.Queueable, Float> >(prioQueue);

			/*System.out.print("Queue:");
			while (!tq.isEmpty()) {
				AbstractMap.SimpleEntry<WordSet.Queueable, Float> e = tq.poll();
				System.out.print(" " + e.getValue());
			}
			System.out.println();*/

			AbstractMap.SimpleEntry<WordSet.Queueable, Float> e = prioQueue.poll();
			WordSet.Queueable item = e.getKey();
			float dist = e.getValue().floatValue();
			//System.out.println("\nPop; dist=" + dist + ", hash=" + item.hashCode()); //System.out.println(item);
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
