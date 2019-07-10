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
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class VocabularySearch extends Vocabulary {

	public interface ResultListener {
		public boolean foundResult(String s, float dist);
	}


	public class TopNResultListener implements ResultListener {
		int N, n;

		public TopNResultListener(int N) {
			this.N = N;
			n = 0;
		}
		public boolean foundResult(String s, float dist) {
			System.out.println("n=" + n + ",dist=" + dist + ", s='" + s + "'");
			n++;
			return n < N;
		}
		public void reset() {
			n = 0;
		}
	}

	public static final void main(String[] argv) { new VocabularySearch(argv); }

	VocabularySearch(String[] argv) {
		String langCode = "deu";
		connectDB("wordset.db");
		long rootNodeId;
		try {
			db.setReadOnly(true);
			qGetLang.setString(1, langCode);
			ResultSet rs = qGetLang.executeQuery();
			if (!rs.next())
				throw new RuntimeException("Lang '" + langCode + "' not found");
			rootNodeId = rs.getLong(2);
		}
		catch (SQLException e) { throw new RuntimeException(e); }

		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);

		TopNResultListener dumpTop10 = new TopNResultListener(10);

		while (true) {
			System.out.print("Ready> ");
			String line;
			try {
				line = br.readLine();
			}
			catch (IOException ex) {
				line = null;
			}
			if (line == null)
				break;
			String word = toNFD.normalize(line.trim());
			search(rootNodeId, word, dumpTop10);
			dumpTop10.reset();
		}
	}

	void search(long rootNodeId, final String s, ResultListener listener) {
		PriorityQueue<AbstractMap.SimpleEntry<Queueable, Float> > prioQueue = new PriorityQueue<AbstractMap.SimpleEntry<Queueable, Float> >(compareByValue);

		ResultCollector collector = new ResultCollector() {
			public void addResult(Queueable n) {
				float dist = n.distance(s);
				//System.out.println("Push; dist=" + dist + ", hash=" + n.hashCode()); //System.out.println(n);
				prioQueue.add(new AbstractMap.SimpleEntry<Queueable, Float>(n, dist));
			}
		};

		collector.addResult(new DBNode(rootNodeId));
		while (!prioQueue.isEmpty()) {
			AbstractMap.SimpleEntry<Queueable, Float> e = prioQueue.poll();
			Queueable item = e.getKey();
			float dist = e.getValue().floatValue();
			//System.out.println("\nPop; dist=" + dist + ", hash=" + item.hashCode()); //System.out.println(item);
			if (item instanceof DBString) {
				DBString dbs = (DBString)item;
				if (!listener.foundResult(dbs.toString(), dist))
					break;
			}
			else if (item instanceof DBNode) {
				DBNode dbn = (DBNode)item;
				dbn.enqueueChildren(collector);
			}
		}
	}
}
