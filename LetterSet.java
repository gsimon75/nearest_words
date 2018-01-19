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

public class LetterSet {

	HashSet<Character> letter;
	boolean containsDiacritic = false;
	boolean containsNonDiacritic = false;
	boolean isOptional = false;

	LetterSet() { letter = new HashSet<Character>(); }
	LetterSet(int n) { letter = new HashSet<Character>(n); }
	LetterSet(LetterSet other) { this(other, false); } // shallow copy
	LetterSet(LetterSet other, boolean deep) {
		if (deep) {
			letter = new HashSet<Character>(other.letter.size());
			letter.addAll(other.letter);
		}
		else {
			letter = new HashSet<Character>(other.letter); 
		}
		containsDiacritic = other.containsDiacritic;
		containsNonDiacritic = other.containsNonDiacritic;
		isOptional = other.isOptional;
	}
	LetterSet(String s) { // for debug only
		int n = s.length();
		letter = new HashSet<Character>(n);
		for (int i = 0; i < n; i++)
			letter.add(s.charAt(i));
	}
	LetterSet createDeepCopy() { return new LetterSet(this, true); }
	void clear() {
		letter.clear();
		containsDiacritic = containsNonDiacritic = isOptional = false;
	}
	public boolean contains(Character c) { return letter.contains(c); }

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Iterator<Character> it = letter.iterator(); it.hasNext(); )
			sb.append(it.next());
		sb.append("]");
		return sb.toString();
	}

	float weight() {
		float w = 0;
		for (Iterator<Character> it = letter.iterator(); it.hasNext(); )
			w += Helper.weight(it.next());
		return w;
	}

	float minWeight() {
		if (isOptional)
			return 0;
		if (containsDiacritic)
			return Helper.WEIGHT_DIACRITIC;
		if (containsNonDiacritic)
			return 1;
		return 0;
	}

	float distance(Character c2) {
		if (letter.contains(c2))
			return 0;
		if (Helper.isDiacritic(c2))
			return containsDiacritic ? Helper.WEIGHT_DIACRITIC : Helper.WEIGHT_DIACRITIC_MIX;

		int lc2 = UCharacter.toLowerCase(c2);
		for (Iterator<Character> it1 = letter.iterator(); it1.hasNext(); ) {
			if (UCharacter.toLowerCase(it1.next()) == lc2)
				return Helper.WEIGHT_CASE;
		}
		return 1;
	}

	float distance(LetterSet other) {
		float dMin = Float.POSITIVE_INFINITY;
		for (Iterator<Character> it2 = other.letter.iterator(); it2.hasNext(); ) {
			float d = distance(it2.next());
			if (d < dMin)
				dMin = d;
		}
		return dMin;
	}

	void add(Character c) {
		letter.add(c);
		if (c == Helper.EMPTY_CHAR)
			isOptional = true;
		else if (Helper.isDiacritic(c))
			containsDiacritic = true;
		else
			containsNonDiacritic = true;
	}
	void addAll(Collection<Character> coll) {
		letter.addAll(coll);
		for (Iterator<Character> it2 = coll.iterator(); (!isOptional || !containsDiacritic || !containsNonDiacritic) && it2.hasNext(); ) {
			Character c = it2.next();
			if (c == Helper.EMPTY_CHAR)
				isOptional = true;
			else if (Helper.isDiacritic(c))
				containsDiacritic = true;
			else
				containsNonDiacritic = true;
		}
	}
	void add(LetterSet other) { addAll(other.letter); }

}
