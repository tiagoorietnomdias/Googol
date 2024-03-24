import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Properties;

public class BarrelMulticast {
    public static void receiveMessage() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("./src/resources/System.properties")) {
            properties.load(input);
        }
        String ipAddress = properties.getProperty("ipAddress");
        int port = Integer.parseInt(properties.getProperty("port"));

        try (MulticastSocket socket = new MulticastSocket(port)) {
            InetAddress group = InetAddress.getByName(ipAddress);
            socket.joinGroup(group);

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + received);
            }
        }
    }



    public static void main (String[]args) throws IOException {
            receiveMessage();
        }
    }
