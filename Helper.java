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

public class Helper {
	static final float WEIGHT_DIACRITIC = 0.1f;
	static final float WEIGHT_CASE      = 0.5f;
	static final float WEIGHT_DIACRITIC_MIX = 100f;

	static final char  EMPTY_CHAR       = '\u00b7';

	static boolean isDiacritic(Character c) {
		return UCharacter.hasBinaryProperty(c, UProperty.DIACRITIC);
	}

	static float weight(Character c) {
		if (c == EMPTY_CHAR)
			return 0;
		if (isDiacritic(c))
			return WEIGHT_DIACRITIC;
		return 1;
	}

	public static float distance(char c1, char c2) {
		if (c1 == c2)
			return 0;
		if (UCharacter.toLowerCase(c1) == UCharacter.toLowerCase(c2))
			return WEIGHT_CASE;
		boolean isDia1 = isDiacritic(c1);
		boolean isDia2 = isDiacritic(c2);
		if (!isDia1 && !isDia2) {
			return 1;
		}
		if (isDia1 && isDia2)
			return WEIGHT_DIACRITIC;
		return WEIGHT_DIACRITIC_MIX; // don't replace diacritic with letter, that's an error. force delete and insert instead.
	}

	public static String pChar(char c) {
		return (isDiacritic(c) ? "\u25cc" + c : "") + c;
	}

	public static float distance(String s, String t) {
		int m = s.length();
		int n = t.length();

		// for all i and j, d[i,j] will hold the Levenshtein distance between
		// the first i characters of s and the first j characters of t
		// note that d has (m+1)*(n+1) values
		float[][] d = new float[m + 1][n + 1];

		d[0][0] = 0;

		// source prefixes can be transformed into empty string by dropping all characters
		for (int i = 1; i <= m; i++)
			d[i][0] = d[i - 1][0] + Helper.weight(s.charAt(i - 1)); // cost of deletion

		// target prefixes can be reached from empty source prefix by inserting every character
		for (int j = 1; j <= n; j++)
			d[0][j] = d[0][j - 1] + Helper.weight(t.charAt(j - 1)); // cost of insertion
 
		for (int j = 1; j <= n; j++) {
			char tj = t.charAt(j - 1);
			for (int i = 1; i <= m; i++) {
				char si = s.charAt(i - 1);

				float costDel = d[i - 1][j    ] + Helper.weight(si);
				float costIns = d[i    ][j - 1] + Helper.weight(tj);
				float costSbs = d[i - 1][j - 1] + Helper.distance(tj, si); // cost of substitution

				d[i][j] = Float.min(costSbs, Float.min(costDel, costIns));
			}
		}

		// dump the matrix
		if (false) {
			System.out.format("%12s", "");
			for (int i = 1; i <= m; i++)
				System.out.format("%6s", Helper.pChar(s.charAt(i - 1)));
			System.out.println();
			for (int j = 0; j <= n; j++) {
				if (j == 0)
					System.out.format("%6s", "");
				else
					System.out.format("%6s", Helper.pChar(t.charAt(j - 1)));
				for (int i = 0; i <= m; i++)
					System.out.format("%6.1f", d[i][j]);
				System.out.println();
			}
		}

		// dump the process
		if (false) {
			int i = m, j = n;
			while ((i > 0) || (j > 0)) {
				float costDel = (i > 0)              ? d[i - 1][j    ] : Float.POSITIVE_INFINITY;
				float costIns = (j > 0)              ? d[i    ][j - 1] : Float.POSITIVE_INFINITY;
				float costSbs = ((i > 0) && (j > 0)) ? d[i - 1][j - 1] : Float.POSITIVE_INFINITY;

				if ((costSbs <= costDel) && (costSbs <= costIns)) {
					i--;
					j--;
					char si = s.charAt(i);
					char tj = t.charAt(j);
					if (si != tj)
						System.out.println("Substitute from '" + pChar(si) + "' to '" + pChar(tj) + "' at pos " + i + ", " + j);
				}
				else if (costDel <= costIns) {
					i--;
					char si = s.charAt(i);
					System.out.println("Delete '" + pChar(si) + "' from pos " + i + ", " + j);
				}
				else {
					j--;
					char tj = t.charAt(j);
					System.out.println("Insert '" + pChar(tj) + "' to pos " + i + ", " + j);
				}
			}
		}
		return d[m][n];
	}

}
