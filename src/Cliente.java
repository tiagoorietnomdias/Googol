/**
 * The `Cliente` class in Java represents a client that interacts with a remote server using RMI for
 * tasks such as searching, managing active barrels, and inserting URLs into a queue.
 */
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class Cliente extends UnicastRemoteObject implements ICliente {

    private static IGateClient gateway;
    private static int clientID;


    public Cliente() throws RemoteException {
        super();
        while(true) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1100);
                gateway = (IGateClient) registry.lookup(getGatewayClientFromProperties());
            } catch (NotBoundException ne) {
                System.out.println("Property file not properly setup.");
                System.exit(0);
            }catch (RemoteException e){
                try{
                    Scanner scanner = new Scanner(System.in);

                    System.out.println("Connection refused: Server unavailable. Do you wish to retry? [Y/N]");
                    String input = scanner.nextLine();
                    if(input.equalsIgnoreCase("y")){
                        Thread.sleep(3000);
                        System.out.println("Resuming Connection...");
                        continue;
                    }
                    else{
                        System.out.println("See you next time!");
                        System.exit(1);
                    }

                } catch (InterruptedException ie){
                    System.out.println("Downloader stopped while waiting");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            break;
        }
        clientID = gateway.subscribeClient(this);
    }



    /**
     * The function `loadProperties` loads properties from a file specified by the filename parameter.
     *
     * @param filename The `filename` parameter in the `loadProperties` method is a `String` that
     *                 represents the name of the file from which properties need to be loaded.
     * @return The method `loadProperties` is returning a `Properties` object.
     */
    private static Properties loadProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(filename)) {
            properties.load(input);
        } catch (IOException e) {
            System.out.println("Couldn't load \"" + filename + "\". Make sure the file is available");
        }
        return properties;
    }
    private static String getGatewayClientFromProperties() throws IOException {
        return loadProperties("./src/resources/System.properties").getProperty("clientRegistry");
    }
    /**
     * The main function in Java takes user input to perform various actions such as searching, accessing
     * an admin page, inserting URLs into a queue, and displaying results.
     */
    public static void main(String[] args) {
        try {
            Cliente c = new Cliente();

            Scanner scanner = new Scanner(System.in);

            System.out.println("Enter text. Type 'exit' to quit.");

            // The code snippet provided is a part of a Java program that handles user input and
            // provides options for the client to interact with the server. Here's a breakdown of what
            // it does:
            while (true) {
                System.out.println("1-Pesquisa\n2-Admin Page\n3-Exit\n4-Inserir URL na queue");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("3")) {
                    System.out.println("Exiting...");
                    System.exit(0);
                    scanner.close();
                    System.exit(0);
                } else if (input.equalsIgnoreCase("1")) {
                    System.out.print("Escreva termo a pesquisar:");
                    String pesquisa = scanner.nextLine().toLowerCase();
                    ArrayList<String> results = gateway.pesquisa(pesquisa);
                    if(results==null) System.out.println("No servers currently available! please try again later");
                    else if (results.size() == 0) System.out.println("No results found for: " + pesquisa);
                    else {
                        int currentIndex = 0;
                        currentIndex = showNextTen(currentIndex, results);
                        while (currentIndex < results.size()) {
                            String optionChosen;
                            do {
                                System.out.println("1-Ver resultados a seguir\n2-Ver resultados anteriores\n3-Sair da pesquisa");
                                optionChosen = scanner.nextLine();
                            } while (!optionChosen.equals("1") && !optionChosen.equals("2") && !optionChosen.equals("3"));

                            if (optionChosen.equals("1")) {//mostrar mais 10
                                //System.out.println("showing next ten!");
                                currentIndex = showNextTen(currentIndex, results);
                                if (currentIndex >= results.size()) System.out.println("No more results.");
                            } else if (optionChosen.equals("2")) {//mostrar 10 anteriores
                                if (currentIndex >= 10) {
                                    currentIndex = showPreviousTen(currentIndex, results);

                                    System.out.println("showing previous ten");
                                }
                            } else { //Sair da pesquisa
                                System.out.println("exiting...\n");
                                break;
                            }


                        }
                    }
                } else if (input.equalsIgnoreCase("2")) {
                    ArrayList<Integer> activeBarrels = gateway.getActiveBarrels();
                    for (int i : activeBarrels) {
                        System.out.println("Active barrel with ID: " + i+"\n");
                    }
                    List<String> erm=gateway.getTop10Searches();
                    for (String search:erm) System.out.println(search);
                    System.out.println("\n");
                    /*Esta informação será atualizada apenas quando houver alterações. Pretende-se saber o estado do sistema, designadamente as 10 pesquisas mais comuns, a lista de Barrels ativos, e o
                     */
                    //10 pesquisas mais comuns

                    //tempo médio de resposta a pesquisas medido pela Gateway descriminado por Barrel (em décimas de segundo).


                }else if (input.equalsIgnoreCase("4")) {
                    //meter linkQUeue
                    System.out.print("Link a inserir: ");
                    String erm=scanner.nextLine();
                    String resultado=gateway.insertInQueue(erm);
                    System.out.println(resultado);
                }


            }
        } catch (RuntimeException | RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * The function `showNextTen` prints the next ten elements from an ArrayList starting from a given
     * index and returns the index of the last element printed.
     *
     * @param currentIndex The `currentIndex` parameter in the `showNextTen` method represents the
     * starting index from which you want to display the next ten elements in the `results` ArrayList.
     * The method will iterate over the elements starting from this index and display up to the next
     * ten elements or until the end of the list
     * @param results The `results` parameter is an `ArrayList` of `String` objects that contains the
     * data to be displayed. The `showNextTen` method takes the `currentIndex` as a starting point and
     * prints the next ten elements from the `results` list, if available. If there are less than
     * @return The method `showNextTen` is returning the index `i` which represents the next index
     * after the last one that was processed in the loop.
     */
    private static int showNextTen(int currentIndex, ArrayList<String> results) {
        int i;
        for (i = currentIndex; i < currentIndex + 10; i++) {
            if (i >= results.size()) break;
            System.out.println(results.get(i));
        }
        return i;
    }

    /**
     * The function `showPreviousTen` prints the previous ten elements from a given index in an
     * ArrayList.
     *
     * @param currentIndex The `currentIndex` parameter is the current index in the list from which you
     * want to display the previous ten elements. The method `showPreviousTen` aims to display the ten
     * elements preceding the current index in the `results` ArrayList.
     * @param results The `showPreviousTen` method you provided takes in two parameters: `currentIndex`
     * and `results`. The `currentIndex` parameter is used to determine the starting point for
     * displaying the previous ten elements from the `results` ArrayList.
     * @return The method `showPreviousTen` is returning the `currentIndex` value, which is an integer.
     */
    private static int showPreviousTen(int currentIndex, ArrayList<String> results) {
        int i;
        for (i = currentIndex - 20; i < currentIndex; i++) {
            if (i < 0) {
                break;
            }
            System.out.println(results.get(i));
        }
        return currentIndex;
    }

    //funcao para pesquisar por palavra
    //gateway agrupa resultados por ranking

}