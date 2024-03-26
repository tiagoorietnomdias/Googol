import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

/*
public class BarrelMulticas extends MulticastSocket{
//isto precisa de dar catch a IOException.
 */
public class BarrelMulticast {

/*
//Esta foi a maneira que eu vi que funcionava (não dava erro de compilação I mean)
public BarrelMulticast() throws IOException{
    super();
}
//Penso que é preciso alterar se o BarrelMulticast receber argumentos, eu não sei se recebe
 */
    private static final String PROPERTIES_FILE_PATH = "./src/resources/System.properties";
    private static final int BUFFER_SIZE = 1024;

    private HashMap<String, HashSet<String>> wordLinkMap = new HashMap<>();

    public HashMap<String, HashSet<String>> updateWordHashMap(String word, String link) {
        wordLinkMap.computeIfAbsent(word, k -> new HashSet<>()).add(link);
        return wordLinkMap;
    }

    public void receiveMessage() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(PROPERTIES_FILE_PATH)) {
            properties.load(input);
        }

        String ipAddress = properties.getProperty("ipAddress");
        int port = Integer.parseInt(properties.getProperty("port"));

        /*
        //Este try do socket não seria preciso
         */
        try (MulticastSocket socket = new MulticastSocket(port)) {
            InetAddress group = InetAddress.getByName(ipAddress);
            /*
            this.joinGroup(group);
             */
            socket.joinGroup(group);

            while (true) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                /*
                this.receive(packet);
                 */
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());

                /*
                //eu não sei se esta condição está bem
                if(packet.getLength() <= 0){
                    //pensei que, se a mensagem que o downloader recebe de volta for igual à que ele enviou, então é porque deu merda?
                    packet.setData(buffer);
                    socket.send(packet);
                }
                */
                System.out.println("Received: " + received);

                processMessage(received);
            }
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split("\\|");

        if (parts.length < 3 || !parts[0].equals("downloader")) {
            return;
        }

        String type = parts[2];
        if (type.equals("links")) {
            System.out.println("Processing links...");

        } else if (type.equals("words")) {
            System.out.println("Processing words...");
            // Extract word and link from the message and update the database
            //String word = parts[3];
            //String link = parts[4];
            //updateWordHashMap(word, link);
            // Perform any other necessary actions related to word processing
        }
    }

    public static void main(String[] args) throws IOException {
        BarrelMulticast barrel = new BarrelMulticast();
        barrel.receiveMessage();
    }
}
