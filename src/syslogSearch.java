/*
 * This file is part of syslogUnity.
 *
 *     syslogUnity is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     syslogUnity is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with syslogUnity.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;

import java.io.BufferedReader;
import java.io.InputStreamReader;

class syslogSearch implements Runnable {

    private QueryParser parser;
    private IndexWriter writer;

    syslogSearch(IndexWriter w, QueryParser p) {
        writer = w;
        parser = p;
    }

    public void run() {

        System.out.print("Enter query: ");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        try {
            IndexReader reader = writer.getReader();
            IndexSearcher searcher = new IndexSearcher(reader);
            String input = in.readLine();

            input = input.trim();

            Query query = parser.parse(input);

            TopScoreDocCollector collector = TopScoreDocCollector.create(25, true);
            searcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            System.out.println("Found " + hits.length + " hits.");
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                System.out.println((i + 1) + ". " + d.get("data"));
            }
        } catch (Exception ex) {
            System.out.print("Exception: " + ex + "\n");
        }

    }

}
