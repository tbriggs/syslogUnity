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
    public byte[] recordBytes = new byte[1024];
    private ByteBuffer data = ByteBuffer.wrap(recordBytes);

    recordStruct(byte[] rawData) {
        data.put(rawData, 0, 1024);
    }

    recordStruct(InetAddress host, int priority, long epoch, String logLine) {
        byte[] stringBytes = logLine.getBytes();
        data.put(host.getAddress());
        data.putInt(priority);
        data.putLong(epoch);
        data.putInt(stringBytes.length);
        try {
            data.put(stringBytes);
        } catch (Exception BufferOverflowException) {
            System.out.print("BufferOverflowException!\n" +
                    "StringLen:" + stringBytes.length + "\n" +
                    "ByteBufferLen:" + data.array().length + "\n" +
                    "ByteBuffer:" + data.toString() + "\n\n");
        }

    }

    public byte[] getHost() {
        ByteBuffer bb = ByteBuffer.wrap(recordBytes,0,4);
        byte[] temp = new byte[4];
        bb.get(temp);
        return temp;
    }

    public int getPriority() {
        ByteBuffer bb = ByteBuffer.wrap(recordBytes,4,4);
        return bb.getInt();
    }

    public long getEpoch() {
        ByteBuffer bb = ByteBuffer.wrap(recordBytes,7,8);
        return bb.getLong();
    }

    public String getLogLine() {
        int logLineLength = stringLength();
        ByteBuffer bbstr = ByteBuffer.wrap(recordBytes,20,logLineLength);
        byte[] temp = new byte[logLineLength];
        bbstr.get(temp);
        return new String(temp);
    }

    public int stringLength() {
        ByteBuffer bbint = ByteBuffer.wrap(recordBytes,16,4);
        return bbint.getInt();
    }

    public byte[] trimmedBytes() {
        return ByteBuffer.wrap(recordBytes,0,(20+stringLength())).array();
    }

}
