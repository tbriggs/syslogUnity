import com.sleepycat.db.DatabaseEntry;

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

class IntEntry extends DatabaseEntry {
    IntEntry() {
    }

    IntEntry(int value) {
        setInt(value);
    }

    void setInt(int value) {
        byte[] data = ByteBuffer.allocate(4).putInt(value).array();
        setData(data);
        setSize(4);
    }

    long getInt() {
        return ByteBuffer.wrap(getData()).getInt();
    }
}
