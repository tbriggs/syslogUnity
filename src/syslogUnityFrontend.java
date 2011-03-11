import java.io.*;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.miscellaneous.PatternAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Simple command-line based search demo.
 */
public class syslogUnityFrontend {

    private syslogUnityFrontend() {
    }

    public static void main(String[] args) throws Exception {

        String index = "/var/lib/syslogUnity/index";

        final PatternAnalyzer analyzer = new PatternAnalyzer(Version.LUCENE_30, Pattern.compile("\\W+"), true, null);

        IndexSearcher isearcher = new IndexSearcher(FSDirectory.open(new File(index)));

        // Parse a simple query that searches for "text":
        QueryParser parser = new QueryParser(Version.LUCENE_30, "data", analyzer);

        System.out.println("Enter query: ");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        String input = in.readLine();

        input = input.trim();

        Query query = parser.parse(input);
        TopDocs hits = isearcher.search(query, 100);

        // Iterate through the results:
        for (int i = 0; i < hits.totalHits; i++) {
            Document hitDoc = isearcher.doc(hits.scoreDocs[i].doc);
            String id = hitDoc.get("id");
            System.out.print("Matched in: " + id + "\n");
        }

        isearcher.close();
    }
}