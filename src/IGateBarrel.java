import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;


public interface IGateBarrel extends Remote {

    int subscribeBarrel(IBarrel iBarrelInterface) throws RemoteException;
    public HashMap<String, HashSet<String>> getupdatedWordMap(int barrelIDRequesting) throws RemoteException;
    public HashMap<String, HashSet<String>> getupdatedLinkMap(int barrelIDRequesting) throws  RemoteException;
    public HashSet<Link> getupdatedInfoMap(int barrelID) throws RemoteException;
    public void renewBarrelState(int barrelID, IBarrel barrel)throws RemoteException;
    public void shutdownBarrel(int barrelID, IBarrel barrel)throws RemoteException;
    public void updateNumberofSearches(int barrelID, IBarrel barrel) throws RemoteException;



}

