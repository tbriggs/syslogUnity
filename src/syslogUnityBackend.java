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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.miscellaneous.PatternAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.FSDirectory;


class syslogUnityBackend {

    public static void main(String[] args) {

        final File INDEX_DIR = new File("/var/lib/syslogUnity/index");
        final PatternAnalyzer analyzer = new PatternAnalyzer(Version.LUCENE_30, Pattern.compile("\\W+"), true, null);
        final IndexWriter writer;

        try {
            writer = new IndexWriter(FSDirectory.open(INDEX_DIR), analyzer, IndexWriter.MaxFieldLength.LIMITED);
        } catch (IOException ex) {
            System.out.print("IOException: " + ex + "\n");
            return;
        }
        writer.setRAMBufferSizeMB(8);

        BlockingQueue<org.w3c.dom.Document> q = new LinkedBlockingQueue<org.w3c.dom.Document>();

        syslogReceive logServer = new syslogReceive(q);
        syslogProcess logStore1 = new syslogProcess(q, writer);
        syslogProcess logStore2 = new syslogProcess(q, writer);
        syslogProcess logStore3 = new syslogProcess(q, writer);
        syslogProcess logStore4 = new syslogProcess(q, writer);
        syslogProcess logStore5 = new syslogProcess(q, writer);
        //searchServer logSearch = new searchServer(writer, analyzer);

        new Thread(logServer).start();
        new Thread(logStore1).start();
        new Thread(logStore2).start();
        new Thread(logStore3).start();
        new Thread(logStore4).start();
        new Thread(logStore5).start();
        //new Thread(logSearch).start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    loopControl.test = false;
                    System.out.print("Exiting gracefully...\n");
                } catch (Exception ex) {
                    System.out.print("Exception:" + ex + "\n");
                }
            }
        });
    }
}


