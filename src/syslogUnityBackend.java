import java.io.File;
import java.io.IOException;
import java.net.*;
//import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//import com.sleepycat.je.*;

import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.miscellaneous.PatternAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.FSDirectory;

class syslogUnityBackend {

    public static void main(String[] args) {

 //       final File DB_DIR = new File("/var/lib/syslogUnity/store/");
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

        /*
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(false);
        envConfig.setAllowCreate(true);
        envConfig.setCacheSize(20971520);

        final Environment env = new Environment(DB_DIR, envConfig);

        DatabaseConfig storeConfig = new DatabaseConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setReadOnly(false);

        final Database store = env.openDatabase(null, "store.db", storeConfig);

        SequenceConfig seqConfig = new SequenceConfig();
        seqConfig.setAllowCreate(true);
        DatabaseEntry seqKey = new DatabaseEntry("sequencecounter".getBytes());

        final Sequence seq = store.openSequence(null, seqKey, seqConfig);
        */

        BlockingQueue<recordStruct> q = new LinkedBlockingQueue<recordStruct>();

        syslogReceive logServer = new syslogReceive(q);
        syslogProcess logStore1 = new syslogProcess(q, /*store, seq,*/ writer);
        syslogProcess logStore2 = new syslogProcess(q, /*store, seq,*/ writer);
        syslogProcess logStore3 = new syslogProcess(q, /*store, seq,*/ writer);
        syslogProcess logStore4 = new syslogProcess(q, /*store, seq,*/ writer);
        syslogProcess logStore5 = new syslogProcess(q, /*store, seq,*/ writer);

        new Thread(logServer).start();
        new Thread(logStore1).start();
        new Thread(logStore2).start();
        new Thread(logStore3).start();
        new Thread(logStore4).start();
        new Thread(logStore5).start();

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

class loopControl {
    public static boolean test = true;
}

class syslogReceive implements Runnable {
    int BUFFER_SIZE = 1000;
    private final BlockingQueue<recordStruct> queue;
    DatagramSocket syslog;
    DatagramPacket logEntry;

    syslogReceive(BlockingQueue<recordStruct> q) {
        queue = q;
    }

    public void run() {
        try {
            syslog = new DatagramSocket(514);
            logEntry = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
        } catch (SocketException ex) {
            System.out.print("SocketException: " + ex.toString() + "\n");
        }

        while (loopControl.test) {
            try {
                syslog.receive(logEntry);
            } catch (IOException ex) {
                System.out.print("IOException: " + ex.toString() + "\n");
            }

            InetAddress logHost = logEntry.getAddress();
            String logLine = new String(logEntry.getData(), 0, logEntry.getLength());
            try {
                queue.put(addToQueue(logLine, logHost));
            } catch (InterruptedException ex) {
                System.out.print("InterruptedException: " + ex.toString() + "\n");
            }
        }
    }

    recordStruct addToQueue(String logLine, InetAddress logHost) {
        Date logDate = new Date();
        String logPriority = "";
        int i;
        for (i = 0; i <= logLine.length(); i++) {
            if (logLine.charAt(i) == '>') break;
            if (logLine.charAt(i) != '<') logPriority = logPriority + logLine.charAt(i);
        }

        int logIntPriority = Integer.parseInt(logPriority.trim());
        String logData = logLine.substring(i + 1, logLine.length());

        return new recordStruct(logDate, logIntPriority, logHost, logData);

    }
}

class syslogProcess implements Runnable {

    private final BlockingQueue<recordStruct> queue;
    /*private final Database store;
    private final Sequence seq; */
    private final IndexWriter writer;

    syslogProcess(BlockingQueue<recordStruct> q, /*Database st, Sequence sq,*/ IndexWriter wr) {
        queue = q;
        /*store = st;
        seq = sq;*/
        writer = wr;
    }

    public void run() {


        try {
            while (loopControl.test) {
                storeLine(queue.take()/*, store, seq*/);
            }
        } catch (InterruptedException ex) {
            System.out.print("InterruptedException: " + ex.toString() + "\n");
        }
    }

    void storeLine(recordStruct logRecord/*, Database store, Sequence seq*/) {

        /*long sk = seq.get(null, 1);
        byte[] k = ByteBuffer.allocate(8).putLong(sk).array();
        byte[] d = logRecord.getBytes();

        DatabaseEntry storeKeyDBT = new DatabaseEntry(k);
        storeKeyDBT.setSize(8);
        DatabaseEntry storeDataDBT = new DatabaseEntry(d);
        storeDataDBT.setSize(d.length);

        try {
            store.put(null, storeKeyDBT, storeDataDBT);
        } catch (Exception dbe) {
            System.out.print("Couldn't add record to database\n");
            return;
        }*/

        Document doc = new Document();
        doc.add(new Field("host", logRecord.host.getHostName(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("date", logRecord.date.toString(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("priority", Integer.toString(logRecord.priority), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("data", logRecord.data, Field.Store.YES, Field.Index.ANALYZED));

        try {
            writer.addDocument(doc);
        } catch (IOException ex) {
            System.out.print("IOException: " + ex + "\n");
        }

        System.out.print("remain\n");
    }
}

class recordStruct {
    public Date date;
    public int priority;
    public InetAddress host;
    public String data;

    recordStruct(Date d, int p, InetAddress i, String s) {
        date = d;
        priority = p;
        host = i;
        data = s;
    }

    /*public byte[] getBytes() {
        byte[] recordBytes = new byte[1024];
        ByteBuffer bbdata = ByteBuffer.wrap(recordBytes);
        byte[] stringBytes = data.getBytes();

        bbdata.put(host.getAddress());
        bbdata.putInt(priority);
        bbdata.putLong(date.getTime());

        try {
            bbdata.put(stringBytes);
        } catch (Exception BufferOverflowException) {
            System.out.print("BufferOverflowException!\n" +
                    "StringLen:" + stringBytes.length + "\n" +
                    "ByteBufferLen:" + bbdata.array().length + "\n" +
                    "ByteBuffer:" + bbdata.toString() + "\n\n");
        }

        return ByteBuffer.wrap(recordBytes, 0, (16 + data.length())).array();

    }*/
}

