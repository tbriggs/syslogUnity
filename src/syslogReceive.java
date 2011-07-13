import java.io.*;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;


class syslogReceive implements Runnable {
    private final BlockingQueue<recordStruct> queue;

    syslogReceive(BlockingQueue<recordStruct> q) {
        queue = q;
    }

    public void run() {

        File pipe = new File("/var/run/syslogUnity/syslogUnity.fifo");

        try {
            FileInputStream syslogPipe = new FileInputStream(pipe);
            DataInputStream in = new DataInputStream(syslogPipe);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String syslogXML;
            while (loopControl.test) {
                while ((syslogXML = br.readLine()) != null) {
                    //queue.put(addToQueue(syslogXML));
                    addToQueue(syslogXML);
                }
            }
            in.close();
        } catch (Exception ex) {
            System.out.print("Exception: " + ex.toString() + "\n");
        }

    }

    //recordStruct addToQueue(String syslogXML) {
    private static void addToQueue(String syslogXML) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(syslogXML);
            doc.getDocumentElement().normalize();

            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            NodeList nList = doc.getElementsByTagName("doc");
            System.out.println("-----------------------");

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
        //return new recordStruct(logDate, logIntPriority, logData);
    }

    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = nlList.item(0);

        return nValue.getNodeValue();
    }
}
