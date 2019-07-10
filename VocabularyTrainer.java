//import VocabularyTrainer;
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

class VocabularyTrainer extends Vocabulary {

	public static final void main(String[] argv) { new VocabularyTrainer(argv); }

	VocabularyTrainer(String[] argv) {
		connectDB("wordset.db");
		long langId = -1;
		long rootNodeId = -1;
		String langCode = "deu";
		try {
			qGetLang.setString(1, langCode);
			ResultSet rs = qGetLang.executeQuery();
			if (rs.next()) {
				langId = rs.getLong(1);
				rootNodeId = rs.getLong(2);
			}
			else {
				qNewLang.setString(1, langCode);
				if (qNewLang.executeUpdate() == 1) {
					rs = qNewLang.getGeneratedKeys();
					if (rs.next())
						langId = rs.getLong(1);
				}
				if (langId == -1)
					throw new RuntimeException("Creating lang failed, no ID obtained.");
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		uploadWordsFile(argv[0], langId);
		buildRtreeIndex(langId, rootNodeId);
	}

	void uploadWordsFile(String fileName, long langId) {
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				System.out.println("Input: " + line);
				DBString s = new DBString(line, langId);
				s.getID();
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
	void buildRtreeIndex(long langId, long oldRootNodeId) {
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

			if (oldRootNodeId != -1)
				new DBNode(oldRootNodeId).removeFromDB();
			qLangSetRootNode.setLong(1, langId);
			qLangSetRootNode.setLong(2, root.getID());
			if (qLangSetRootNode.executeUpdate() != 1)
				throw new SQLException("Cannot update lang root node id");
			db.commit();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
