import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;


public class BarrelMulticast extends MulticastSocket{
    String ipAddress;
   // private final int port;
    ArrayList<Integer> packetCounter= new ArrayList<>();
public BarrelMulticast() throws IOException {
    super(getPortFromProperties());
    Properties properties = new Properties();
    try (FileInputStream input = new FileInputStream(PROPERTIES_FILE_PATH)) {
        properties.load(input);
    }

    ipAddress = properties.getProperty("ipAddress");
}

    private static int getPortFromProperties() throws IOException {
        try (FileInputStream input = new FileInputStream(PROPERTIES_FILE_PATH)) {
            Properties properties = new Properties();
            properties.load(input);
            return Integer.parseInt(properties.getProperty("port"));
        }
    }
    private static final String PROPERTIES_FILE_PATH = "./src/resources/System.properties";
    private static final int BUFFER_SIZE = 1024;

    private HashMap<String, HashSet<String>> wordLinkMap = new HashMap<>();

    public HashMap<String, HashSet<String>> updateWordHashMap(String word, String link) {
        wordLinkMap.computeIfAbsent(word, k -> new HashSet<>()).add(link);
        return wordLinkMap;
    }

    public void receiveMessage() throws IOException {


        InetAddress group = InetAddress.getByName(ipAddress);
        this.joinGroup(group);

        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            this.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received: " + received);
            processMessage(received);
        }
    }

    //Protocolo: packetID|downloader|downloaderID|LinkAtual|words/links|....
    //Protocolo:     0   |     1    |      2     |    3    |      4    |....
    private void processMessage(String message) {
        String[] parts = message.split("\\|");

        if (parts.length < 6 || !parts[1].equals("downloader")) {
            return;
        }
        int packetID= Integer.parseInt(parts[0]);
        int downloaderID=Integer.parseInt(parts[2]);
        String currentLink= parts[3];
        String type = parts[4];

        //Se o packet ID que chegou não corresponde ao atual
        //packetID-packetIDanterior <=1 ta td bem
        //portanto se packetID-packetIDanterior >1 n ta bem
        if(packetID- packetCounter.get(downloaderID) >1){//Falhou pelo menos um pacote

        }
        else {
            packetCounter.set(downloaderID, packetCounter.get(packetID));
        }

        if (type.equals("links")) {
            System.out.println("Processing links...");

        } else if (type.equals("words")) {
            System.out.println("Processing words...");

        }
    }

    public static void main(String[] args) throws IOException {
        BarrelMulticast barrel = new BarrelMulticast();
        barrel.receiveMessage();
    }
}
