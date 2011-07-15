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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;

import java.lang.*;
import java.net.*;

class searchServer implements Runnable {

    private StandardAnalyzer analyzer;
    private IndexWriter writer;

    searchServer(IndexWriter w, StandardAnalyzer a) {
        writer = w;
        analyzer = a;
    }

    public void run() {

        try {
            ServerSocket searchListener = new ServerSocket(1228);
            Socket searchSocket;
            int i = 0;

            while ((i++ < 32)) {
                searchSocket = searchListener.accept();
                syslogSearch searchInstance = new syslogSearch(searchSocket, writer, analyzer);
                Thread searchThread = new Thread(searchInstance);
                searchThread.start();
            }

            searchListener.close();
        } catch (Exception e) {
            System.out.print("Whoops! It didn't work!\n");
        }
    }
}
