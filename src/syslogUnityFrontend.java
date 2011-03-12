import java.awt.*;
import java.io.*;
//import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.miscellaneous.PatternAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.IndexReader;
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

        //final PatternAnalyzer analyzer = new PatternAnalyzer(Version.LUCENE_30, Pattern.compile("\\W+"), true, null);

        IndexReader reader = IndexReader.open(FSDirectory.open(new File(index)), true);
        Searcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);
        CheckIndex check = new CheckIndex(FSDirectory.open(new File(index)));

        CheckIndex.Status res = check.checkIndex();

        System.out.print("Dir: " + res.dir + "\n" +
                         "Clean: " + res.clean + "\n" +
                         "seg:" + res.numSegments + " bad:" + res.numBadSegments + "\n" +
                         "format: " + res.segmentFormat + "\n");


        // Parse a simple query that searches for "text":
        QueryParser parser = new QueryParser(Version.LUCENE_30, "priority", analyzer);

        System.out.println("Enter query: ");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        //String input = in.readLine();

        //input = input.trim();

        String input = "spamhaus";

        Query query = parser.parse(input);

        TopDocs hits = searcher.search(query, 1000);

        System.out.print(hits.totalHits + " Hits for '" + query.toString() + "'\n");

        // Iterate through the results:
        for (int i = 0; i < hits.totalHits; i++) {
            Document hitDoc = searcher.doc(hits.scoreDocs[i].doc);
            String data = hitDoc.get("priority");
            System.out.print("Matched in: " + data + "\n");
        }


        reader.close();
    }

}