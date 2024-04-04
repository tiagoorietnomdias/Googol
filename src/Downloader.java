import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Downloader extends UnicastRemoteObject implements IDownloader {
    private static final String STOPWORDS_FILE = "stopwords_pt.txt";
    private static final int MAX_MESSAGE_SIZE = 50 * 1024;
    private static int downloaderID;
    private static int packetID = 0;
    private IGateDownloader gateway;

    private static String title;

    public Downloader() throws RemoteException {
        super();
        try {
            gateway = (IGateDownloader) Naming.lookup("GatewayDownloader");
        } catch (NotBoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
        downloaderID = gateway.subscribeDownloader(this);
        System.out.println("Client sent subscription to server");
    }

    private static Properties loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(filename)) {
            properties.load(input);
        }
        return properties;
    }

    private static int sendMessage(String message, String ipAddress, int port) throws IOException {

        try (MulticastSocket socket = new MulticastSocket(port)) {
            InetAddress group = InetAddress.getByName(ipAddress);
            socket.joinGroup(group);
            byte[] msg = message.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, group, port);
            int retries = 0;

            while (retries < 3) {
                socket.send(packet);
                socket.setSoTimeout(1);

                byte[] buf = new byte[1024];
                DatagramPacket acknowledgmentPacket = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(acknowledgmentPacket);
                    //System.out.println(acknowledgmentPacket);
                    System.out.println("Recebido");
                    return 0;
                } catch (SocketTimeoutException e) {
                    retries++;
                    System.out.println("Timeout occurred, retrying (" + retries + "/" + 3 + ")");
                }

            }

            return -1;
        }
    }

    private static int getStringSizeInBytes(String str) {
        return str.getBytes().length;
    }

    private static int messageBuilder(ArrayList<String> words, int type, String url) throws IOException {
        int currentIndex = 0;
        int wordsLength = words.size();

        while (currentIndex < wordsLength) {
            StringBuilder messageToSend = new StringBuilder();
            String prefix = (type == 1) ? packetID + "|downloader|" + downloaderID + "|" + url + "|words|"+ title+"|"  : packetID + "|downloader|" + downloaderID + "|" + url + "|links|";
            packetID += 1;
            messageToSend.append(prefix);
            while (currentIndex < wordsLength && getStringSizeInBytes(prefix + words.get(currentIndex)) < MAX_MESSAGE_SIZE) {
                messageToSend.append(words.get(currentIndex)).append("|");
                currentIndex++;
            }

            System.out.println(messageToSend.toString());
            int sendResult = sendMessage(messageToSend.toString(), getUrlFromProperties(), getPortFromProperties());
            if (sendResult == -1) {
                System.out.println("Sending did not work. Placing link back in queue");
                return -1;
            }
        }
        return 0;
    }

    private static String getUrlFromProperties() throws IOException {
        return loadProperties("./src/resources/System.properties").getProperty("ipAddress");
    }

    private static int getPortFromProperties() throws IOException {
        return Integer.parseInt(loadProperties("./src/resources/System.properties").getProperty("port"));
    }

    private static Set<String> loadStopwords() {
        Set<String> stopwordsSet = new HashSet<>();
        try (Scanner scanner = new Scanner(new File(STOPWORDS_FILE))) {
            while (scanner.hasNextLine()) {
                stopwordsSet.add(scanner.nextLine().trim().toLowerCase());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Stopwords file not found: " + e.getMessage());
            e.printStackTrace();
        }
        return stopwordsSet;
    }

    public static void LinkScraper(String url, List<String> palavrasEncontradas, List<String> linksEncontrados) {
        Set<String> stopwordsSet = loadStopwords();
        try {
            Document doc = Jsoup.connect(url).get();
            title=doc.title();
            Elements links = doc.select("a[href]");
            StringTokenizer tokens = new StringTokenizer(doc.text());
            while (tokens.hasMoreElements()) {
                String currentToken = tokens.nextToken().toLowerCase().trim().replaceAll("(?i)[!.,?\"']", "");
                if (!stopwordsSet.contains(currentToken)) {
                    palavrasEncontradas.add(currentToken);
                }
            }
            for (Element link : links) {
                linksEncontrados.add(link.attr("abs:href"));
            }

        } catch (SSLHandshakeException ssle){
            System.out.println("SSL apanhado\n");
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run(String url) throws RemoteException {
        while (true) {
            List<String> words = new ArrayList<>();
            List<String> links = new ArrayList<>();

            LinkScraper(url, words, links);
            int result1 = 0, result2 = 0;
            try {
                result1 = messageBuilder(new ArrayList<>(words), 1, url);
                result2 = messageBuilder(new ArrayList<>(links), 0, url);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (result1 == -1 || result2 == -1) {
                links.clear();
                links.add(url);
            }

            //wait for notify signal
            gateway.putLinksInQueue(links);

            try {
                url = gateway.getLastLink();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java Downloader <url>");
            return;
        }

        String url = args[0];
        Downloader downloader = new Downloader();
        downloader.run(url);
    }
}
