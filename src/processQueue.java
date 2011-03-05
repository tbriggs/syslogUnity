import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.regex.Pattern;
import java.lang.Thread;

import com.sleepycat.db.*;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.miscellaneous.PatternAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.FSDirectory;

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

public class processQueue {
    private static File dbEnvDir = new File("/var/lib/syslogUnity/queueDB/");
    private static IndexWriter writer = null;
    private static PatternAnalyzer analyzer = null;


    public static void main(String[] args) throws Exception {

        final File INDEX_DIR = new File("indexDB/");

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        envConfig.setInitializeCache(true);
        envConfig.setCacheSize(20971520);

        final Environment env = new Environment(dbEnvDir, envConfig);

        DatabaseConfig queueConfig = new DatabaseConfig();
        queueConfig.setType(DatabaseType.QUEUE);
        queueConfig.setAllowCreate(false);
        queueConfig.setReadOnly(false);
        queueConfig.setPageSize(4096);
        queueConfig.setRecordLength(1024);

        final Database queue = env.openDatabase(null, "logQueue.db", null, queueConfig);

        IntEntry kdbt = new IntEntry();
        StringEntry ddbt = new StringEntry();
        final Cursor cursor;
        cursor = queue.openCursor(null, null);
        OperationStatus check;

        PatternAnalyzer analyzer = new PatternAnalyzer(Version.LUCENE_30, Pattern.compile("\\W+"), true, null);
        IndexWriter writer = new IndexWriter(FSDirectory.open(INDEX_DIR), analyzer, IndexWriter.MaxFieldLength.LIMITED);
        writer.setRAMBufferSizeMB(8);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    cursor.close();
                    queue.close();
                    env.close();
                    System.out.print("Closed DB Gracefully...\n");
                } catch (Exception dbe) {
                    System.out.print("Caught SIGINT, couldn't close queueDB successfully\n");
                }
            }
        });

        check = cursor.getFirst(kdbt, ddbt, null);

        if (check == OperationStatus.SUCCESS) {
            indexLine(kdbt, ddbt);
        } else {
            System.out.print("Error on first read: " + check.toString() + "\n");
        }

        while (true) {

            check = cursor.getNext(kdbt, ddbt, null);

            if (check == OperationStatus.SUCCESS) {
                indexLine(kdbt, ddbt);
                //System.out.print("key: " + kdbt.getRecordNumber() + " data: " + ddbt.getString() + "\n");
            } else if (check == OperationStatus.NOTFOUND) {

            }
        }

//    cursor.close();
//    queue.close();
    }

    private static void indexLine(IntEntry key, StringEntry record) {
        recordStruct queueRecord = new recordStruct(record.getData());

        Date queueDate = new Date(queueRecord.getEpoch());
        int queuePriority = queueRecord.getPriority();
        InetAddress queueHost;
        String queueLogLine = queueRecord.getLogLine();

        try {
            queueHost = InetAddress.getByAddress(queueRecord.getHost());
        } catch (Exception UnknownHostException) {
            return;
        }

        System.out.print("key:" + key.getInt() +
                         "\nDate:" + queueDate.toString() +
                         "\nPri:" + queuePriority +
                         "\nIP:" + queueHost.toString() +
                         "\nData:\n" + queueRecord.logLineLength + queueLogLine + "\n");
//    Document doc = new Document();
//    doc.add
//    writer.addDocument(doc);
    }
}