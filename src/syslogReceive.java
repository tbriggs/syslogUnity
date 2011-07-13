import java.io.*;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

class syslogReceive implements Runnable {
    private final BlockingQueue<recordStruct> queue;

    syslogReceive(BlockingQueue<recordStruct> q) {
        queue = q;
    }

    public void run() {

        while (loopControl.test) {
            try {
                File pipe = new File("/var/run/syslogUnity/syslogUnity.fifo");
                System.out.println(pipe.canRead());
                FileInputStream fis = new FileInputStream(pipe);
                System.out.println("exiting.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            //try {
            //    queue.put(addToQueue(logLine, logHost));
            //} catch (InterruptedException ex) {
            //    System.out.print("InterruptedException: " + ex.toString() + "\n");
            //}
        }
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
