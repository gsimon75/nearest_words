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

/*
   Expression = list of character sets, like '[ap][pel][pau][lrm][e·]' for 'apple', 'pear', 'plum'
   It covers more than the actual words, but then behaves like a 'bounding box' for them.
   Accents are separated from the characters, so for example 'á' is actually two characters:
   'a' and '◌́' (0x61 and 0x0301).
   By calculating weight, a diacritic-only set counts with a different value, just as
   for distance calculations an insertion or deletion of a diacritic mark.
   For replacement costs, however, while a diacritic-to-diacritic change is also cheap,
   a replacement between a diacritic and a normal character is considered expensive.
 */
public class Expression {
	ArrayList<LetterSet> letters;
	double areaCached; // NOTE: to prevent overflowing Float, store the log of it

	Expression() { letters = new ArrayList<LetterSet>(); areaCached = 0; } 
	Expression(int n) { letters = new ArrayList<LetterSet>(n); areaCached = 0; } 
	Expression(Expression other) { this(other, false); }  // shallow
	Expression(Expression other, boolean deep) {
		if (deep) {
			letters = new ArrayList<LetterSet>(other.letters.size());
			for (Iterator<LetterSet> it = other.letters.iterator(); it.hasNext(); )
				letters.add(it.next().createDeepCopy());
		}
		else {
			letters = new ArrayList<LetterSet>(other.letters);
		}
		areaCached = other.areaCached;
	}

