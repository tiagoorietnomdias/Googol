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
import java.util.Scanner;

public class Cliente extends UnicastRemoteObject implements ICliente {

    private static IGateClient gateway;
    private static int clientID;


    public Cliente() throws RemoteException {
        super();
        while(true) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1100);
                gateway = (IGateClient) registry.lookup("GatewayClient");
            } catch (NotBoundException ne) {
                throw new RuntimeException(ne);
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
            }

            break;
        }
        clientID = gateway.subscribeClient(this);
    }



    public static void main(String[] args) {
        try {
            Cliente c = new Cliente();

            Scanner scanner = new Scanner(System.in);

            System.out.println("Enter text. Type 'exit' to quit.");

            while (true) {
                System.out.println("1-Pesquisa\n2-Admin Page\n3-Exit\n4-Inserir URL na queue");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("3")) {
                    System.out.println("Exiting...");
                    break;
                } else if (input.equalsIgnoreCase("1")) {
                    System.out.print("Escreva termo a pesquisar:");
                    String pesquisa = scanner.nextLine();
                    ArrayList<String> results = gateway.pesquisa(pesquisa);
                    if (results.size() == 0) System.out.println("No results found for: " + pesquisa);
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
                    /*Esta informação será atualizada apenas quando houver alterações. Pretende-se saber o estado do sistema, designadamente as 10 pesquisas mais comuns, a lista de Barrels ativos, e o
                     */
                    //10 pesquisas mais comuns

                    //tempo médio de resposta a pesquisas medido pela Gateway descriminado por Barrel (em décimas de segundo).


                }else if (input.equalsIgnoreCase("4")) {
                    //meter linkQUeue
                    System.out.print("Link a inserir: ");
                    String erm=scanner.nextLine();
                    gateway.insertInQueue(erm);

                }


            }
            scanner.close();
        } catch (RuntimeException | RemoteException ex) {
            ex.printStackTrace();
        }
    }

    private static int showNextTen(int currentIndex, ArrayList<String> results) {
        int i;
        for (i = currentIndex; i < currentIndex + 10; i++) {
            if (i >= results.size()) break;
            System.out.println(results.get(i));
        }
        return i;
    }

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
