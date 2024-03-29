import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

class Link implements Serializable{
    public String title;
    public String url;
    public String citation;
}

public class BarrelMulticast extends MulticastSocket implements IBarrel,Serializable {
    String ipAddress;
    ArrayList<Integer> packetCounter = new ArrayList<>();


    //db structures
    private HashMap<String, HashSet<String>> wordLinkMap = new HashMap<>();
    private HashMap<String, HashSet<String>> linkLinkMap = new HashMap<>();
    Link currentLink = new Link();


    private static final String PROPERTIES_FILE_PATH = "./src/resources/System.properties";
    private static final int BUFFER_SIZE = 1024;
    private IGateBarrel gateway;
    private static String filePath;

    private static int barrelID;
    public BarrelMulticast() throws IOException{
        super(getPortFromProperties());
        Registry registry = LocateRegistry.getRegistry("localhost", 1098);
        try {
            gateway = (IGateBarrel) registry.lookup("GatewayBarrel");
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
        barrelID=gateway.subscribeBarrel(this);
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(PROPERTIES_FILE_PATH)) {
            properties.load(input);
        }

        ipAddress = properties.getProperty("ipAddress");

        filePath= "output"+barrelID+".txt";
    }

    public void printWordLinkMap() {
        System.out.println("Contents of wordLinkMap:");
        for (Map.Entry<String, HashSet<String>> entry : wordLinkMap.entrySet()) {
            System.out.println("Word: " + entry.getKey());
            System.out.println("Links:");
            for (String link : entry.getValue()) {
                System.out.println("\t" + link);
            }
        }
    }

    // Method to print the contents of linkLinkMap
    public void printLinkLinkMap() {
        System.out.println("Contents of linkLinkMap:");
        for (Map.Entry<String, HashSet<String>> entry : linkLinkMap.entrySet()) {
            System.out.println("Link: " + entry.getKey());
            System.out.println("Linked Links:");
            for (String linkedLink : entry.getValue()) {
                System.out.println("\t" + linkedLink);
            }
        }
    }


    private static int getPortFromProperties() throws IOException {
        try (FileInputStream input = new FileInputStream(PROPERTIES_FILE_PATH)) {
            Properties properties = new Properties();
            properties.load(input);
            return Integer.parseInt(properties.getProperty("port"));
        }
    }




    public void updateWordHashMap(String word, String link) {
        wordLinkMap.computeIfAbsent(word, k -> new HashSet<>()).add(link);
    }

    public void updateLinkHashMap(String currentLink, String linkToAdd) {
        linkLinkMap.computeIfAbsent(currentLink, k -> new HashSet<>()).add(linkToAdd);
    }

    public void receiveMessage() throws IOException {


        InetAddress group = InetAddress.getByName(ipAddress);
        this.joinGroup(group);

        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {

            this.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            //System.out.println("Received: " + received);
            processMessage(received);

        }

    }

    //Protocolo: packetID|downloader|downloaderID|LinkAtual|words/links|....
    //Protocolo:     0   |     1    |      2     |    3    |      4    |....
    private void processMessage(String message) throws IOException {
        String[] parts = message.split("\\|");

        if (parts.length < 6 || !parts[1].equals("downloader")) {
            return;
        }
        int packetID = Integer.parseInt(parts[0]);
        int downloaderID = Integer.parseInt(parts[2]);
        String currentLink = parts[3];
        String type = parts[4];
        if(downloaderID+1>packetCounter.size())packetCounter.add(-1);

        //Se o packet ID que chegou não corresponde ao atual
        //packetID-packetIDanterior <=1 ta td bem
        //portanto se packetID-packetIDanterior >1 n ta bem
        if (packetID - packetCounter.get(downloaderID) > 1) {//Falhou pelo menos um pacote
            //mecanismo de recuperação
        } else {
            packetCounter.set(downloaderID, packetCounter.get(packetID));
            if (filePath != null) {
                FileWriter fileWriter = new FileWriter(filePath);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(message);
                bufferedWriter.write("*");
            } else {
                System.err.println("File path is null. Cannot create FileWriter.");
            }

        }

        if (type.equals("links")) {
            for (int i = 5; i < parts.length; i++) {
                updateLinkHashMap(currentLink, parts[i]);//adicionar a link atual->link q aponta pra ele
            }

        } else if (type.equals("words")) {
            for (int i = 5; i < parts.length; i++) {
                updateWordHashMap(parts[i], currentLink);//adicionar a palavra->link atual
            }

        }
    }

    public static void main(String[] args) throws IOException {
        BarrelMulticast barrel = new BarrelMulticast();
        barrel.receiveMessage();
    }
}
