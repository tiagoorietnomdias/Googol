import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.ArrayList;
import java.util.List;


public class Gateway extends UnicastRemoteObject implements IGateway{


    public ArrayList<IDownloader> downloaders;
    public int queue[];

    public Gateway() throws IOException{
        super();
        queue = new int[10];
        downloaders = new ArrayList<>();
        LocateRegistry.createRegistry(8000); //maybe fazer isto automaticamente dando load ao system properties

        try{
            Naming.rebind("Gateway", (IGateway)this);
        } catch (MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribe(IDownloader d) throws RemoteException{
        downloaders.add(d);
    }

    @Override
    public void putLinksInQueue(List<String> links) {

    }

    @Override
    public void getLastLink() {

    }
}
