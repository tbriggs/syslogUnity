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

import org.apache.lucene.analysis.miscellaneous.PatternAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class syslogSearch implements Runnable {

    private PatternAnalyzer analyzer;
    private IndexWriter writer;
    private Socket searchSocket;

    syslogSearch(Socket s, IndexWriter w, PatternAnalyzer a) {
        writer = w;
        analyzer = a;
        searchSocket = s;
    }

    public void run() {

        try {

            String searchQuery =
                    (new BufferedReader(new InputStreamReader(searchSocket.getInputStream()))).readLine().trim();

            IndexReader reader = writer.getReader();
            Searcher searcher = new IndexSearcher(reader);

            QueryParser indexParser = new QueryParser(Version.LUCENE_30, "data", new StandardAnalyzer(Version.LUCENE_30));

            SortField hitSortField = new SortField("date", SortField.LONG);
            Sort hitSort = new Sort(hitSortField);

            TopFieldDocs hits = searcher.search(indexParser.parse(searchQuery), null, 1000, hitSort);

            PrintWriter searchReply = new PrintWriter(searchSocket.getOutputStream(), true);

            searchReply.println(hits.totalHits + " Hits for " + searchQuery);

            for (int i = 0; i < hits.totalHits; i++) {
                Document document = searcher.doc(hits.scoreDocs[i].doc);

                String host = document.get("hostname");
                String date = document.get("date");
                String data = document.get("data");

                searchReply.print("host: " + host + ", date: " + date + ", data: " + data + "\n\n");
            }

            searchReply.close();
            searcher.close();
            reader.close();
            searchSocket.close();
        } catch (Exception ex) {
            System.out.print("Exception: " + ex + "\n");
        }

    }

}
