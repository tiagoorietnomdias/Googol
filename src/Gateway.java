import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;


public class Gateway extends UnicastRemoteObject implements IGateDownloader, IGateBarrel, IGateClient {

    public ArrayList<IDownloader> downloaders;
    public ArrayList<IBarrel> barrels;

    public ArrayList<ICliente> clients;
    public int queue[];
    ArrayDeque<String> linkQueue = new ArrayDeque<>();

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


        barrelRegistry.rebind("GatewayBarrel", this);
        downloaderRegistry.rebind("GatewayDownloader", this);
    }


    @Override
    public int subscribeDownloader(IDownloader d) throws RemoteException {
        downloaders.add(d);
        return downloaders.indexOf(d);
    }

    @Override
    public int subscribeBarrel(IBarrel b) throws RemoteException {
        barrels.add(b);
        return barrels.indexOf(b);
    }
    @Override
    public void renewBarrelState(int barrelID, IBarrel barrel)throws RemoteException{
        barrels.set(barrelID, barrel);
    }

    @Override
    public int subscribeClient(ICliente c) throws RemoteException {
        clients.add(c);
        return clients.indexOf(c);
    }

    @Override
    public ArrayList<Integer> getActiveBarrels() throws RemoteException{
        ArrayList<Integer> activeBarrels= new ArrayList<>();
        for (IBarrel barrel: barrels)activeBarrels.add(barrels.indexOf(barrel));
        return activeBarrels;
    }

    @Override
    public List<String> getTop10Searches() throws RemoteException{
        HashMap<String,Integer> top10= new HashMap<>();
        for (IBarrel barrel:barrels){
            top10=barrel.getNumberOfSearches();
            break;
        }
        System.out.println(top10.size());
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(top10.entrySet());

        entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        List<String> sortedKeys = new ArrayList<>();
        int contador=0;
        for (Map.Entry<String, Integer> entry : entryList) {
            contador+=1;
            sortedKeys.add("Top "+contador+": "+entry.getKey()+"\n");
        }

        System.out.println(sortedKeys);
    return sortedKeys;
    }

    @Override
    public void putLinksInQueue(List<String> links) throws RemoteException {
        for (String link : links) {
            linkQueue.addFirst(link);
        }
    }

    @Override
    public String getLastLink() throws RemoteException {
        return linkQueue.removeLast();
    }

    @Override
    public HashMap<String, HashSet<String>> getupdatedWordMap(int barrelIDRequesting) throws RemoteException {
        int i = 0;
        HashMap<String, HashSet<String>> updatedWordMap=null;
        for (IBarrel barrel : barrels) {
            if (barrel.returnUpToDateState() && (i != barrelIDRequesting)) {
                updatedWordMap = barrel.getWordLinkMap();
                break;
            }
            i++;
        }
        return updatedWordMap;
    }

    @Override
    public HashMap<String, HashSet<String>> getupdatedLinkMap(int barrelIDRequesting) throws RemoteException{
        int i = 0;
        HashMap<String, HashSet<String>> updatedLinkMap=null;
        for (IBarrel barrel : barrels) {
            if (barrel.returnUpToDateState() && (i != barrelIDRequesting)) {
                updatedLinkMap = barrel.getLinkLinkMap();
            break;

            }
            i++;
        }
        return updatedLinkMap;
    }
    @Override
    public ArrayList<String> pesquisa(String wordToSearch) throws RemoteException{
            ArrayList<Link> results= new ArrayList<>();
            ArrayList<String> coolResults = new ArrayList<>();
            if (wordToSearch.startsWith("http://")||wordToSearch.startsWith("https://")) {

                //link
                results = barrels.get(0).searchLink(wordToSearch);

                for (Link link : results){

                    String string ="URL: " + link.getUrl() + "\n";
                    coolResults.add(string);
                }
               // System.out.println(coolResults.size());


            } else {
                results = barrels.get(0).searchWord(wordToSearch);
                results.sort(Comparator.comparingInt(Link::getRank).reversed());
                for (Link link : results){

                    String string ="Title: " + link.title + "\n" + "URL: " + link.getUrl() + "\n" + "Citation: " + link.citation + "\n";
                    coolResults.add(string);
                }

             //   System.out.println(results.size());



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
