import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class syslogUnityBackend {
    public static void main(String[] args) {
        BlockingQueue<recordStruct> q = new LinkedBlockingQueue<recordStruct>();
        syslogReceive logServer = new syslogReceive(q);
        syslogProcess logPrinter = new syslogProcess(q);
        new Thread(logServer).start();
        new Thread(logPrinter).start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    loopControl.test = false;
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

        return new recordStruct(logHost, logIntPriority, logDate.getTime(), logData);

    }
}

class syslogProcess implements Runnable {
    private final BlockingQueue<recordStruct> queue;

    syslogProcess(BlockingQueue<recordStruct> q) {
        queue = q;
    }

    public void run() {
        try {
            while (loopControl.test) {
                printLine(queue.take());
            }
        } catch (InterruptedException ex) {
            System.out.print("InterruptedException: " + ex.toString() + "\n");
        }
    }

    void printLine(recordStruct logRecord) {
        Date queueDate = new Date(logRecord.getEpoch());
        int queuePriority = logRecord.getPriority();
        InetAddress queueHost;
        String queueLogLine = logRecord.getLogLine();

        try {
            queueHost = InetAddress.getByAddress(logRecord.getHost());
        } catch (Exception ex) {
            System.out.print("UnknownHostException: " + ex.toString() + "\n");
            return;
        }

        System.out.print("Date:" + queueDate.toString() +
                "\nPri:" + queuePriority +
                "\nIP:" + queueHost.getHostAddress() +
                "\nData: " + queueLogLine + "\n");
    }
}

class recordStruct {
    public byte[] recordBytes = new byte[1024];
    ByteBuffer data = ByteBuffer.wrap(recordBytes);

//    recordStruct(byte[] rawData) {
//        data.put(rawData, 0, 1024);
//    }

    recordStruct(InetAddress host, int priority, long epoch, String logLine) {
        byte[] stringBytes = logLine.getBytes();
        data.put(host.getAddress());
        data.putInt(priority);
        data.putLong(epoch);
        data.putInt(stringBytes.length);
        try {
            data.put(stringBytes);
        } catch (Exception ex) {
            System.out.print("BufferOverflowException: " + ex.toString() + "\n");
        }

    }

    public byte[] getHost() {
        ByteBuffer bb = ByteBuffer.wrap(recordBytes, 0, 4);
        byte[] temp = new byte[4];
        bb.get(temp);
        return temp;
    }

    public int getPriority() {
        ByteBuffer bb = ByteBuffer.wrap(recordBytes, 4, 4);
        return bb.getInt();
    }

    public long getEpoch() {
        ByteBuffer bb = ByteBuffer.wrap(recordBytes, 7, 8);
        return bb.getLong();
    }

    public String getLogLine() {
        int logLineLength = stringLength();
        ByteBuffer bbstr = ByteBuffer.wrap(recordBytes, 20, logLineLength);
        byte[] temp = new byte[logLineLength];
        bbstr.get(temp);
        return new String(temp);
    }

    public int stringLength() {
        ByteBuffer bbint = ByteBuffer.wrap(recordBytes, 16, 4);
        return bbint.getInt();
    }

//    public byte[] trimmedBytes() {
//        return ByteBuffer.wrap(recordBytes,0,(20+stringLength())).array();
//    }

}

