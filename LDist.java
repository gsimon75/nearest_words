import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
//import android.icu.ETC

// NOTE: download ICU from: http://site.icu-project.org/download/60#TOC-ICU4J-Download

class LDist {
	LDist() {}

	static final float WEIGHT_DIACRITIC = 0.1f;
	static final float WEIGHT_CASE      = 0.5f;

	public static boolean isDiacritic(char c) {
		return UCharacter.hasBinaryProperty(c, UProperty.DIACRITIC);
	}

	public static float costDeletion(char c) {
		return isDiacritic(c) ? WEIGHT_DIACRITIC : 1;
	}

	public static float costInsertion(char c) {
		return isDiacritic(c) ? WEIGHT_DIACRITIC : 1;
	}

	public static float costSubstitution(char c1, char c2) {
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
		return 100; // don't replace diacritic with letter, that's an error. force delete and insert instead.
	}

	public static String pChar(char c) {
		return (isDiacritic(c) ? "\u25cc" + c : "") + c;
	}

	public static float LevenshteinDistance(String s, String t) {
		int m = s.length();
		int n = t.length();

		// for all i and j, d[i,j] will hold the Levenshtein distance between
		// the first i characters of s and the first j characters of t
		// note that d has (m+1)*(n+1) values
		float[][] d = new float[m + 1][n + 1];

		d[0][0] = 0;

		// source prefixes can be transformed into empty string by dropping all characters
		for (int i = 1; i <= m; i++)
			d[i][0] = d[i - 1][0] + costDeletion(s.charAt(i - 1));

		// target prefixes can be reached from empty source prefix by inserting every character
		for (int j = 1; j <= n; j++)
			d[0][j] = d[0][j - 1] + costInsertion(t.charAt(j - 1));
 
		for (int j = 1; j <= n; j++) {
			char tj = t.charAt(j - 1);
			for (int i = 1; i <= m; i++) {
				char si = s.charAt(i - 1);

				float costDel = d[i - 1][j    ] + costDeletion(si);
				float costIns = d[i    ][j - 1] + costInsertion(tj);
				float costSbs = d[i - 1][j - 1] + costSubstitution(tj, si);

				d[i][j] = Float.min(costSbs, Float.min(costDel, costIns));
			}
		}

		// dump the matrix
		System.out.format("%12s", "");
		for (int i = 1; i <= m; i++)
			System.out.format("%6s", pChar(s.charAt(i - 1)));
		System.out.println();
		for (int j = 0; j <= n; j++) {
			if (j == 0)
				System.out.format("%6s", "");
			else
				System.out.format("%6s", pChar(t.charAt(j - 1)));
			for (int i = 0; i <= m; i++)
				System.out.format("%6.1f", d[i][j]);
			System.out.println();
		}

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
		return d[m][n];
	}

	public static void main(String argv[]) {
		Normalizer2 toNFD = Normalizer2.getNFDInstance();
		System.out.println(LevenshteinDistance(toNFD.normalize(argv[0]), toNFD.normalize(argv[1])));
	}
}

