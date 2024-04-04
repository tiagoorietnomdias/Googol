import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface IGateClient extends Remote {
    public ArrayList<String> pesquisa(String wordToSearch) throws RemoteException;
    public int subscribeClient(ICliente c) throws RemoteException;
}
