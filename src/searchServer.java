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

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.regex.Pattern;

class searchServer implements Runnable {

    public void run() {

        try {
            ServerSocket searchListener = new ServerSocket(1228);

            Socket searchSocket = searchListener.accept();
            BufferedReader searchInput = new BufferedReader(new InputStreamReader(searchSocket.getInputStream()));

            String[] searchQuery = new String[5];
            int i = 0;

            while (i < 5) {
                searchQuery[i] = searchInput.readLine().trim();
                if (searchQuery[i].equals("\n") ||
                        searchQuery[i].equals("\r\n") ||
                        searchQuery[i].isEmpty())
                    break;
                i++;
            }

            System.out.print(i + " elements in array\n");

            String hostnameField = null;
            String priorityField = null;
            String dateStartField = null;
            String dateEndField = null;
            String dataField = null;

            Pattern matchData = Pattern.compile("^data:", Pattern.CASE_INSENSITIVE);
            Pattern matchHostname = Pattern.compile("^hostname:", Pattern.CASE_INSENSITIVE);
            Pattern matchPriority = Pattern.compile("^priority:", Pattern.CASE_INSENSITIVE);
            Pattern matchDateStart = Pattern.compile("^datestart:", Pattern.CASE_INSENSITIVE);
            Pattern matchDateEnd = Pattern.compile("^dateend:", Pattern.CASE_INSENSITIVE);



            for (int n = 0; n < i; n++) {
                if (matchData.matcher(searchQuery[n]).find()) {
                    dataField = searchQuery[n].substring(5).trim();
                } else if (matchHostname.matcher(searchQuery[n]).find()) {
                    hostnameField = searchQuery[n].substring(9).trim();
                } else if (matchPriority.matcher(searchQuery[n]).find()) {
                    priorityField = searchQuery[n].substring(9).trim();
                } else if (matchDateStart.matcher(searchQuery[n]).find()) {
                    dateStartField = searchQuery[n].substring(10).trim();
                } else if (matchDateEnd.matcher(searchQuery[n]).find()) {
                    dateEndField = searchQuery[n].substring(8).trim();
                }
            }


            PrintWriter searchReply = new PrintWriter(searchSocket.getOutputStream(), true);

            String reply = "SYSLOGUNITY\n";

            reply = reply.concat("Your Search:\n");

            if (hostnameField != null) reply = reply.concat("Hostname: '" + hostnameField + "'\n");
            if (priorityField != null) reply = reply.concat("Priority: '" + priorityField + "'\n");
            if (dateStartField != null) reply = reply.concat("Start Date: '" + dateStartField + "'\n");
            if (dateEndField != null) reply = reply.concat("End Date: '" + dateEndField + "'\n");
            if (dataField != null) reply = reply.concat("Data: '" + dataField + "'\n\n");

            searchReply.print(reply);

            searchReply.close();
            searchSocket.close();
            searchListener.close();
        } catch (Exception e) {
            System.out.print("Whoops! It didn't work!\n");
        }
    }
}
