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

        BlockingQueue<recordStruct> q = new LinkedBlockingQueue<recordStruct>();

        syslogReceive logServer = new syslogReceive(q);
        syslogProcess logStore1 = new syslogProcess(q, writer);
        syslogProcess logStore2 = new syslogProcess(q, writer);
        syslogProcess logStore3 = new syslogProcess(q, writer);
        syslogProcess logStore4 = new syslogProcess(q, writer);
        syslogProcess logStore5 = new syslogProcess(q, writer);
        searchServer  logSearch = new searchServer(writer, analyzer);

        new Thread(logServer).start();
        new Thread(logStore1).start();
        new Thread(logStore2).start();
        new Thread(logStore3).start();
        new Thread(logStore4).start();
        new Thread(logStore5).start();
        new Thread(logSearch).start();

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


