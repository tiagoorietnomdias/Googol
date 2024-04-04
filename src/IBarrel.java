import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public interface IBarrel {
    boolean returnUpToDateState();
    public HashMap<String, HashSet<String>> getWordLinkMap();

    public HashMap<String, HashSet<String>> getLinkLinkMap();

    public HashSet<Link> getLinkInfoMap() throws RemoteException;

    public ArrayList<Link> searchWord(String wordstoSearch);
    public ArrayList<Link> searchLink(String linkToSearch);
}
