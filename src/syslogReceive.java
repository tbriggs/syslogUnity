
import java.io.*;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

class syslogReceive implements Runnable {
    private final BlockingQueue<recordStruct> queue;

    syslogReceive(BlockingQueue<recordStruct> q) {
        queue = q;
    }

    public void run() {

        File pipe = new File("/var/run/syslogUnity/syslogUnity.fifo");

        try {
            FileInputStream syslogPipe = new FileInputStream(pipe);
            DataInputStream in = new DataInputStream(syslogPipe);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String syslogLine;
            while (loopControl.test) {
                syslogLine = br.readLine();
                System.out.println(syslogLine);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        //while (loopControl.test) {


        //try {
        //    queue.put(addToQueue(logLine, logHost));
        //} catch (InterruptedException ex) {
        //    System.out.print("InterruptedException: " + ex.toString() + "\n");
        //}
        //}
    }

    recordStruct addToQueue(String logLine) {
        Date logDate = new Date();
        String logPriority = "";
        int i;
        for (i = 0; i <= logLine.length(); i++) {
            if (logLine.charAt(i) == '>') break;
            if (logLine.charAt(i) != '<') logPriority = logPriority + logLine.charAt(i);
        }

        int logIntPriority = Integer.parseInt(logPriority.trim());
        String logData = logLine.substring(i + 1, logLine.length());

        return new recordStruct(logDate, logIntPriority, logData);

    }
}
