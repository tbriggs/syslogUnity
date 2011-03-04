import com.sleepycat.je.DatabaseEntry;

import java.nio.ByteBuffer;

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

class LongEntry extends DatabaseEntry {
    LongEntry() {
    }

    LongEntry(long value) {
        setLong(value);
    }

    void setLong(long value) {
        byte[] data = ByteBuffer.allocate(8).putLong(value).array();
        setData(data);
        setSize(4);
    }

    long getLong() {
        return ByteBuffer.wrap(getData()).getLong();
    }
}
