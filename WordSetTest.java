//import WordSet;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

class WordSetTest {
	public static WordSet ws = new WordSet();

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

	public static final void main(String[] argv) {
		String fileName = argv[0];

		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				String word = line.trim();

				//System.out.println("Input='" + word + "'");
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
			String word = line.trim();

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
