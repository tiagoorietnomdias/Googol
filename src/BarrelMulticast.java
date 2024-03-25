import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class BarrelMulticast {

    private static final String PROPERTIES_FILE_PATH = "./src/resources/System.properties";
    private static final int BUFFER_SIZE = 1024;

    private HashMap<String, HashSet<String>> wordLinkMap = new HashMap<>();

    public static HashMap<String, HashSet<String>> updateWordHashMap(HashMap<String, HashSet<String>> map, String word, String link) {
        map.computeIfAbsent(word, k -> new HashSet<>()).add(link);
        return map;
    }


    public static void receiveMessage() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(PROPERTIES_FILE_PATH)) {
            properties.load(input);
        }

        String ipAddress = properties.getProperty("ipAddress");
        int port = Integer.parseInt(properties.getProperty("port"));

        try (MulticastSocket socket = new MulticastSocket(port)) {
            InetAddress group = InetAddress.getByName(ipAddress);
            socket.joinGroup(group);

            while (true) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + received);

                processMessage(received);
            }
        }
    }


    private static void processMessage(String message) {
        String[] parts = message.split("\\|");

        if (parts.length < 3 || !parts[0].equals("downloader")) {
            return;
        }

        String type = parts[2];
        if (type.equals("links")) {
            System.out.println("Processing links...");

        } else if (type.equals("words")) {
            System.out.println("Processing words...");

        }
    }

    public static void main(String[] args) throws IOException {
        receiveMessage();
    }
}
