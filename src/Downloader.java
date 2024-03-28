import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
    private IGateway gateway;

    public Downloader() throws RemoteException {
        super();
        try {
            gateway = (IGateway) Naming.lookup("Gateway");
        } catch (NotBoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
        downloaderID = gateway.subscribeDownloader((IDownloader) this);
        System.out.println("Client sent subscription to server");
    }

    private static Properties loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(filename)) {
            properties.load(input);
        }
        return properties;
    }

    private static void sendMessage(String message, String ipAddress, int port) throws IOException {
        //Todo: implement reliable multicast
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(ipAddress);
            byte[] msg = message.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, group, port);
            socket.send(packet);
        }
    }

    private static int getStringSizeInBytes(String str) {
        return str.getBytes().length;
    }

    private static void messageBuilder(ArrayList<String> words, int type, String url) throws IOException {
        int currentIndex = 0;
        int wordsLength = words.size();

        while (currentIndex < wordsLength) {
            StringBuilder messageToSend = new StringBuilder();
            String prefix = (type == 1) ? packetID + "|downloader|" + downloaderID + "|" + url + "|words|" : packetID + "|downloader|" + downloaderID + "|" + url + "|links|";
            packetID += 1;
            messageToSend.append(prefix);
            while (currentIndex < wordsLength && getStringSizeInBytes(prefix + words.get(currentIndex)) < MAX_MESSAGE_SIZE) {
                messageToSend.append(words.get(currentIndex)).append("|");
                currentIndex++;
            }

            System.out.println(messageToSend.toString());
            sendMessage(messageToSend.toString(), getUrlFromProperties(), getPortFromProperties());
        }
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run(String url) {
        while (true) {
            List<String> words = new ArrayList<>();
            List<String> links = new ArrayList<>();

            LinkScraper(url, words, links);
            try {
                messageBuilder(new ArrayList<>(words), 1, url);
                messageBuilder(new ArrayList<>(links), 0, url);
            } catch (IOException e) {
                e.printStackTrace();
            }


            try {
                gateway.putLinksInQueue(links);
            } catch(RemoteException e){
                e.printStackTrace();
            }

            //wait for notify signal
            try {
                url = gateway.getLastLink();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //repeat
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
