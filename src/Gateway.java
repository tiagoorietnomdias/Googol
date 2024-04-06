/**
 * The `Gateway` class in Java implements remote interfaces for managing downloaders, barrels, and
 * clients in a distributed system.
 */
import java.io.IOException;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class Gateway extends UnicastRemoteObject implements IGateDownloader, IGateBarrel, IGateClient {

    public ArrayList<IDownloader> downloaders;
    //private final Lock queueLock;
    public ArrayList<IBarrel> barrels;

    public ArrayList<ICliente> clients;
    public int queue[];
    BlockingDeque<String> linkQueue = new LinkedBlockingDeque<>();


    // The `public Gateway() throws IOException, AlreadyBoundException { ... }` constructor in the
// `Gateway` class is responsible for initializing the Gateway object. Here's a breakdown of what it
// does:
    public Gateway() throws IOException, AlreadyBoundException {
        super();
        //String serverHostname = "192.168.245.239";
        //System.setProperty("java.rmi.server.hostname", serverHostname);

        queue = new int[10];
        downloaders = new ArrayList<>();
        barrels = new ArrayList<>();
        clients = new ArrayList<>();
        Registry downloaderRegistry = LocateRegistry.createRegistry(1099);
        Registry barrelRegistry = LocateRegistry.createRegistry(1098);
        Registry clientRegistry = LocateRegistry.createRegistry(1100); //registry para o cliente


        barrelRegistry.rebind("GatewayBarrel", this);
        downloaderRegistry.rebind("GatewayDownloader", this);
        clientRegistry.rebind("GatewayClient", this);
    }


    /**
     * The `subscribeDownloader` method adds a downloader to a list and returns its index in the list.
     *
     * @param d The parameter `d` in the `subscribeDownloader` method is of type `IDownloader`, which is an
     * interface representing a downloader.
     * @return The method `subscribeDownloader` is adding a downloader object `d` to a list called
     * `downloaders` and then returning the index of the added downloader in the `downloaders` list.
     */
    @Override
    public int subscribeDownloader(IDownloader d) throws RemoteException {
        downloaders.add(d);
        return downloaders.indexOf(d);
    }

    /**
     * The `subscribeBarrel` function adds a barrel to a list and returns its index in the list.
     *
     * @param b IBarrel b is an object of type IBarrel, which is being passed as a parameter to the
     * subscribeBarrel method.
     * @return The method `subscribeBarrel(IBarrel b)` is returning the index of the `IBarrel` object `b`
     * after adding it to the `barrels` list.
     */

    @Override
    public int subscribeBarrel(IBarrel b) throws RemoteException {
        barrels.add(b);
        return barrels.indexOf(b);
    }
    /**
     * The `renewBarrelState` function updates the state of a barrel with a given ID in a collection of
     * barrels.
     *
     * @param barrelID The `barrelID` parameter is an integer value that represents the unique identifier
     * of a barrel in the system.
     * @param barrel The `barrel` parameter in the `renewBarrelState` method is an object of type
     * `IBarrel`. It represents the updated state of a barrel that needs to be renewed in the system.
     */

    @Override
    public void renewBarrelState(int barrelID, IBarrel barrel) throws RemoteException {
        barrels.set(barrelID, barrel);
    }


    /**
     * This Java function adds a client to a list and returns the index of the added client.
     *
     * @param c The parameter `c` in the `subscribeClient` method is of type `ICliente`, which represents a
     * client interface.
     * @return The method `subscribeClient` is adding a client to a list called `clients` and then
     * returning the index of the added client in the list.
     */
    @Override
    public int subscribeClient(ICliente c) throws RemoteException {
        clients.add(c);
        return clients.indexOf(c);
    }

    /**
     * The function `getActiveBarrels` returns a list of indices of active barrels in an ArrayList.
     *
     * @return An ArrayList of integers representing the indices of active barrels.
     */
    @Override
    public ArrayList<Integer> getActiveBarrels() throws RemoteException {
        ArrayList<Integer> activeBarrels = new ArrayList<>();
        for (IBarrel barrel : barrels) activeBarrels.add(barrels.indexOf(barrel));
        return activeBarrels;
    }

    /**
     * The function `getTop10Searches` retrieves the top 10 search terms from a collection of barrels and
     * returns them in a sorted list.
     *
     * @return The `getTop10Searches` method is returning a list of the top 10 search terms based on the
     * number of searches in descending order. The list contains strings in the format "Top [rank]: [search
     * term]".
     */
    @Override
    public List<String> getTop10Searches() throws RemoteException {
        HashMap<String, Integer> top10 = new HashMap<>();
        for (IBarrel barrel : barrels) {
            top10 = barrel.getNumberOfSearches();
            break;
        }
        System.out.println(top10.size());
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(top10.entrySet());

        entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        List<String> sortedKeys = new ArrayList<>();
        int contador = 0;
        for (Map.Entry<String, Integer> entry : entryList) {
            contador += 1;
            sortedKeys.add("Top " + contador + ": " + entry.getKey() + "\n");
        }

        System.out.println(sortedKeys);
        return sortedKeys;
    }

    /**
     * The `insertInQueue` method adds a URL to a queue if it starts with "https://" or "http://".
     *
     * @param linkToInsert The `linkToInsert` parameter is a `String` representing a URL that is being
     * inserted into a queue. The method checks if the URL starts with either "https://" or "http://"
     * before adding it to the queue. If the URL does not start with these prefixes, it prints a message
     */
    @Override
    public void insertInQueue(String linkToInsert) throws RemoteException {
        try {
        if (linkToInsert.startsWith("https://") || linkToInsert.startsWith("http://")) {
            linkQueue.addFirst(linkToInsert);
            System.out.println(linkQueue.getFirst());
        } else {
            System.out.println("Please insert a valid URL");
        }
        } catch (NoSuchElementException e) {
            // Handle NoSuchElementException
            System.out.println("Please retry! There was a server error");
        }
    }

    /**
     * The `putLinksInQueue` method adds a list of links to a queue in Java.
     *
     * @param links A list of strings containing links that need to be added to a queue.
     */
    @Override
    public void putLinksInQueue(List<String> links) throws RemoteException {
        for (String link : links) {
            linkQueue.addFirst(link);
        }
    }

    /**
     * The `getLastLink` function overrides a method to retrieve and return the last link from a queue in
     * Java, potentially throwing RemoteException and InterruptedException.
     *
     * @return The method is returning the last link from the `linkQueue` data structure.
     */
    @Override
    public String getLastLink() throws RemoteException, InterruptedException {
        String link = linkQueue.takeLast();
        return link;

    }


    /**
     * This Java function iterates through a list of barrels to find an up-to-date state and returns the
     * word link map from the first barrel that meets the criteria.
     *
     * @param barrelIDRequesting The `barrelIDRequesting` parameter is an integer value representing the ID
     * of the barrel requesting the updated word map.
     * @return The method `getupdatedWordMap` returns a `HashMap` where the keys are `String` and the
     * values are `HashSet<String>`.
     */
    @Override
    public HashMap<String, HashSet<String>> getupdatedWordMap(int barrelIDRequesting) throws RemoteException {
        int i = 0;
        HashMap<String, HashSet<String>> updatedWordMap = null;
        for (IBarrel barrel : barrels) {
            if (barrel.returnUpToDateState() && (i != barrelIDRequesting)) {
                updatedWordMap = barrel.getWordLinkMap();
                break;
            }
            i++;
        }
        return updatedWordMap;
    }

    /**
     * The function `getupdatedLinkMap` returns a HashMap of String keys to HashSet values from a
     * collection of barrels, excluding the barrel specified by ID that is not up to date.
     *
     * @param barrelIDRequesting The `barrelIDRequesting` parameter is an integer representing the ID of
     * the barrel requesting the updated link map.
     * @return This method returns a HashMap<String, HashSet<String>> containing updated link information
     * from a specific barrel identified by the barrelIDRequesting parameter.
     */
    @Override
    public HashMap<String, HashSet<String>> getupdatedLinkMap(int barrelIDRequesting) throws RemoteException {
        int i = 0;
        HashMap<String, HashSet<String>> updatedLinkMap = null;
        for (IBarrel barrel : barrels) {
            if (barrel.returnUpToDateState() && (i != barrelIDRequesting)) {
                updatedLinkMap = barrel.getLinkLinkMap();
                break;

            }
            i++;
        }
        return updatedLinkMap;
    }

    /**
     * This Java function searches for a given word or URL in a collection of links and returns
     * relevant information in a formatted string.
     *
     * @param wordToSearch The `pesquisa` method you provided is used to search for either a link or a
     * word in a collection of links. If the `wordToSearch` parameter starts with "http://" or
     * "https://", it is treated as a link and searched accordingly. Otherwise, it is treated as a
     * @return The method `pesquisa` is returning an `ArrayList` of `String` objects named
     * `coolResults`.
     */
    @Override
    public ArrayList<String> pesquisa(String wordToSearch) throws RemoteException {
        ArrayList<Link> results = new ArrayList<>();
        ArrayList<String> coolResults = new ArrayList<>();
        try{
        if (wordToSearch.startsWith("http://") || wordToSearch.startsWith("https://")) {

            //link
            results = barrels.get(0).searchLink(wordToSearch);

            for (Link link : results) {

                String string = "URL: " + link.getUrl() + "\n";
                coolResults.add(string);
            }
            // System.out.println(coolResults.size());


        } else {
            results = barrels.get(0).searchWord(wordToSearch);
            results.sort(Comparator.comparingInt(Link::getRank).reversed());
            for (Link link : results) {

                String string = "Title: " + link.title + "\n" + "URL: " + link.getUrl() + "\n" + "Citation: " + link.citation + "\n";
                coolResults.add(string);
            }

            //   System.out.println(results.size());


        }}
        catch (IndexOutOfBoundsException e){
            System.out.println("Server is currently down, try again later");
            return null;
        }
        //Construir string a retornar a client
        return coolResults;
    }

    public static void main(String args[]) {
        try {
            Gateway gateway = new Gateway();
        } catch (IOException | AlreadyBoundException e) {
            throw new RuntimeException(e);
        }

    }
}
