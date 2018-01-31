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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.nio.ByteBuffer;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer2;

public class WordSet {
	Connection db;
	PreparedStatement qNewWord, qNewNode, qLangRootNode;
	PreparedStatement qGetWord, qGetNode;

	interface ResultCollector {
		public void addResult(Queueable n);
	}

	interface Queueable {
		public long getID();
		public float distance(String s);
		public void enqueueChildren(ResultCollector coll);
	}

	static Normalizer2 toNFD = Normalizer2.getNFDInstance();

	static Comparator<AbstractMap.SimpleEntry<Queueable, Float> > compareByValue =
		new Comparator<AbstractMap.SimpleEntry<Queueable, Float> > () {
			public int compare(AbstractMap.SimpleEntry<Queueable, Float> a, AbstractMap.SimpleEntry<Queueable, Float> b) {
				if (a.getValue() < b.getValue())
					return -1;
				else if (a.getValue() > b.getValue())
					return 1;
				else
					return 0;
			}
		};

	class DBString implements Queueable {
		String content;
		long langId;
		long id;

		public DBString(long nId) {
			id = nId;
			content = null;
			langId = -1;
		}

		public DBString(long nId, String s, long nLangId) {
			id = nId;
			content = s;
			langId = nLangId;
		}

		public DBString(String s, long nLangId) {
			id = -1;
			content = toNFD.normalize(s.trim());
			langId = nLangId;
		}

		public float distance(String t) {
			return Helper.distance(toString(), t);
		}

		public long getID() {
			if (id == -1) {
				try {
					qNewWord.setLong(1, langId);
					qNewWord.setString(2, content);
					if (qNewWord.executeUpdate() < 0)
						throw new SQLException("Creating word failed, no ID obtained.");
					ResultSet keys = qNewWord.getGeneratedKeys();
					if (keys.next())
						id = keys.getLong(1);
				}
				catch (SQLException e) { throw new RuntimeException(e); }
			}
			return id;
		}

		public void enqueueChildren(WordSet.ResultCollector coll) {
		}

		void fetch() {
			try {
				qGetWord.setLong(1, id);
				ResultSet rs = qGetWord.executeQuery();
				if (rs.isAfterLast())
					throw new RuntimeException("Word '" + id + "' not found");
				langId = rs.getLong(1);
				content = rs.getString(2);
			}
			catch (SQLException e) { throw new RuntimeException(e); }
		}

		public String toString() {
			if (content == null)
				fetch();
			return content;
		}

