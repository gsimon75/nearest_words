//import WordSetTrainer;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.ibm.icu.text.Normalizer2;
//import android.icu.ETC
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;

class WordSetTrainer extends WordSet {

	public static final void main(String[] argv) { new WordSetTrainer(argv); }

	WordSetTrainer(String[] argv) {
		connectDB("wordset.db");
		long langId = createLang("deu");
		uploadWordsFile(argv[0], langId);
		buildRtreeIndex(langId);
	}

	long createLang(String langCode) {
		try {
			PreparedStatement qLang = db.prepareStatement("INSERT INTO lang_t (isocode) VALUES (?1)", Statement.RETURN_GENERATED_KEYS);
			qLang.setString(1, langCode);
			long id = -1;
			if (qLang.executeUpdate() == 1) {
				ResultSet keys = qLang.getGeneratedKeys();
				if (keys.next())
					id = keys.getLong(1);
			}
			if (id < 0)
				throw new RuntimeException("Creating lang failed, no ID obtained.");
			return id;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void uploadWordsFile(String fileName, long langId) {
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;

			while ((line = bufferedReader.readLine()) != null) {

			}   

			bufferedReader.close();         
			db.commit();
		}
		catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + fileName + "'");                
		}
		catch (IOException ex) {
			System.out.println("Error reading file '" + fileName + "'");                  
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public int levels = 1;
	void buildRtreeIndex(long langId) {
		DBNode root = new DBNode(false);

		try {
			PreparedStatement qAllWords = db.prepareStatement("SELECT id, value FROM word_t WHERE lang=?1");
			qAllWords.setLong(1, langId);
			ResultSet rs = qAllWords.executeQuery();
			while (rs.next()) {
				System.out.println("Indexing '" + rs.getString(2) + "'");
				DBString ws = new DBString(rs.getLong(1), rs.getString(2), langId);
				root.add(ws);
				if (root.needsSplit()) {
					DBNode[] newNodes = root.split();
					root.add(newNodes[0]);
					root.add(newNodes[1]);
					levels++;
				}
			}

			// TODO: zap old node tree if exists - not easy, needs traversal
			qLangRootNode.setLong(1, langId);
			qLangRootNode.setLong(2, root.getID());
			if (qLangRootNode.executeUpdate() != 1)
				throw new SQLException("Cannot update lang root node id");
			db.commit();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