	Expression(String s, int mandatory_prefix_len) {
		int idx = 0;
		int nLetters = 0;
		while (idx >= 0) {
			nLetters++;
			idx = s.indexOf("|", idx + 1);
		}
		letters = new ArrayList<LetterSet>(nLetters);
		areaCached = -1f;
		idx = 0;
		int sLen = s.length();
		for (int i = 0; i < nLetters; i++) {
			letters.add(new LetterSet());
			while (idx < sLen) {
				char c = s.charAt(idx);
				idx++;
				if (c == '|')
					break;
				letters.get(i).add(c);
			}
			if (i >= mandatory_prefix_len)
				letters.get(i).add(Helper.EMPTY_CHAR);
		}
	}
	void add(LetterSet ls) { letters.add(ls); } // for debug only
	Expression createDeepCopy() { return new Expression(this, true); }
	public void clear() { letters.clear(); areaCached = 0; }
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("'");
		Iterator<LetterSet> it = letters.iterator();
		while (it.hasNext())
			sb.append(it.next());
		sb.append("'");
		return sb.toString();
	}

	double area() {
		if (areaCached < 0) {
			//areaCached = 0;
			areaCached = 1;
			for (Iterator<LetterSet> it = letters.iterator(); it.hasNext(); ) {
				LetterSet ls = it.next();
				//areaCached += Math.log(ls.weight());
				areaCached *= ls.weight();
				if (!Double.isFinite(areaCached))
					throw new RuntimeException("areaCached got transfinite");
			}
		}
		return areaCached;
	}

	public float distance_naive_but_suboptimal(String s) {
		int nLetters = letters.size();
		int nOtherLetters = s.length();
		int nMin = Math.min(nLetters, nOtherLetters);

		float totalDist = 0;
		for (int i = 0; i < nMin; i++)
			totalDist += letters.get(i).distance(s.charAt(i));
		for (int i = nMin; i < nOtherLetters; i++)
			totalDist += Helper.weight(s.charAt(i));

		return totalDist;
	}

	public float distance(String t) {
		boolean debug = false;
		int m = letters.size();
		int n = t.length();

		// for all i and j, d[i,j] will hold the Levenshtein distance between
		// the first i characters of s and the first j characters of t
		// note that d has (m+1)*(n+1) values
		float[][] d = new float[m + 1][n + 1];

		d[0][0] = 0;

		// source prefixes can be transformed into empty string by dropping all characters
		for (int i = 1; i <= m; i++)
			d[i][0] = d[i - 1][0] + letters.get(i - 1).minWeight(); // cost of deletion

		// target prefixes can be reached from empty source prefix by inserting every character
		for (int j = 1; j <= n; j++)
			d[0][j] = d[0][j - 1] + Helper.weight(t.charAt(j - 1)); // cost of insertion
 
		if (debug) {
			for (int i = 0; i < m; i++) {
				LetterSet si = letters.get(i);
				System.out.println("letters[" + i  + "] = " + si);
				System.out.println("   weight=" + si.weight() + ", minWeight=" + si.minWeight() +
						", isOptional=" + si.isOptional + ", containsDiacritic=" + si.containsDiacritic + ", containsNonDiacritic=" +
						si.containsNonDiacritic);
			}
		}

		for (int j = 1; j <= n; j++) {
			char tj = t.charAt(j - 1);
			for (int i = 1; i <= m; i++) {
				LetterSet si = letters.get(i - 1);

				float costDel = d[i - 1][j    ] + si.minWeight();
				float costIns = d[i    ][j - 1] + Helper.weight(tj);
				float costSbs = d[i - 1][j - 1] + si.distance(tj); // cost of substitution

				d[i][j] = Float.min(costSbs, Float.min(costDel, costIns));
			}
		}

		// dump the matrix
		if (debug) {
			System.out.format("%12s", "");
			for (int i = 1; i <= m; i++)
				System.out.format("%6s", letters.get(i - 1));
			System.out.println();
			for (int j = 0; j <= n; j++) {
				if (j == 0)
					System.out.format("%6s", "");
				else
					System.out.format("%6s", t.charAt(j - 1));
				for (int i = 0; i <= m; i++)
					System.out.format("%6.1f", d[i][j]);
				System.out.println();
			}
		}

		// dump the process
		if (debug) {
			int i = m, j = n;
			while ((i > 0) || (j > 0)) {
				float costDel = (i > 0)              ? d[i - 1][j    ] : Float.POSITIVE_INFINITY;
				float costIns = (j > 0)              ? d[i    ][j - 1] : Float.POSITIVE_INFINITY;
				float costSbs = ((i > 0) && (j > 0)) ? d[i - 1][j - 1] : Float.POSITIVE_INFINITY;

				if ((costSbs <= costDel) && (costSbs <= costIns)) {
					i--;
					j--;
					String si = letters.get(i).toString();
					char tj = t.charAt(j);
					if (!letters.get(i).contains(tj)) {
						System.out.println("Substitute from '" + si + "' to '" + tj + "' at pos " + i + ", " + j);
					}
				}
				else if (costDel <= costIns) {
					i--;
					if (!letters.get(i).contains(Helper.EMPTY_CHAR)) {
						String si = letters.get(i).toString();
						System.out.println("Delete '" + si + "' from pos " + i + ", " + j);
					}
				}
				else {
					j--;
					char tj = t.charAt(j);
					System.out.println("Insert '" + tj + "' to pos " + i + ", " + j);
				}
			}
		}
		return d[m][n];
	}


	/*
	   Add to the current expression
	 */
	void add(String s) {
		int sLen = s.length();
		if (letters.isEmpty()) {
			for (int i = letters.size(); i < sLen; i++)
				letters.add(new LetterSet());
		}
		else {
			for (int i = letters.size(); i < sLen; i++) {
				LetterSet ls = new LetterSet();
				ls.add(Helper.EMPTY_CHAR);
				letters.add(ls);
			}
		}
		for (int i = 0; i < sLen; i++)
			letters.get(i).add(new Character(s.charAt(i)));
		for (int i = letters.size() - 1; i >= sLen; i--)
			letters.get(i).add(Helper.EMPTY_CHAR);
		areaCached = -1f;
	}

	void add(Expression other) {
		//System.out.print("Adding " + other + " to " + this);
		int nLetters = letters.size();
		int nOtherLetters = other.letters.size();
		int nMax = Math.max(nLetters, nOtherLetters);
		if (letters.isEmpty()) {
			for (int i = nLetters; i < nMax; i++)
				letters.add(new LetterSet());
		}
		else {
			for (int i = nLetters; i < nMax; i++) {
				LetterSet ls = new LetterSet();
				ls.add(Helper.EMPTY_CHAR);
				letters.add(ls);
			}
		}
		for (int i = 0; i < nOtherLetters; i++)
			letters.get(i).add(other.letters.get(i));
		for (int i = nLetters - 1; i >= nOtherLetters; i--)
			letters.get(i).add(Helper.EMPTY_CHAR);
		areaCached = -1f;
		//System.out.println(": result=" + this);
	}

	double areaIncreaseIfAdded(Expression other) {
		// FIXME: naive algorithm, may be worth refactoring for some more effective
		Expression temp = createDeepCopy();
		temp.add(other);
		double diff = temp.area() - area();
		//System.out.println("areaIncreaseIfAdded; this=" + this + ", candidate=" + temp + ", diff=" + diff);
		return diff;
	}

	double areaIncreaseIfAdded(String s) {
		// FIXME: naive algorithm, may be worth refactoring for some more effective
		Expression temp = createDeepCopy();
		temp.add(s);
		double diff = temp.area() - area();
		return diff;
	}

	int size() { return letters.size(); }
}


