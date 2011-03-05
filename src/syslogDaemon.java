import com.sleepycat.db.*;


import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Date;
import java.io.File;

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

public class syslogDaemon {

    public static void main(String[] args) throws Exception {
        new syslogDaemon().getEntries();
    }

    private void getEntries() throws Exception {
        final File dbEnvDir = new File("/var/lib/syslogUnity/queueDB/");
        int BUFFER_SIZE = 972;

        DatagramSocket syslog = new DatagramSocket(514);
        DatagramPacket logEntry = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        envConfig.setCacheSize(20971520);
        envConfig.setInitializeCache(true);

        final Environment env = new Environment(dbEnvDir, envConfig);

        DatabaseConfig queueConfig = new DatabaseConfig();
        queueConfig.setType(DatabaseType.QUEUE);
        queueConfig.setReadUncommitted(true);
        queueConfig.setAllowCreate(true);
        queueConfig.setPageSize(4096);
        queueConfig.setRecordLength(1024);
        final Database queue = env.openDatabase(null, "logQueue.db", null, queueConfig);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                  whileBreak.almostalways = false;
                } catch (Exception dbe) {
                    System.out.print("Caught SIGINT, couldn't close queueDB successfully\n");
                }
            }
        });

        while (whileBreak.almostalways) {
            syslog.receive(logEntry);
            InetAddress logHost = logEntry.getAddress();
            String logLine = new String(logEntry.getData(), 0, logEntry.getLength());
            doStuff(logLine, logHost, queue);
        }
                            queue.close();
                    env.close();
                    System.out.print("Closed DB Gracefully...\n");
    }

    void doStuff(String logLine, InetAddress logHost, Database queue) {
        Date logDate = new Date();
        String logPriority = "";
        int i;
        for (i = 0; i <= logLine.length(); i++) {
            if (logLine.charAt(i) == '>') break;
            if (logLine.charAt(i) != '<') logPriority = logPriority + logLine.charAt(i);
        }

        int logIntPriority = Integer.parseInt(logPriority.trim());
        String logData = logLine.substring(i + 1, logLine.length());

        recordStruct queueRecord = new recordStruct(logHost,logIntPriority,logDate.getTime(),logData);
        byte[] k = new byte[4];
        byte[] d = queueRecord.recordBytes;

        DatabaseEntry kdbt = new DatabaseEntry(k);
        kdbt.setSize(4);
        DatabaseEntry ddbt = new DatabaseEntry(d);
        ddbt.setSize(1024);

        try {
            queue.append(null, kdbt, ddbt);
        } catch (Exception dbe) {
            System.out.print("Couldn't add record to database\n");
        }
    }
}

