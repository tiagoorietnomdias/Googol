import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public interface IBarrel {
    boolean returnUpToDateState();
    HashMap<String, HashSet<String>> getWordLinkMap();

    HashMap<String, HashSet<String>> getLinkLinkMap();

    HashSet<Link> getLinkInfoMap() throws RemoteException;
    ArrayList<Link> searchWord(String wordstoSearch) throws RemoteException;
    ArrayList<Link> searchLink(String linkToSearch);
    HashMap<String, Integer> getNumberOfSearches() throws RemoteException;
    IBarrel renewBarrel()throws RemoteException;

    void loadFromTxt(String filename);
    boolean getBarrelStatus();

    void shutdown();


}
