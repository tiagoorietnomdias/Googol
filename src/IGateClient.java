import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface IGateClient extends Remote {
    public ArrayList<String> pesquisa(String wordToSearch) throws RemoteException;
    public int subscribeClient(ICliente c) throws RemoteException;

    ArrayList<Integer> getActiveBarrels() throws RemoteException;

    List<String> getTop10Searches() throws RemoteException;

    void insertInQueue(String link) throws RemoteException;
}
