/**
 * The `Barrel` class in Java implements a system for managing and processing links and words,
 * including methods for searching and updating link and word maps.
 */


import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

/**
 * The `Link` class in Java represents a link with properties such as title, URL, citation, and rank.
 */
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

/**
 * The `Barrel` class in Java extends `MulticastSocket`, implements `IBarrel` and `Serializable`, and
 * contains various data structures and methods for processing and storing information related to links
 * and words.
 */
public class Barrel extends MulticastSocket implements IBarrel, Serializable {
    String sendIpAddress;
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

    private boolean isdead = false;

    public Barrel() throws IOException, RemoteException {
        super(getPortFromProperties());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown(barrelID);
            System.out.println("Barrel " + barrelID + "just died. Here's proof: " + getBarrelStatus());
        }));
        sendIpAddress = getIpAddressFromProperties();
        while (true) {
            try {
                Registry registry = LocateRegistry.getRegistry(getHostFromProperties(), getRMIPortFromProperties());
                gateway = (IGateBarrel) registry.lookup(getBarrelGatewayFromProperties());

            } catch (NotBoundException e) {
                System.out.println("Properties file not properly setup.");
                System.exit(0);
            } catch (RemoteException e) {
                try {
                    System.out.println("Barrel" + barrelID + " couldn't connect, waiting 3 second and retrying...");
                    Thread.sleep(3000);
                    System.out.println("Resuming Connection...");
                    continue;

                } catch (InterruptedException ie) {
                    System.out.println("Barrel" + barrelID + " stopped while waiting");
                }
            }
            break;
        }
        barrelID = gateway.subscribeBarrel(this);
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(PROPERTIES_FILE_PATH)) {
            properties.load(input);
        } catch (IOException e) {
            System.out.println("Couldn't open " + PROPERTIES_FILE_PATH);
        }
        sendIpAddress = getIpAddressFromProperties();


        filePath = "output" + barrelID + ".txt";
        File f = new File(filePath);
        wordLinkMap = gateway.getupdatedWordMap(barrelID);
        linkLinkMap = gateway.getupdatedLinkMap(barrelID);
        linkInfoMap = gateway.getupdatedInfoMap(barrelID);
        if ((wordLinkMap == null && linkLinkMap == null) || !f.exists() || (wordLinkMap.size() == 0 && linkLinkMap.size() == 0)) {
            System.out.println("No barrels with updated info..reading text file");
            this.loadFromTxt(filePath);

        }
        gateway.renewBarrelState(barrelID, this);
        System.out.println(filePath);
        this.receiveMessage();
    }

    public void loadFromTxt(String filename) {
        String message = null;
        try {

            FileReader fr = new FileReader(filename);
            BufferedReader bf = new BufferedReader(fr);
            while (bf.readLine() != null) {
                message = bf.readLine();
                if (message == null) {
                    return;
                }
                String[] parts = message.split("\\|");

                if (parts.length < 6 || !parts[1].equals("downloader")) {
                    return;
                }
                int packetID = Integer.parseInt(parts[0]);
                int downloaderID = Integer.parseInt(parts[2]);
                String currentLink = parts[3];
                String type = parts[4];

                if (type.equals("links")) {
                    for (int i = 5; i < parts.length; i++) {
                        updateLinkHashMap(currentLink, parts[i]);//adicionar a link atual->link q aponta pra ele
                    }

                } else if (type.equals("words")) {
                    Link linkToAdd = new Link();
                    linkToAdd.title = parts[6];
                    linkToAdd.url = parts[3];
                    linkInfoMap.add(linkToAdd);
                    StringBuilder citation = new StringBuilder();
                    for (int i = 6; i < parts.length; i++) {
                        if (i < 50) {
                            citation.append(parts[i]);
                            citation.append(" ");
                            if(i==25)citation.append("\n");
                        }
                        updateWordHashMap(parts[i], currentLink);//adicionar a palavra->link atual
                    }
                    linkToAdd.citation = String.valueOf(citation);

                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error reading file");
        }

    }

    /**
     * The function `getWordLinkMap` returns a HashMap where each key is a String and each value is a
     * HashSet of Strings.
     *
     * @return A HashMap<String, HashSet<String>> named wordLinkMap is being returned.
     */
    public HashMap<String, HashSet<String>> getWordLinkMap() {
        return wordLinkMap;
    }

    /**
     * The function `getLinkLinkMap` returns a HashMap with keys of type String and values of type
     * HashSet<String>.
     *
     * @return A HashMap with keys of type String and values of type HashSet<String> is being returned.
     */
    public HashMap<String, HashSet<String>> getLinkLinkMap() {
        return linkLinkMap;
    }

    /**
     * The function returns a HashSet containing Link objects representing link information.
     *
     * @return A HashSet containing Link objects is being returned.
     */
    public HashSet<Link> getLinkInfoMap() {
        return linkInfoMap;
    }

    /**
     * The function `printWordLinkMap` prints the contents of a map where each word is associated with
     * a set of links.
     */
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

    /**
     * The function `printLinkLinkMap` prints the contents of a map where each key is a link and the
     * corresponding value is a set of linked links.
     */
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


    /**
     * The function `getPortFromProperties` reads a properties file to retrieve and return the port
     * number specified in the file, handling exceptions by printing an error message and exiting if
     * the file cannot be loaded.
     *
     * @return The method `getPortFromProperties` is returning an integer value, which is the port
     * number read from the properties file specified by the key "port". If there is an IOException
     * while reading the properties file, the method will return -1.
     */
    private static int getPortFromProperties() throws IOException {
        try (FileInputStream input = new FileInputStream(PROPERTIES_FILE_PATH)) {
            Properties properties = new Properties();
            properties.load(input);
            return Integer.parseInt(properties.getProperty("port"));
        } catch (IOException e) {
            System.out.println("Couldn't load \"" + PROPERTIES_FILE_PATH + "\". Make sure the file is available");
            System.exit(1);
            return -1;
        }

    }
    private static int getRMIPortFromProperties() throws IOException {
        return Integer.parseInt(loadProperties("./src/resources/System.properties").getProperty("barrelPort"));
    }
    private static String getHostFromProperties() throws IOException {
        return loadProperties("./src/resources/System.properties").getProperty("host");
    }
    private static Properties loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(filename)) {
            properties.load(input);
        } catch (IOException e) {
            System.out.println("Couldn't load \"" + filename + "\". Make sure the file is available");
        }
        return properties;
    }
    private static String getIpAddressFromProperties() throws IOException {
        return loadProperties("./src/resources/System.properties").getProperty("sendIpAddress");
    }
    private static String getBarrelGatewayFromProperties() throws IOException {
        return loadProperties("./src/resources/System.properties").getProperty("barrelRegistry");
    }

    /**
     * The `renewBarrel` function in Java returns an `IBarrel` object and may throw a
     * `RemoteException`.
     *
     * @return The `renewBarrel` method is returning an object that implements the `IBarrel` interface.
     * In this case, it is returning the current object (`this`).
     */
    @Override
    public IBarrel renewBarrel() throws RemoteException {

        return this;
    }

    /**
     * The `updateWordHashMap` function adds a link to a HashSet associated with a word in a HashMap if
     * the word is not already present in the HashMap.
     *
     * @param word The `word` parameter is a String representing a word that will be used as a key in a
     *             HashMap.
     * @param link The `link` parameter in the `updateWordHashMap` method is a String that represents a
     *             link associated with a particular word.
     */
    public void updateWordHashMap(String word, String link) {
        if (wordLinkMap == null) {
            wordLinkMap.put(word, new HashSet<>()).add(link);
        } else {
            wordLinkMap.computeIfAbsent(word, k -> new HashSet<>()).add(link);
        }
    }

    /**
     * The `updateLinkHashMap` function adds a link to a HashSet associated with a given key in a
     * HashMap.
     *
     * @param currentLink The `currentLink` parameter is a String that represents the key in the
     *                    `linkLinkMap` HashMap where we want to add a new link.
     * @param linkToAdd   The `linkToAdd` parameter is a String representing the link that you want to
     *                    add to the HashSet associated with the `currentLink` key in the `linkLinkMap`.
     */
    public void updateLinkHashMap(String currentLink, String linkToAdd) {
        if (linkLinkMap == null) {

            linkLinkMap.put(currentLink, new HashSet<>()).add(linkToAdd);
        } else {
            linkLinkMap.computeIfAbsent(currentLink, k -> new HashSet<>()).add(linkToAdd);
        }
    }

    /**
     * The `receiveMessage` function in Java listens for incoming messages over a network using
     * DatagramSocket and processes the received data.
     */
    public void receiveMessage() throws IOException {

        InetAddress group = InetAddress.getByName(sendIpAddress);
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

    /**
     * The function `returnUpToDateState` returns the value of the `isUpToDate` boolean variable.
     *
     * @return The method `returnUpToDateState` is returning the value of the boolean variable
     * `isUpToDate`.
     */
    public boolean returnUpToDateState() {
        return isUpToDate;
    }

    /**
     * The `acknowledgeReception` method sends a UDP packet containing the message "shuptidu" to a
     * specified IP address and port.
     */
    private void acknowledgeReception(int downloaderID) throws IOException {
        String message = "ack " + downloaderID;
        InetAddress sendgroup = InetAddress.getByName(sendIpAddress);
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, sendgroup, getPortFromProperties());
        this.send(packet);
        //String ackMessage = new String(packet.getData(), 0, packet.getLength());
        //System.out.println("here's what i sent"+ackMessage);

    }

    //Protocolo: packetID|downloader|downloaderID|LinkAtual|words/links|....
    //Protocolo:     0   |     1    |      2     |    3    |      4    |....

    /**
     * The `processMessage` function in Java processes a message, updates packet and file information,
     * and handles different types of data based on the message content.
     *
     * @param message The `processMessage` method takes a `String` message as input and processes it
     *                according to the specified logic. The message is split into parts using the pipe character as
     *                the delimiter.
     */
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
                linkInfoMap = gateway.getupdatedInfoMap(barrelID);
                System.out.println("Something missed but I am now updated");

            } while (gateway.getupdatedWordMap(barrelID) == null || gateway.getupdatedLinkMap(barrelID) == null || gateway.getupdatedInfoMap(barrelID) == null);
            packetCounter.set(downloaderID, packetID);
        } else {
            acknowledgeReception(downloaderID);
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

        }else if (type.equals("words")) {
            Link linkToAdd = new Link();
            linkToAdd.title = parts[6];
            linkToAdd.url = parts[3];
            linkInfoMap.add(linkToAdd);
            StringBuilder citation = new StringBuilder();
            for (int i = 6; i < parts.length; i++) {
                if (i < 50) {
                    citation.append(parts[i]);
                    citation.append(" ");
                    if(i==25)citation.append("\n");
                }
                updateWordHashMap(parts[i], currentLink);//adicionar a palavra->link atual
            }
            linkToAdd.citation = String.valueOf(citation);

        }
    }

    /**
     * This Java function returns a HashMap containing the number of searches.
     *
     * @return The method `getNumberOfSearches` is returning a `HashMap` with keys of type `String` and
     * values of type `Integer`.
     */
    // The above Java code is implementing two methods in a remote interface.
    @Override
    public HashMap<String, Integer> getNumberOfSearches() throws RemoteException {
        //gateway.renewBarrelState(barrelID, this);
        return numberOfSearches;
    }

    /**
     * The `searchWord` function in Java processes a search query, updates search counts, retrieves
     * associated links, calculates ranks, and renews state using a gateway.
     *
     * @param wordstoSearch The `searchWord` method you provided seems to be a part of a remote service
     *                      that searches for links associated with a given set of words. The method splits the input
     *                      `wordstoSearch` into individual words, keeps track of the number of searches for each word, and
     *                      then searches for links associated
     * @return The method `searchWord` is returning an ArrayList of Link objects.
     */
    @Override
    public ArrayList<Link> searchWord(String wordstoSearch) throws RemoteException {

        String[] words = wordstoSearch.split(" ");

        if (numberOfSearches.containsKey(wordstoSearch)) {
            int count = numberOfSearches.get(wordstoSearch);
            numberOfSearches.put(wordstoSearch, count + 1);
        } else {
            numberOfSearches.put(wordstoSearch, 1);
        }

        System.out.println("tamanho do hashmap:" + numberOfSearches.size());
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
                            System.out.println(link.getUrl());
                            System.out.println(linkLinkMap.get(link.getUrl()));
                            if (linkLinkMap.get(link.getUrl()) != null)
                                link.rank = linkLinkMap.get(link.getUrl()).size();
                            else link.rank = 0;
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
        gateway.updateNumberofSearches(barrelID, this);
        return finalList;
    }


    /**
     * The `searchLink` function searches for a specified link in a map and returns a list of
     * associated links.
     *
     * @param linktoSearch The `searchLink` method you provided takes a `String` parameter named
     *                     `linktoSearch`, which represents the link that you want to search for in the `linkLinkMap`. The
     *                     method then retrieves the associated links for the given input link and returns a list of `Link`
     *                     objects containing those
     * @return The `searchLink` method returns an ArrayList of Link objects that are associated with
     * the input `linktoSearch`. If the input `linktoSearch` is found in the `linkLinkMap`, the method
     * retrieves the associated links from the map, creates Link objects for each associated link, adds
     * them to the finalList, and returns the finalList of Link objects. If the input `linkto
     */
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

    public void setNumberOfSearches(HashMap<String, Integer> newNumberOfSearches){
        this.numberOfSearches = newNumberOfSearches;
    }


    public boolean getBarrelStatus() {
        return isdead;
    }


    public void shutdown(int bID) {
        this.isdead = true;
        try {
            gateway.shutdownBarrel(barrelID, this);
        } catch (RemoteException | NullPointerException e) {
            System.out.println("Error shutting down the barrel");
            System.exit(0);
        }
    }

    public static void main(String[] args) throws IOException {
        boolean crash = false;
        int bID = 0;
        while (true) {

            //try {
            Barrel barrel = new Barrel();
            if (crash) {
                barrel.shutdown(bID);
            }
            bID = barrelID;
            if (crash) {
                barrel.shutdown(bID);
            }
                /*
            } catch (RuntimeException e) {
                System.out.println("Barrel crashed, rebooting");
                crash = true;
                continue;
            }

                 */
            break;
        }
    }
}