
import java.rmi.*;
import java.util.List;

public interface IGateway extends Remote{
    int subscribeDownloader(IDownloader iDownloaderInterface) throws RemoteException;

    void putLinksInQueue(List<String> links) throws RemoteException;

    String getLastLink() throws RemoteException;
}
