//import WordSet;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.ibm.icu.text.Normalizer2;
//import android.icu.ETC

class WordSetTest {
	public static WordSet ws = new WordSet("wordset.db", "deu");
	static Normalizer2 toNFD = Normalizer2.getNFDInstance();

	public static void dumpSizes() {
		ws.dumpSizes();
	}

	public static final void main1(String[] argv) {
		String s1 = "abarbeiten";
		String s2 = "abarbeitung";
		Expression e = new Expression();
		e.add(s1);
		e.add(s2);

		String s = "abblasen";
		float dist = e.distance(s);
		System.out.println("Dist of " + e + " and '" + s + "' = "  + dist);

		System.out.println("\n----\n");
		System.out.println("Dist of '" + s1 + "' and '" + s + "' = "  + Helper.distance(s1, s));
		
		System.out.println("\n----\n");
		System.out.println("Dist of '" + s2 + "' and '" + s + "' = "  + Helper.distance(s2, s));
	}

	public static final void main2(String[] argv) {
		Expression eBad = new Expression();
		eBad.add(new LetterSet("Aa"));
		eBad.add(new LetterSet("brun"));
		eBad.add(new LetterSet("abefghknrstwz"));
		eBad.add(new LetterSet("acdëilnoprstu"));
		eBad.add(new LetterSet("abcdeg̈hiklnorstu·z"));
		eBad.add(new LetterSet("̈ßabcdefghilmnoprstuvw·xz"));
		eBad.add(new LetterSet("abcdefgḧiklmnoprstuw·z"));
		eBad.add(new LetterSet("̈abcdefghiklmnopqrstuv·wz"));
		eBad.add(new LetterSet("̈ß abcdefghiklmnoprstuv·wz"));
		eBad.add(new LetterSet("̈Zabcdefghiklmnoprstuv·wz"));
		eBad.add(new LetterSet("̈abcdefghiklmnopqrstuv·wz"));
		eBad.add(new LetterSet("acdefgḧiklmnoprstuv·w"));
		eBad.add(new LetterSet("acefg̈hiklmnoprstu·wß"));
		eBad.add(new LetterSet("abcefg̈hiklmnprst·w"));
		eBad.add(new LetterSet("acdefgḧiklmnprtu·wzß"));
		eBad.add(new LetterSet("acdëhiklmnorstu·"));
		eBad.add(new LetterSet("abcdeg̈hikmnorstu·zß"));
		eBad.add(new LetterSet("cdegḧilnrtu·"));
		eBad.add(new LetterSet("ceghiklmnrtu·z"));
		eBad.add(new LetterSet("reu·ghikln"));
		eBad.add(new LetterSet("ue·ikn"));
		eBad.add(new LetterSet("ste·gin"));
		eBad.add(new LetterSet("t·gi"));
		eBad.add(new LetterSet("t·"));

		Expression eGood = new Expression();
		eGood.add(new LetterSet("Aa"));
		eGood.add(new LetterSet("bun"));
		eGood.add(new LetterSet("abrsfgwhzk"));
		eGood.add(new LetterSet("arteüiln"));
		eGood.add(new LetterSet("abceg̈iklnorstuz"));
		eGood.add(new LetterSet("abcdefg̈hilmnoprstu·zß"));
		eGood.add(new LetterSet("adefghklmnprtuw·z"));
		eGood.add(new LetterSet("abefghklmnorstuv·wz"));
		eGood.add(new LetterSet(" adegiklmnoprst·zß"));
		eGood.add(new LetterSet("abcdefghlmnorstu·Z"));
		eGood.add(new LetterSet("abefghiklmnpqrstu·"));
		eGood.add(new LetterSet("acefg̈hilorstuv·w"));
		eGood.add(new LetterSet("arse·̈hlnoß"));
		eGood.add(new LetterSet("rstf·gẅkl"));
		eGood.add(new LetterSet("te·izlß"));
		eGood.add(new LetterSet("arste·in"));
		eGood.add(new LetterSet("abcdt·io"));
		eGood.add(new LetterSet("c·ḧin"));
		eGood.add(new LetterSet("t·ghk"));
		eGood.add(new LetterSet("e·kn"));
		eGood.add(new LetterSet("e·i"));
		eGood.add(new LetterSet("st·i"));
		eGood.add(new LetterSet("t·"));

		String s = "abblasen";
		System.out.println("bad dist: " + eBad.distance(s));
		System.out.println(eBad);
		System.out.println("good dist: " + eGood.distance(s));
		System.out.println(eGood);
	}


	public static final void main(String[] argv) {
		String fileName = argv[0];

		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				String word = toNFD.normalize(line.trim());

				System.out.println("" + ws.levels + " Input='" + word + "'");
				ws.add(word);
			}   

			bufferedReader.close();         
		}
		catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + fileName + "'");                
		}
		catch (IOException ex) {
			System.out.println("Error reading file '" + fileName + "'");                  
		}
		//System.out.println("Tree=\n" + ws); 
		ws.nodesToDB();

		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
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

			{
				WordSet.ResultListener listener =
					new WordSet.ResultListener() {
						int n = 0;
						public boolean foundResult(String s, float dist) {
							System.out.println("n=" + n + ",dist=" + dist + ", s='" + s + "'");
							n++;
							return n < 10;
						}
					};
				ws.search(word, listener);
			}
		}
	}
}
