import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Downloader {
    public static void sendMessage(String message) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("./src/resources/System.properties")) {
            properties.load(input);
        }
        String ipAddress = properties.getProperty("ipAddress");
        int port = Integer.parseInt(properties.getProperty("port"));
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(ipAddress);
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length,
                group, port);
        socket.send(packet);
        socket.close();
    }

    public static int getStringSizeInBytes(String str) {
        // Get the byte array representation of the string
        byte[] byteArray = str.getBytes();

        // Get the length of the byte array

        return byteArray.length;
    }

    public static void messageBuilder(ArrayList<String> words, int maximumSize, int type) throws IOException {
        int wordsLength = words.size();
        int currentIndex = 0;
        while (currentIndex < wordsLength - 1) {// enquanto n ta vazio, encher msg
            StringBuilder messageToSend;
            if (type == 1) {
                messageToSend = new StringBuilder("downloader|words|");
            } else {
                messageToSend = new StringBuilder("downloader|links|");
            }

            String elementToGet = words.get(currentIndex);

            while (messageToSend.toString().getBytes().length + elementToGet.getBytes().length < maximumSize
                    && currentIndex < wordsLength - 1) { // while(messageToSend<tamanhomaximo(50kB)){

                messageToSend.append(elementToGet);
                messageToSend.append("|");
                currentIndex++;
                elementToGet = words.get(currentIndex);
            }
            System.out.println(messageToSend.toString());
            // System.out.println(messageToSend.toString().getBytes().length);
            sendMessage(messageToSend.toString());
        }

        // messageToSend+=words.next()
        // }

    }

    private static HashSet<String> stopwordsSet;

    // Load stopwords from file into a HashSet
    private static void loadStopwords(String filename) {
        stopwordsSet = new HashSet<>();
        try {
            Scanner scanner = new Scanner(new File(filename));
            while (scanner.hasNextLine()) {
                stopwordsSet.add(scanner.nextLine().trim().toLowerCase());
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.err.println("Stopwords file not found: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LinkScraper(String url, ArrayList<String> palavrasEncontradas,
            ArrayList<String> linksEncontrados) {
        File stopwordsFile = new File("stopwords_pt.txt");
        loadStopwords("stopwords_pt.txt"); // Load stopwords before scraping

        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href]");
            StringTokenizer tokens = new StringTokenizer(doc.text());
            int countTokens = 0;
            while (tokens.hasMoreElements()) {
                String currentToken = tokens.nextToken().toLowerCase();
                currentToken = currentToken.trim().replaceAll("(?i)[!.,?\"']", "");

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
    // for(String palavra:palavrasEncontradas) System.out.println(palavra);
    // for(String link:linksEncontrados) System.out.println(link);
    // enviar palavra a palavra até não haver mais

    // enviar link a link até não haver mais

    public static void main(String args[]) throws IOException {
        String url = args[0];
        ArrayList<String> words = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();

        LinkScraper(url, words, links);

        // 1= Words 0= links
        messageBuilder(words, 50 * 1024, 1);
        messageBuilder(links, 50 * 1024, 0);

        // Step 3: Send the message built to sendMessage
        /*
         * try {
         * sendMessage("your_message_here", "your_ip_address_here", your_port_here);
         * } catch (IOException e) {
         * e.printStackTrace();
         * }
         * }
         */
    }

}