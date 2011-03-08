import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.sql.*;

class syslogUnityBackend {
    public static void main(String[] args) {
        BlockingQueue<recordStruct> q = new LinkedBlockingQueue<recordStruct>();

        syslogReceive logServer = new syslogReceive(q);
        syslogProcess logPrinter1 = new syslogProcess(q);
        syslogProcess logPrinter2 = new syslogProcess(q);
        syslogProcess logPrinter3 = new syslogProcess(q);
        syslogProcess logPrinter4 = new syslogProcess(q);
        syslogProcess logPrinter5 = new syslogProcess(q);

        new Thread(logServer).start();
        new Thread(logPrinter1).start();
        new Thread(logPrinter2).start();
        new Thread(logPrinter3).start();
        new Thread(logPrinter4).start();
        new Thread(logPrinter5).start();


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
        Connection dbConnection;

        String userName = "syslogUnity";
        String password = "syslogUnity";
        String url = "jdbc:mysql://localhost/syslogUnity";

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            dbConnection = DriverManager.getConnection(url, userName, password);
        } catch (Exception ex) {
            System.out.print("Exception: " + ex.toString() + "\n");
            return;
        }

        try {
            while (loopControl.test) {
                printLine(queue.take(), dbConnection);
            }
        } catch (InterruptedException ex) {
            System.out.print("InterruptedException: " + ex.toString() + "\n");
        }
    }

    void printLine(recordStruct logRecord, Connection dbConnection) {
        Date queueDate = new Date(logRecord.getEpoch());
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        int queuePriority = logRecord.getPriority();
        InetAddress queueHost;
        String queueLogLine = logRecord.getLogLine();
        Statement logLineSQL;
        long logLineKey;

        try {
            logLineSQL = dbConnection.createStatement();
        } catch (SQLException ex) {
            System.out.print("SQLException: " + ex.toString() + "\n");
            return;
        }

        try {
            queueHost = InetAddress.getByAddress(logRecord.getHost());
        } catch (Exception ex) {
            System.out.print("UnknownHostException: " + ex.toString() + "\n");
            return;
        }

        try {
            logLineSQL.executeUpdate(
                    "INSERT INTO syslogUnity (date, priority, host, data)"
                            + "VALUES ("
                            + "'" + dateFormat.format(queueDate) + "',"
                            + queuePriority + ","
                            + "'" + queueHost.getHostAddress() + "',"
                            + "'" + queueLogLine + "'"
                            + ")"
            );
            ResultSet rs = logLineSQL.getGeneratedKeys();
            logLineKey = rs.getLong(1);
            rs.close();
            logLineSQL.close();
        } catch (SQLException ex) {
            System.out.print("SQLException: " + ex.toString() + "\n");
            return;
        }


        System.out.print("Inserted Successfully Key#" + logLineKey + "\n");
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

