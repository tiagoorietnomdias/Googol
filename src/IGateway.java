
import java.rmi.*;
import java.util.List;

public interface IGateway extends Remote{
    void subscribe(IDownloader iDownloaderInterface) throws RemoteException;

    void putLinksInQueue(List<String> links);

    void getLastLink();
}
