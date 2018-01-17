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

public abstract class WordSetNode implements WordSet.Queueable {

	Expression boundary = new Expression();

	abstract int size();
	abstract boolean needsSplit();
	abstract WordSetNode[] explode();

	public void add(WordSetNode n) {
		boundary.add(n.boundary);
	}

	public void add(String s) {
		boundary.add(s);
	}

	public float distance(String s) {
		return boundary.distance(s);
	}

	public String toString() { return boundary.toString(); }

	// Split the node, return the 2 results in an array
	// NOTE: this instance is cleared in the process
	WordSetNode[] split() {
		int nItems = size();
		WordSetNode[] e = explode();

		// calculate a cost matrix (NOTE: only the upper half is used)
		float area_inc[][] = new float[nItems][nItems];
		for (int i = 0; i < (nItems - 1); i++)
			for (int j = i + 1; j < nItems; j++) {
				//System.out.println("Considering adding " + e[j] + " to " + e[i]);
				area_inc[i][j] = e[i].boundary.areaIncreaseIfAdded(e[j].boundary);
			}

		for (int nRemaining = nItems; nRemaining > 2; nRemaining--) {
			//System.out.println("Remaining " + nRemaining + " of " + nItems);
			// find the pair whose merge would result in a less increase of area,
			// and break the ties by choosing the smallest one
			float leastAreaIncrease = Float.POSITIVE_INFINITY;
			float leastArea = Float.POSITIVE_INFINITY;
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
			//System.out.println(Dump.indent + "Merge before: " + e[mergeTo]);
			//System.out.println(Dump.indent + "      what  : " + e[mergeWhat]);
			e[mergeTo].add(e[mergeWhat]);
			//System.out.println(Dump.indent + "      after : " + e[mergeTo]);
			e[mergeWhat] = null;
			//System.out.println("Fixing the matrix");
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
		return e;
	}

}
