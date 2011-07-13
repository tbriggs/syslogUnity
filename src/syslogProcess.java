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
                System.out.print("Left in queue: " + queue.size() + "  \r");
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
            Node nNode = nList.item(0);
            Element eElement = (Element) nNode;

            String from = getTagValue("from", eElement);
            String facility = getTagValue("facility", eElement);
            String msg = getTagValue("msg", eElement);
            String hostname = getTagValue("hostname", eElement);
            int priority = Integer.parseInt(getTagValue("priority", eElement));
            String tag = getTagValue("tag", eElement);
            String program = getTagValue("program", eElement);
            String severity = getTagValue("severity", eElement);
            long generated = Long.parseLong(getTagValue("generated", eElement));

            Document doc = new Document();
            doc.add(new Field("from", from, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("facility", facility, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("data", msg, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("hostname", hostname, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new NumericField("priority", Field.Store.YES, true).setIntValue(priority));
            doc.add(new Field("tag", tag, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("program", program, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("severity", severity, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new NumericField("date", Field.Store.YES, true).setLongValue(generated));

            writer.addDocument(doc);

        } catch (Exception ex) {
            System.out.print("Exception: " + ex.toString() + "\n");
        }

    }

    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = nlList.item(0);

        return nValue.getNodeValue();
    }
}
