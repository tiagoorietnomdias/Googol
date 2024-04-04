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



            } else {
                //word
                //obter todos os links que no seu hashmap tenham a palavra
                //mete-los numa arraylist
                //Agrupar de 10 em 10
                //Para agrupar de 10 em 10 agrupar em gateway ou em cliente?
                // Eufras: na gateway, é preciso calcular o score da correspondencia Sim, mas primeiro temos de obter as cenas
                //so depois é que rankeamos porque nao calcular o rank assim que encontramos correspondencia? é isso q vamos fazer
                //mas pra ja queria so ter o requisito de pesquisar a funfar antes de os ordenar
                //Cada entrada tem de conter link, titulo e citação com a palavra|||
                //Assim que encontramos no hashmap, pesquiisar no hashset de links pela palavra
                //Obter titulo, link,
                //Como criar citação? obter texto do link com jsoup. Criar citação a partir de texto
                //A clodia e a mj so foram buscar 50 carateres atras e a frente da palabra pra obter
                //Era como é que pesquisavamos os links mas ja pensei nisso poggers
                results = barrels.get(0).searchWord(wordToSearch);
                for (Link link : results){

                    String string ="Title: " + link.title + "\n" + "URL: " + link.getUrl() + "\n" + "Citation: " + link.citation + "\n";
                    coolResults.add(string);
                }

                System.out.println(results.size());



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
