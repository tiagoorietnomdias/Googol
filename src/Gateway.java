import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;


public class Gateway extends UnicastRemoteObject implements IGateDownloader, IGateBarrel{


    public ArrayList<IDownloader> downloaders;
    public ArrayList<IBarrel> barrels;
    public int queue[];
    ArrayDeque<String> linkQueue = new ArrayDeque<>();
    public Gateway() throws IOException, AlreadyBoundException {
        super();
        //String serverHostname = "192.168.245.239";
        //System.setProperty("java.rmi.server.hostname", serverHostname);

        queue = new int[10];
        downloaders = new ArrayList<>();
        barrels = new ArrayList<>();
        Registry downloaderRegistry = LocateRegistry.createRegistry(1099);
        Registry barrelRegistry = LocateRegistry.createRegistry(1098);


        barrelRegistry.rebind("GatewayBarrel", this);
        downloaderRegistry.rebind("GatewayDownloader", this);
    }


    @Override
    public int subscribeDownloader(IDownloader d) throws RemoteException{
        downloaders.add(d);
        return downloaders.indexOf(d);
    }
    @Override
    public int subscribeBarrel(IBarrel b) throws RemoteException{
        barrels.add(b);
        return barrels.indexOf(b);
    }

    @Override
    public void putLinksInQueue(List<String> links) throws RemoteException{
        for (String link:links){
            linkQueue.addFirst(link);
        }
    }

    @Override
    public String getLastLink() throws RemoteException{
        return linkQueue.removeLast();
    }

    public static void main(String args[]) {
        try {
            Gateway gateway = new Gateway();
        } catch (IOException | AlreadyBoundException e) {
            throw new RuntimeException(e);
        }

    }
}
