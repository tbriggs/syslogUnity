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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

class syslogReceive implements Runnable {
    int BUFFER_SIZE = 1000;
    private BlockingQueue<recordStruct> queue;
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
