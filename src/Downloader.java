import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.ConnectException;
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
        while(true) {
            try {
                gateway = (IGateDownloader) Naming.lookup("GatewayDownloader");
            } catch (NotBoundException | MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (RemoteException e) {
                try{
                    System.out.println("Connection refused, waiting 3 second and retrying...");
                    Thread.sleep(3000);
                    System.out.println("Resuming Connection...");
                    continue;

                } catch (InterruptedException ie){
                    System.out.println("Downloader stopped while waiting");
                }
            }
            break;
        }
        downloaderID = gateway.subscribeDownloader(this);
        System.out.println("Client sent subscription to server");
    }

    private static Properties loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(filename)) {
            properties.load(input);
        } catch(IOException e){
            System.out.println("Couldn't load \"" + filename + "\". Make sure the file is available");
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
                socket.setSoTimeout(100);

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
            String prefix = (type == 1) ? packetID + "|downloader|" + downloaderID + "|" + url + "|words|" + title + "|" : packetID + "|downloader|" + downloaderID + "|" + url + "|links|";
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
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0").get();
            title = doc.title();
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

        } catch(RemoteException e){
            System.out.println("Connection refused");
        }catch (SSLHandshakeException ssle) {
            System.out.println("SSL apanhado\n");
        } catch (IOException e) {
            System.out.println("Malformed URL");

        }
    }

    private void run() throws RemoteException {
        while (true) {
            List<String> words = new ArrayList<>();
            List<String> links = new ArrayList<>();
            String url=null;
            do {
                try{
                    url = gateway.getLastLink();
                }
                catch (NoSuchElementException | InterruptedException e){
                    System.out.println("found nothing imma keep lookin");
                }
            } while (url == null);
            System.out.println(url);
            LinkScraper(url, words, links);
            System.out.println(url);
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
            } catch (RemoteException | InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java Downloader <url>");
            return;
        }


        Downloader downloader = new Downloader();
        downloader.run();
    }
}
