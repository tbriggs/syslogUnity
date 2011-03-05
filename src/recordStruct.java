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

import java.net.InetAddress;
import java.nio.ByteBuffer;

class recordStruct {
    private static ByteBuffer data = ByteBuffer.allocate(1024);
    private static int logLineLength;

    recordStruct(byte[] rawData) {
        data.put(rawData,0,1024);
    }

    recordStruct(InetAddress host, int priority, long epoch, String logLine) {
        System.out.print(logLine.length() + "\n");
        data.put(host.getAddress(),0,4);
        data.putInt(4,priority);
        data.putLong(7,epoch);
        data.put(logLine.getBytes(),16,logLine.length());
        logLineLength = logLine.length();
    }

    public byte[] recordBytes() {
        return data.array();
    }

    public byte[] getHost() {
        byte[] temp = new byte[4];
        data.get(temp,0,4);
        return temp;
    }

    public int getPriority() {
        return data.getInt(4);
    }

    public long getEpoch() {
        return data.getLong(7);
    }

    public String getLogLine() {
        byte[] temp = new byte[logLineLength];
        return new String(temp);
    }

}
