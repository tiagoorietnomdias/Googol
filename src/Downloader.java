/**
 * The `Downloader` class in Java is a remote object that scrapes links and words from a given URL,
 * sends messages to a multicast group, and interacts with a gateway for downloading tasks.
 */

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Downloader extends UnicastRemoteObject implements IDownloader {
    private static final String STOPWORDS_FILE = "stopwords_pt.txt";
    private static final int MAX_MESSAGE_SIZE = 50 * 1024;
    private static int downloaderID;
    private static int packetID = 0;
    private IGateDownloader gateway;

    private static String title;

    // This `public Downloader() throws RemoteException` constructor in the `Downloader` class is
    // responsible for initializing a new instance of the `Downloader` class. Here's a breakdown of
    // what it does:
    public Downloader() throws RemoteException {
        super();
        while (true) {
            try {
                gateway = (IGateDownloader) Naming.lookup("GatewayDownloader");
            } catch (NotBoundException | MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (RemoteException e) {
                try {
                    System.out.println("Connection refused, waiting 3 second and retrying...");
                    Thread.sleep(3000);
                    System.out.println("Resuming Connection...");
                    continue;

                } catch (InterruptedException ie) {
                    System.out.println("Downloader stopped while waiting");
                }
            }
            break;
        }
        downloaderID = gateway.subscribeDownloader(this);
        System.out.println("Client sent subscription to server");
    }

    /**
     * The function `loadProperties` loads properties from a file specified by the filename parameter.
     *
     * @param filename The `filename` parameter in the `loadProperties` method is a `String` that
     *                 represents the name of the file from which properties need to be loaded.
     * @return The method `loadProperties` is returning a `Properties` object.
     */
    private static Properties loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(filename)) {
            properties.load(input);
        } catch (IOException e) {
            System.out.println("Couldn't load \"" + filename + "\". Make sure the file is available");
        }
        return properties;
    }

    /**
     * The `sendMessage` function sends a message over a multicast socket to a specified IP address and
     * port, with retries in case of timeouts and returning success or failure status.
     *
     * @param message          The `message` parameter in the `sendMessage` method is a string that represents the
     *                         message you want to send over a multicast socket to a specific IP address and port. This message
     *                         will be converted to bytes before being sent as a DatagramPacket over the network.
     * @param sendIpAddress    The `ipAddress` parameter in the `sendMessage` method is the IP address of the
     *                         multicast group to which the message will be sent. It represents the destination address for the
     *                         multicast message.
     * @param receiveIpAddress
     * @param port             The `port` parameter in the `sendMessage` method is used to specify the port number on
     *                         which the multicast socket will be created and through which the message will be sent to the
     *                         specified IP address. It is an integer value that represents the port number for the communication.
     * @return The `sendMessage` method returns an integer value. If the message is successfully sent and
     * an acknowledgment is received within 3 retries, it returns 0. If no acknowledgment is received
     * within the retries, it returns -1.
     */
    private static int sendMessage(String message, String sendIpAddress, String receiveIpAddress, int port) throws IOException {

        try (MulticastSocket socket = new MulticastSocket(port)) {
            InetAddress sendgroup = InetAddress.getByName(sendIpAddress);
            //InetAddress receivegroup = InetAddress.getByName(receiveIpAddress);

            int retries = 0;

            while (retries < 3) {

                byte[] msg = message.getBytes();
                DatagramPacket packetToSend = new DatagramPacket(msg, msg.length, sendgroup, port);
                socket.send(packetToSend);
                //System.out.println(packetToSend);
                //socket.leaveGroup(sendgroup);
                //socket.joinGroup(receivegroup);
                socket.joinGroup(sendgroup);
                socket.setSoTimeout(7000);

                byte[] buf = new byte[1024];
                DatagramPacket acknowledgmentPacket = new DatagramPacket(buf, buf.length, sendgroup, port);
                boolean receivedFromOtherInstance = false;
                try {
                    do {
                        socket.receive(acknowledgmentPacket);
                        String ackMessage = new String(acknowledgmentPacket.getData(), 0, acknowledgmentPacket.getLength());

                        if (ackMessage.contains("ack " + downloaderID)) {
                            receivedFromOtherInstance = true;
                            System.out.println("acknowledge received,moving on");
                            //System.out.println("Received acknowledgment from another instance");
                        } else {
                            // This acknowledgment packet is self-generated, ignore it and continue receiving
                            //System.out.println("Received self-generated acknowledgment, continuing to receive...");
                        }
                    } while (!receivedFromOtherInstance);
                    return 0;
                } catch (SocketTimeoutException e) {
                    retries++;
                    System.out.println("Timeout occurred, retrying (" + retries + "/" + 3 + ")");
                }
                socket.leaveGroup(sendgroup);

            }

            return -1;
        }
    }

    /**
     * The function `getStringSizeInBytes` returns the size of a given string in bytes.
     *
     * @param str A string for which you want to calculate the size in bytes.
     * @return The method `getStringSizeInBytes` returns the size of the input string `str` in bytes.
     */
    private static int getStringSizeInBytes(String str) {
        return str.getBytes().length;
    }

    /**
     * The function `messageBuilder` constructs and sends messages based on a list of words, a type,
     * and a URL, ensuring the message size does not exceed a maximum limit.
     *
     * @param words The `words` parameter is an ArrayList of Strings containing the words that need to
     *              be processed and sent in the message.
     * @param type  The `type` parameter in the `messageBuilder` method is used to determine the format
     *              of the message prefix based on its value. If `type` is equal to 1, the prefix will include
     *              additional information such as `packetID`, `downloaderID`, `url`, and `title
     * @param url   The `url` parameter in the `messageBuilder` method is a String variable that
     *              represents the URL to be included in the message being constructed. It is used to specify the
     *              destination URL for the message being sent.
     * @return The method `messageBuilder` is returning an integer value. If the sending of the message
     * fails (sendResult == -1), it will return -1. Otherwise, it will return 0.
     */
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
            int sendResult = sendMessage(messageToSend.toString(), getSendIpAddressFromProperties(),
                    getReceiveIpAddressFromProperties(), getPortFromProperties());
            if (sendResult == -1) {
                System.out.println("Sending did not work. Placing link back in queue");
                return -1;
            }
        }
        return 0;
    }

    /**
     * The function `getUrlFromProperties` loads a properties file and retrieves the value of the
     * "ipAddress" property.
     *
     * @return The method `getUrlFromProperties` is returning the value of the property "ipAddress" from
     * the System.properties file located in the resources folder.
     */
    private static String getSendIpAddressFromProperties() throws IOException {
        return loadProperties("./src/resources/System.properties").getProperty("sendIpAddress");
    }

    private static String getReceiveIpAddressFromProperties() throws IOException {
        return loadProperties("./src/resources/System.properties").getProperty("receiveIpAddress");
    }

    /**
     * The function `getPortFromProperties` parses and retrieves the port value from a properties file
     * located in the src/resources directory.
     *
     * @return The method `getPortFromProperties` is returning an integer value representing the port
     * number obtained from the "port" property in the System.properties file located in the resources
     * directory.
     */
    private static int getPortFromProperties() throws IOException {
        return Integer.parseInt(loadProperties("./src/resources/System.properties").getProperty("port"));
    }

    /**
     * The function `loadStopwords` reads a file containing stopwords, stores them in a set after
     * trimming and converting to lowercase, and returns the set.
     *
     * @return The method `loadStopwords()` returns a `Set<String>` containing stopwords loaded from a
     * file.
     */
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

    /**
     * The function `LinkScraper` scrapes a given URL for words and links, filtering out stopwords and
     * handling various exceptions.
     *
     * @param url                 The `url` parameter is a String representing the URL of the webpage that you want to
     *                            scrape for information.
     * @param palavrasEncontradas The parameter `palavrasEncontradas` is a list that will store the words
     *                            found on the webpage after scraping and processing the text content.
     * @param linksEncontrados    The parameter `linksEncontrados` is a List of Strings that will store the
     *                            URLs found on the webpage specified by the `url` parameter in the `LinkScraper` method. Each URL
     *                            found on the webpage will be added to this list for further processing or analysis.
     */
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

        } catch (RemoteException e) {
            System.out.println("Connection refused");
        } catch (SSLHandshakeException ssle) {
            System.out.println("SSL apanhado\n");
        } catch (IOException e) {
            System.out.println("Malformed URL");

        } catch (IllegalArgumentException e) {
            System.out.println("Reached the end of site");
        }
    }

    /**
     * This Java function continuously retrieves URLs from a gateway, scrapes words and links from the
     * URLs, builds messages from the words and links, and handles exceptions while updating the links
     * list in a loop.
     */
    private void run() throws RemoteException {
        while (true) {
            List<String> words = new ArrayList<>();
            List<String> links = new ArrayList<>();
            String url = null;
            do {
                try {
                    url = gateway.getLastLink();
                } catch (NoSuchElementException | InterruptedException e) {
                    System.out.println("found nothing imma keep lookin");
                }
            } while (url == null);
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

            gateway.putLinksInQueue(links);

            try {
                url = gateway.getLastLink();
            } catch (RemoteException | InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * The main method in Java checks for command line arguments and creates an instance of the
     * Downloader class to run the program.
     */
    public static void main(String[] args) throws IOException {


        Downloader downloader = new Downloader();
        downloader.run();
    }
}