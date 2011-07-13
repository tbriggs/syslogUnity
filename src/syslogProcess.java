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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.BlockingQueue;

class syslogProcess implements Runnable {

    private static BlockingQueue<org.w3c.dom.Document> queue;
    private static IndexWriter writer;

    syslogProcess(BlockingQueue<org.w3c.dom.Document> q, IndexWriter wr) {
        queue = q;
        writer = wr;
    }

    public void run() {


        try {
            while (loopControl.test) {
                storeLine(queue.take());
                //System.out.print("Left in queue: " + queue.size() + "  \r");
            }
            writer.commit();
            writer.optimize();
            writer.close();
        } catch (Exception ex) {
            System.out.print("Exception: " + ex.toString() + "\n");
        }

    }

    void storeLine(org.w3c.dom.Document syslogDoc) {

        try {

            NodeList nList = syslogDoc.getElementsByTagName("doc");

            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    System.out.println("From : " + getTagValue("from", eElement));
                    System.out.println("Facility : " + getTagValue("facility", eElement));
                    System.out.println("Data : " + getTagValue("msg", eElement));
                    System.out.println("Hostname : " + getTagValue("hostname", eElement));
                    System.out.println("Priority : " + getTagValue("priority", eElement));
                    System.out.println("Tag : " + getTagValue("tag", eElement));
                    System.out.println("Program : " + getTagValue("program", eElement));
                    System.out.println("Severity : " + getTagValue("severity", eElement));
                    System.out.println("Generated : " + getTagValue("generated", eElement));

                }
            }
        } catch (Exception ex) {
            System.out.print("Exception: " + ex.toString() + "\n");
        }

        //Document doc = new Document();
        //doc.add(new Field("host", logRecord.host.getHostName(), Field.Store.YES, Field.Index.ANALYZED));
        //doc.add(new NumericField("date", Field.Store.YES, true).setLongValue(logRecord.date.getTime()));
        //doc.add(new NumericField("priority", Field.Store.YES, true).setIntValue(logRecord.priority));
        //doc.add(new Field("data", logRecord.data, Field.Store.YES, Field.Index.ANALYZED));

        //try {
        //    writer.addDocument(doc);
        //} catch (IOException ex) {
        //    System.out.print("IOException: " + ex + "\n");
        //}

    }

    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = nlList.item(0);

        return nValue.getNodeValue();
    }
}
