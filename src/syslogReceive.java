import java.io.*;
import java.util.concurrent.BlockingQueue;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.xml.sax.InputSource;


class syslogReceive implements Runnable {
    private final BlockingQueue<org.w3c.dom.Document> queue;

    syslogReceive(BlockingQueue<org.w3c.dom.Document> q) {
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
                    queue.put(addToQueue(syslogXML));
                }
            }
            in.close();
        } catch (Exception ex) {
            System.out.print("Exception: " + ex.toString() + "\n");
        }

    }

    org.w3c.dom.Document addToQueue(String syslogXML) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(syslogXML));
            return dBuilder.parse(is);

        } catch (Exception ex) {
            System.out.print("Exception: " + ex.toString() + "\n");
            return null;
        }
    }
}
