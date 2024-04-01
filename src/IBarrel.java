import java.util.HashMap;
import java.util.HashSet;

public interface IBarrel {
    boolean returnUpToDateState();
    public HashMap<String, HashSet<String>> getWordLinkMap();

    public HashMap<String, HashSet<String>> getLinkLinkMap();
}
