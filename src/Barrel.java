import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

class Link implements Serializable {
    public String title;
    public String url;
    public String citation;

    public int rank;

    public String getUrl() {
        return url;
    }

    public int getRank() {
        return rank;
    }
}

public class Barrel extends MulticastSocket implements IBarrel, Serializable {
    String ipAddress;
    ArrayList<Integer> packetCounter = new ArrayList<>();


    //db structures
    public HashMap<String, HashSet<String>> wordLinkMap = new HashMap<>();
    private HashMap<String, HashSet<String>> linkLinkMap = new HashMap<>();

    private HashSet<Link> linkInfoMap = new HashSet<>();

    HashMap<String, Integer> numberOfSearches = new HashMap<>();
    private boolean isUpToDate = true;

    private static final String PROPERTIES_FILE_PATH = "./src/resources/System.properties";

    private static final int BUFFER_SIZE = 1024;
    private IGateBarrel gateway;
    private static String filePath;

    private static int barrelID;

    public Barrel() throws IOException {
        super(getPortFromProperties());
        Registry registry = LocateRegistry.getRegistry("localhost", 1098);
        try {
            gateway = (IGateBarrel) registry.lookup("GatewayBarrel");
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
        barrelID = gateway.subscribeBarrel(this);
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(PROPERTIES_FILE_PATH)) {
            properties.load(input);
        }
        ipAddress = properties.getProperty("ipAddress");
        filePath = "output" + barrelID + ".txt";
        System.out.println(filePath);
    }

    public HashMap<String, HashSet<String>> getWordLinkMap() {
        return wordLinkMap;
    }

    public HashMap<String, HashSet<String>> getLinkLinkMap() {
        return linkLinkMap;
    }

    public HashSet<Link> getLinkInfoMap() {
        return linkInfoMap;
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
    @Override
    public IBarrel renewBarrel()throws RemoteException{

        return this;
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
            gateway.renewBarrelState(barrelID, this);

        }

    }

    public boolean returnUpToDateState() {
        return isUpToDate;
    }

    private void acknowledgeReception() throws IOException {
        String message = "shuptidu";
        InetAddress group = InetAddress.getByName(ipAddress);
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, group, getPortFromProperties());
        this.send(packet);
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
        if (downloaderID + 1 > packetCounter.size()) packetCounter.add(-1);


        if (packetID - packetCounter.get(downloaderID) > 1) {//Falhou pelo menos um pacote
            //mecanismo de recuperação
            isUpToDate = false;
            ///req to gateway
            do {
                wordLinkMap = gateway.getupdatedWordMap(barrelID);
                linkLinkMap = gateway.getupdatedLinkMap(barrelID);

            } while (gateway.getupdatedWordMap(barrelID) != null && gateway.getupdatedLinkMap(barrelID) != null);
            packetCounter.set(downloaderID, packetID);
            //update
            System.out.println("packetID" + packetID);
            System.out.println("current index in array" + packetCounter.get(downloaderID));
        } else {
            acknowledgeReception();
            packetCounter.set(downloaderID, packetID);
            if (filePath != null) {
                try {
                    FileWriter fileWriter = new FileWriter(filePath, true); // 'true' indicates append mode
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    System.out.println(message);
                    bufferedWriter.write(message);
                    bufferedWriter.newLine(); // Writes a newline character
                    bufferedWriter.flush(); // Flushes the buffer
                    bufferedWriter.close(); // Closes the writer
                    // System.out.println(wordLinkMap);
                } catch (IOException e) {
                    System.err.println("Error writing to file: " + e.getMessage());
                    e.printStackTrace();
                }

            } else {
                System.err.println("File path is null. Cannot create FileWriter.");
            }

        }

        if (type.equals("links")) {
            for (int i = 5; i < parts.length; i++) {
                updateLinkHashMap(currentLink, parts[i]);//adicionar a link atual->link q aponta pra ele
            }

        } else if (type.equals("words")) {
            Link linkToAdd = new Link();
            linkToAdd.title = parts[6];
            linkToAdd.url = parts[3];
            linkInfoMap.add(linkToAdd);
            for (int i = 6; i < parts.length; i++) {
                updateWordHashMap(parts[i], currentLink);//adicionar a palavra->link atual
            }

        }
    }

    @Override
    public HashMap<String, Integer> getNumberOfSearches() throws RemoteException {
        //gateway.renewBarrelState(barrelID, this);
        return numberOfSearches;
    }

    @Override
    public ArrayList<Link> searchWord(String wordstoSearch) throws RemoteException {

        String[] words = wordstoSearch.split(" ");

        if (numberOfSearches.containsKey(wordstoSearch)) {
            int count = numberOfSearches.get(wordstoSearch);
            numberOfSearches.put(wordstoSearch, count + 1);
        } else {
            numberOfSearches.put(wordstoSearch, 1);
        }

        System.out.println("tamanho do hashmap:"+numberOfSearches.size());
        for (String key : numberOfSearches.keySet()) {
            System.out.println(key);
        }

        ArrayList<Link> finalList = new ArrayList<>();
        HashSet<String> associatedLinks;
        if (wordLinkMap.containsKey(words[0])) {
            associatedLinks = wordLinkMap.get(words[0]);
            for (String l : associatedLinks) {
                boolean linkExists = true;
                for (int j = 1; j < words.length; j++) {
                    if (wordLinkMap.get(words[j]).contains(l)) {
                        linkExists = false;
                        System.out.println("link exists false");
                        break;
                    }
                }
                if (linkExists) {
                    for (Link link : linkInfoMap) {
                        if (link.getUrl().equals(l)) {
                            link.rank = linkLinkMap.get(l).size();
                            //System.out.println("Rank:"+link.rank);
                            finalList.add(link);
                            break;
                        }

                    }
                }

            }
            System.out.println("Values associated with the word '" + wordstoSearch + "': " + finalList);
        }
        gateway.renewBarrelState(barrelID, this);
        return finalList;
    }


    public ArrayList<Link> searchLink(String linktoSearch) {
        ArrayList<Link> finalList = new ArrayList<>();
        HashSet<String> associatedLinks;
        if (linkLinkMap.containsKey(linktoSearch)) {
            associatedLinks = linkLinkMap.get(linktoSearch);
            //System.out.println(associatedLinks);
            for (String link : associatedLinks) {
                Link erm = new Link();
                erm.url = link;
                finalList.add(erm);
            }

        }
        return finalList;
    }

    public static void main(String[] args) throws IOException {
        Barrel barrel = new Barrel();
        barrel.receiveMessage();
    }
}
