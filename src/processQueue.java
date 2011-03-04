import java.io.File;
import java.util.regex.Pattern;
import java.lang.Thread;

import com.sleepycat.je.*;

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

    private static Environment env;
    private static Database queue;
    private static Cursor cursor;


    public static void main(String[] args) throws Exception {

        final File INDEX_DIR = new File("indexDB/");

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        envConfig.setSharedCache(true);
        envConfig.setReadOnly(true);
        envConfig.setCacheSize(20971520);

        env = new Environment(dbEnvDir, envConfig);

        DatabaseConfig config = new DatabaseConfig();
        config.setAllowCreate(false);
        config.setReadOnly(true);

        queue = env.openDatabase(null, "logQueue", config);

        LongEntry kdbt = new LongEntry();
        StringEntry ddbt = new StringEntry();

        CursorConfig curConfig = new CursorConfig();
        curConfig.setReadUncommitted(true);
        cursor = queue.openCursor(null, curConfig);
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

        long cursorPos = kdbt.getLong();
        while (true) {
            check = cursor.getNext(kdbt, ddbt, null);
            if (check == OperationStatus.SUCCESS) {
                indexLine(kdbt, ddbt);
                cursorPos = kdbt.getLong();
                //System.out.print("key: " + kdbt.getRecordNumber() + " data: " + ddbt.getString() + "\n");
            } else if (check == OperationStatus.NOTFOUND) {
                Thread.sleep(10);
                do {
                  cursor.close();
                  queue.close();
                  env.close();
                  env = new Environment(dbEnvDir, envConfig);
                  queue = env.openDatabase(null, "logQueue", config);
                  cursor = queue.openCursor(null, curConfig);
                  kdbt.setLong(cursorPos+1);
                } while (cursor.getSearchKey(kdbt,ddbt,null) != OperationStatus.SUCCESS);

                indexLine(kdbt, ddbt);
                cursorPos = kdbt.getLong();
            }
        }

//    cursor.close();
//    queue.close();
    }

    private static void indexLine(LongEntry key, StringEntry record) {
        String[] arr = record.getString().split("##FD##");
        System.out.print("key:" + key.getLong() + "\nDate:" + arr[0] + "\nPri:" + arr[1] + "\nIP:" + arr[2] + "\nData:\n" + arr[3] + "\n");
//    Document doc = new Document();
//    doc.add
//    writer.addDocument(doc);
    }
}