		public long getLangId() {
			if (langId == -1)
				fetch();
			return langId;
		}

	}
	void connectDB(String dbFileName) {
		try {
			Class.forName("org.sqlite.JDBC");
			db = DriverManager.getConnection("jdbc:sqlite:" + dbFileName);
			db.setAutoCommit(false);

			qGetWord = db.prepareStatement("SELECT lang, value FROM word_t WHERE id=?1");
			qGetNode = db.prepareStatement("SELECT ntype, boundary, mandatory_pfx, children FROM node_t WHERE id=?1");
			qNewWord = db.prepareStatement("INSERT INTO word_t (lang, value) VALUES (?1, ?2)", Statement.RETURN_GENERATED_KEYS);
			qNewNode = db.prepareStatement("INSERT INTO node_t (ntype, boundary, mandatory_pfx, children) VALUES (?1, ?2, ?3, ?4)", Statement.RETURN_GENERATED_KEYS);
			qLangRootNode = db.prepareStatement("UPDATE lang_t SET rootnode=?2 WHERE id=?1", Statement.RETURN_GENERATED_KEYS);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	class DBNode implements Queueable {
		long id;
		boolean isLeaf;
		Expression boundary;
		ArrayList<Queueable> children;
		ByteBuffer childIDs;

		DBNode(boolean leaf) { // create blank for builting the tree
			id = -1;
			isLeaf = leaf;
			//boundary = new Expression();
			//children = new ArrayList<Queueable>();
		}

		DBNode(long nid) { // create existing from DB
			try {
				id = nid;
				qGetNode.setLong(1, id);
				ResultSet rs = qGetNode.executeQuery();
				if (rs.isAfterLast())
					throw new RuntimeException("Node '" + id + "' not found");

				isLeaf = (rs.getInt(1) == 0);
				boundary = new Expression(rs.getString(2), rs.getInt(3));
				childIDs = ByteBuffer.wrap(rs.getBytes(4));
			}
			catch (SQLException e) { throw new RuntimeException(e); }
		}

		int size() {
			return children.size();
		}

		boolean needsSplit() {
			return size() >= 24;
		}

		public void add(Queueable n) {
			if (isLeaf) {
				if (!(n instanceof DBString))
					throw new RuntimeException("Leaf node may contain only WordSetStrings");
				DBString ns = (DBString)n;
				boundary.add(ns.content);
				children.add(ns);
			}
			else if (n instanceof DBNode) {
				DBNode nn = (DBNode)n;
				boundary.add(nn.boundary);
				children.add(nn);
			}
			else {
				DBString ns = (DBString)n;
				boundary.add(ns.content);
				double leastAreaIncrease = Double.POSITIVE_INFINITY;
				double leastArea = Double.POSITIVE_INFINITY;
				DBNode bestFit = null;

				for (Iterator<Queueable> it = children.iterator(); it.hasNext(); ) {
					DBNode nn = (DBNode)it.next();
					double d = nn.boundary.areaIncreaseIfAdded(ns.content);
					if ((d < leastAreaIncrease) || ((d == leastAreaIncrease) && (nn.size() < leastArea))) {
						leastAreaIncrease = d;
						leastArea = nn.size();
						bestFit = nn;
					}
				}
				if (bestFit != null) {
					bestFit.add(ns);
					if (bestFit.needsSplit()) {
						DBNode[] newNodes = bestFit.split();
						children.remove(bestFit);
						children.add(newNodes[0]);
						children.add(newNodes[1]);
					}
				}
				else {
					DBNode nn = new DBNode(true);
					nn.add(ns);
					children.add(nn);
				}
			}
		}

		public void merge(DBNode n) {
			if (isLeaf != n.isLeaf)
				throw new RuntimeException("Mixed merge");
			boundary.add(n.boundary);
			children.addAll(n.children);
		}

		public float distance(String s) {
			return boundary.distance(s);
		}

		DBNode reduce(DBNode n) {
			while (!isLeaf && (n.size() == 1))
				n = (DBNode)(children.get(0));
			return n;
		}

		// Split the node, return the 2 results in an array
		// NOTE: this instance is cleared in the process
		DBNode[] split() {
			// explode
			int nItems = size();
			DBNode[] e = new DBNode[nItems];
			for (int i = nItems - 1; i >= 0; i--) {
				e[i] = new DBNode(isLeaf);
				e[i].add(children.remove(i));
			}
			boundary.clear();

			// calculate a cost matrix (NOTE: only the upper half is used)
			double area_inc[][] = new double[nItems][nItems];
			for (int i = 0; i < (nItems - 1); i++)
				for (int j = i + 1; j < nItems; j++)
					area_inc[i][j] = e[i].boundary.areaIncreaseIfAdded(e[j].boundary);

			for (int nRemaining = nItems; nRemaining > 2; nRemaining--) {
				// find the pair whose merge would result in a less increase of area,
				// and break the ties by choosing the smallest one
				double leastAreaIncrease = Double.POSITIVE_INFINITY;
				double leastArea = Double.POSITIVE_INFINITY;
				int mergeTo = -1;
				int mergeWhat = -1;

				for (int i = 0; i < (nItems - 1); i++) if (e[i] != null) {
					for (int j = i + 1; j < nItems; j++) if (e[j] != null) {
						if ((area_inc[i][j] < leastAreaIncrease) ||
								((area_inc[i][j] == leastAreaIncrease) && (e[i].boundary.area() < leastArea))) {
							mergeTo = i;
							mergeWhat = j;
							leastAreaIncrease = area_inc[i][j];
							leastArea = e[i].boundary.area();
								}
					}
				}

				if ((mergeTo < 0) || (mergeWhat < 0)) {
					System.out.println("Cannot find merge src or dest, nItems=" + nItems + ", nRemaining=" + nRemaining);
					System.out.println("mergeTo=" + mergeTo + ", mergeWhat=" + mergeWhat + ", leastAreaIncrease=" + leastAreaIncrease + ", leastArea=" + leastArea);
					for (int i = 0; i < (nItems - 1); i++) if (e[i] != null) {
						for (int j = i + 1; j < nItems; j++) if (e[j] != null) {
							System.out.print("\t" + area_inc[i][j]);
						}
						System.out.println();
					}
					for (int i = 0; i < nItems; i++) if (e[i] != null)
						System.out.println("e[" + i + "]=" + e[i]);
					throw new RuntimeException("Cannot find merge src or dest");
				}

				// merge e[mergeWhat] into e[mergeTo], and recalculate the affected area_inc[][] values
				e[mergeTo].merge(e[mergeWhat]);
				e[mergeWhat] = null;
				for (int i = 0; i < mergeTo; i++) if (e[i] != null)
					area_inc[i][mergeTo] = e[i].boundary.areaIncreaseIfAdded(e[mergeTo].boundary);
				for (int j = mergeTo + 1; j < nItems; j++) if (e[j] != null)
					area_inc[mergeTo][j] = e[mergeTo].boundary.areaIncreaseIfAdded(e[j].boundary);
			}

			// find the first of the remaining expressions and move it to e[0]
			int i;
			for (i = 0; i < nItems; i++) {
				if (e[i] != null) {
					if (i != 0) {
						e[0] = e[i];
						e[i] = null;
					}
					break;
				}
			}
			// find the second remaining expression and move it to e[1]
			for (i++; i < nItems; i++) {
				if (e[i] != null) {
					if (i != 1) {
						e[1] = e[i];
						e[i] = null;
					}
				}
			}
			// reduce unneeded wrappers
			DBNode a = reduce(e[0]);
			DBNode b = reduce(e[1]);
			e = new DBNode[2];
			e[0] = a;
			e[1] = b;
			// done
			return e;
		}

		public long getID() {
			if (id == -1) {
				childIDs = ByteBuffer.allocate(8 * size());
				if (isLeaf) {
					for (Iterator<Queueable> it = children.iterator(); it.hasNext(); ) 
						childIDs.putLong(((DBString)it.next()).id);
				}
				else {
					for (Iterator<Queueable> it = children.iterator(); it.hasNext(); ) 
						childIDs.putLong(((DBNode)it.next()).getID());
				}

				int mandatory_pfx = 0;
				for (Iterator<LetterSet> it = boundary.letters.iterator(); it.hasNext(); ) {
					if (it.next().isOptional)
						break;
					mandatory_pfx++;
				}

				StringBuilder sb = new StringBuilder();
				for (Iterator<LetterSet> it = boundary.letters.iterator(); it.hasNext(); ) {
					LetterSet ls = it.next();
					for (Iterator<Character> itc = ls.letter.iterator(); itc.hasNext(); ) {
						Character c = itc.next();
						if (c != Helper.EMPTY_CHAR)
							sb.append(c);
					}
					if (it.hasNext())
						sb.append('|');

				}
				try {
				qNewNode.setInt(1, isLeaf ? 0 : 1); // ?1 = ntype
				qNewNode.setString(2, sb.toString());
				qNewNode.setInt(3, mandatory_pfx);
				qNewNode.setBytes(4, childIDs.array()); // ?4 = children

				if (qNewNode.executeUpdate() == 1) {
					ResultSet keys = qNewNode.getGeneratedKeys();
					if (keys.next())
						id = keys.getLong(1);
				}
				if (id < 0)
					throw new RuntimeException("Creating node failed, no ID obtained.");
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			return id;
		}

		public void enqueueChildren(ResultCollector coll) {
			childIDs.position(0);
			int nIds = childIDs.remaining() / Long.BYTES;
			if (isLeaf) {
				for (int i = 0; i < nIds; i++)
					coll.addResult(new DBString(childIDs.getLong()));
			}
			else {
				for (int i = 0; i < nIds; i++)
					coll.addResult(new DBNode(childIDs.getLong()));
			}
		}
	}
}
