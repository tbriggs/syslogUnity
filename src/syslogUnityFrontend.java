import java.io.*;
//import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.miscellaneous.PatternAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.Document;
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


        // Parse a simple query that searches for "text":
        QueryParser parser = new QueryParser(Version.LUCENE_30, "data", analyzer);

        System.out.println("Enter query: ");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        String input = in.readLine();

        input = input.trim();

        Query query = parser.parse(input);

        doSearch(searcher, query);

        /*System.out.print(hits.totalHits + " Hits for '" + query.toString() + "'\n");

        // Iterate through the results:
        for (int i = 0; i < hits.totalHits; i++) {
            Document hitDoc = searcher.doc(hits.scoreDocs[i].doc);
            String data = hitDoc.get("data");
            System.out.print("Matched in: " + data + "\n");
        }
        */

        reader.close();
    }

    public static void doSearch(final Searcher searcher, Query query) {
                Collector streamingHitCollector = new Collector() {
            private Scorer scorer;
            private int docBase;

            // simply print docId and score of every matching document
            @Override
            public void collect(int doc) throws IOException {
                System.out.println("doc=" + doc + docBase + " score=" + scorer.score());
            }

            @Override
            public boolean acceptsDocsOutOfOrder() {
                return true;
            }

            @Override
            public void setNextReader(IndexReader reader, int docBase)
                    throws IOException {
                this.docBase = docBase;
            }

            @Override
            public void setScorer(Scorer scorer) throws IOException {
                this.scorer = scorer;
            }

        };

        try {
            searcher.search(query, streamingHitCollector);
        } catch (IOException ex) {
            System.out.print("IOException: " + ex);
        }
    }
}