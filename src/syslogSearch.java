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
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Pattern;

class syslogSearch implements Runnable {

    private PatternAnalyzer analyzer;
    private IndexWriter writer;
    private Socket searchSocket;

    final Pattern matchData = Pattern.compile("^data:", Pattern.CASE_INSENSITIVE);
    //final Pattern matchHostname = Pattern.compile("^hostname:", Pattern.CASE_INSENSITIVE);
    //final Pattern matchPriority = Pattern.compile("^priority:", Pattern.CASE_INSENSITIVE);
    //final Pattern matchDateStart = Pattern.compile("^datestart:", Pattern.CASE_INSENSITIVE);
    //final Pattern matchDateEnd = Pattern.compile("^dateend:", Pattern.CASE_INSENSITIVE);

    syslogSearch(Socket s, IndexWriter w, PatternAnalyzer a) {
        writer = w;
        analyzer = a;
        searchSocket = s;
    }

    public void run() {

        try {

            BufferedReader searchInput = new BufferedReader(new InputStreamReader(searchSocket.getInputStream()));

            String[] searchQuery = new String[5];
            int i = 0;

            while (i < 5) {
                searchQuery[i] = searchInput.readLine().trim();
                if (searchQuery[i].equals("\n") ||
                        searchQuery[i].equals("\r\n") ||
                        searchQuery[i].isEmpty())
                    break;
                i++;
            }

            //String hostnameField = null;
            //String priorityField = null;
            //String dateStartField = null;
            //String dateEndField = null;
            String dataField = null;

            //QueryParser hostnameParser = new QueryParser(Version.LUCENE_30, "hostname", analyzer);
            //QueryParser priorityParser = new QueryParser(Version.LUCENE_30, "priority", analyzer);
            //QueryParser dateParser = new QueryParser(Version.LUCENE_30, "date", analyzer);
            QueryParser dataParser = new QueryParser(Version.LUCENE_30, "data", analyzer);


            for (int n = 0; n < i; n++) {
                if (matchData.matcher(searchQuery[n]).find()) {
                    dataField = searchQuery[n].substring(5).trim();
                }

                //else if (matchHostname.matcher(searchQuery[n]).find()) {
                //    hostnameField = searchQuery[n].substring(9).trim();
                //}

                //else if (matchPriority.matcher(searchQuery[n]).find()) {
                //    priorityField = searchQuery[n].substring(9).trim();
                //}

                //else if (matchDateStart.matcher(searchQuery[n]).find()) {
                //    dateStartField = searchQuery[n].substring(10).trim();
                //}

                //else if (matchDateEnd.matcher(searchQuery[n]).find()) {
                //    dateEndField = searchQuery[n].substring(8).trim();
                //}
            }

            PrintWriter searchReply = new PrintWriter(searchSocket.getOutputStream(), true);

            IndexReader reader = writer.getReader();
            IndexSearcher searcher = new IndexSearcher(reader);

            //Query[] queries = new Query[4];
            //queries[0] = hostnameParser.parse(hostnameField);
            //queries[1] = priorityParser.parse(priorityField);
            //queries[2] = dateParser.parse(dateStartField);
            //queries[3] = dataParser.parse(dataField);

            Query query = dataParser.parse(dataField);

            TopScoreDocCollector collector = TopScoreDocCollector.create(100, true);
            searcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            for (int j = 0; j < hits.length; ++j) {
                int docId = hits[j].doc;
                Document d = searcher.doc(docId);
                searchReply.print((j + 1) + ". " + d.get("data"));
            }

            searchReply.close();
            searchSocket.close();
        } catch (Exception ex) {
            System.out.print("Exception: " + ex + "\n");
        }

    }

}
