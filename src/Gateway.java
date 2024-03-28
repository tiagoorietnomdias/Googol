import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;


public class Gateway extends UnicastRemoteObject implements IGateway{


    public ArrayList<IDownloader> downloaders;
    public int queue[];
    ArrayDeque<String> linkQueue = new ArrayDeque<>();
    public Gateway() throws IOException{
        super();
        queue = new int[10];
        downloaders = new ArrayList<>();
        LocateRegistry.createRegistry(1099); //maybe fazer isto automaticamente dando load ao system properties

        try{
            Naming.rebind("Gateway", (IGateway)this);
        } catch (MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public int subscribeDownloader(IDownloader d) throws RemoteException{
        downloaders.add(d);
        return downloaders.indexOf(d);
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
