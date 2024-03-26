import java.io.IOException;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.ArrayList;
import java.util.Scanner;


public class Gateway extends UnicastRemoteObject implements IGateway{

    public int queue[];
    public Gateway() throws IOException{
        super();
        queue = new int[10];
        LocateRegistry.createRegistry(8000); //maybe fazer isto automaticamente dando load ao system properties

        try{
            Naming.rebind("Gateway", (IGateway)this);
        } catch (MalformedURLException e){
            throw new RuntimeException(e);
        }
    }
}
