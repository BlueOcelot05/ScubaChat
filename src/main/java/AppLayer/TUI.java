package AppLayer;

import LinkLayer.LinkLayer;
import NetworkLayer.DA.DynamicAddressing;
import NetworkLayer.NetworkLayer;
import TransportLayer.TransportLayer;
import NetworkLayer.NetworkingDAO;

import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TUI implements Runnable {
    // Singleton
    private static final TUI instance = new TUI();

    // Get instance
    public static TUI getSingleton() {
        return instance;
    }

    // Private constructor
    private TUI() {
    }

    private static final Scanner scanner = new Scanner(System.in);

    private static void printMenu() {
        System.out.println("\n--- MENU ---");
        System.out.println("Your current address: " + NetworkingDAO.getInstance().getLocalAddress());
        System.out.println("RANGE: Shows all nodes that are reachable");
        System.out.println("WHISPER: Send message to specific node");
        System.out.println("BROAD: Send message to all nodes in range");
        System.out.println("TOP: To see a short description of the network topology");
        System.out.println("HELP: Print the help menu again");
        System.out.println("QUIT: Quit the program");
    }

    private void printNodes() {
        System.out.print("\nYour address: ");
        System.out.println(NetworkingDAO.getInstance().getLocalAddress());
        System.out.print("These are the nodes you can reach: ");
        for (int node : NetworkingDAO.getInstance().getReachableNodes()) {
            System.out.print(node);
            System.out.print(" ");
        }
        System.out.println();
    }

    public void receiveMessage(String message, int source) {
        // App header incoming from transport layer
        System.out.println("\nMessage from " + source + ": " + message);
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        //Initialize LinkLayer to make connection to Physical Layer.
        LinkLayer.getSingleton();
        DynamicAddressing.getSingleton();
        NetworkLayer.getSingleton();

        System.out.println("Welcome to the chat app interface");
        boolean running = true;
        printMenu();
        while (running) {
            System.out.print("Enter command: ");
            String input = scanner.nextLine().trim().toUpperCase();
            switch (input.split(" ")[0]) {
                case "RANGE":
                    TUI.getSingleton().printNodes();
                    break;
                case "WHISPER": // Send message to specific node
                    if (!NetworkingDAO.getInstance().getReachableNodes().isEmpty()) {
                        int destination = -1;
                        while (destination < 0) {
                            try {
                                if (input.split(" ").length == 1) {

                                    System.out.print("Enter the desired recipient or e to cancel ( ");
                                    for (int node : NetworkingDAO.getInstance().getReachableNodes()) {
                                        System.out.print(node);
                                        System.out.print(" ");
                                    }
                                    System.out.print("): ");

                                    input = scanner.nextLine().trim().toUpperCase();
                                    if (input.equals("E")) {
                                        break;
                                    }

                                    destination = Integer.parseInt(input);

                                }else destination = Integer.parseInt(input.split(" ")[1]);

                                if (!NetworkingDAO.getInstance().getReachableNodes().contains(destination)) {
                                    System.out.println("Node not in range, choose another one please");
                                    destination = -1;
                                    continue;
                                }

                                TUI.getSingleton().sendWhisper(destination);

                            } catch (InputMismatchException | NumberFormatException e) {
                                System.out.println("Invalid input, try again. Type e to exit.");
                            }
                        }
                    } else {
                        System.out.println("Currently there are no reachable nodes, try again later.");
                    }
                    break;
                case "BROAD":
                    // Send message to every node in range
                    if (!NetworkingDAO.getInstance().getReachableNodes().isEmpty()) {
                        TUI.getSingleton().sendBroadCast();
                    } else {
                        System.out.println("No reachable nodes (yet), try again later.");
                    }
                    break;
                case "TOP":
                    printTopology();
                    break;
                case "HELP":
                    printMenu();
                    break;
                case "QUIT":
                    running = false;
                    break;
                default:
                    System.out.println("Unknown command. Please try again");
                    break;
            }
        }
        System.out.println("Exiting TUI. Goodbye!");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException _) {
        }
        System.out.println("Closing application");
        System.exit(0);
    }

    private void printTopology() {
        System.out.print("\nYour address: ");
        System.out.println(NetworkingDAO.getInstance().getLocalAddress());
        Map<Integer, NetworkLayer.RouteEntry> routes = NetworkLayer.getSingleton().getAllRoutes();
        for (int i : NetworkingDAO.getInstance().getKnownNodes()) {
            System.out.println("Node " + i + " with cost " + ((routes.get(i) == null) ? "unknown" : routes.get(i).cost));
        }
        System.out.println();
    }

    private void sendWhisper(int destination) {
        String message = getInputMessage();
        TransportLayer.getSingleton().sendNewMessage(message, destination);
    }

    public void sendBroadCast() {
        String message = getInputMessage();
        TransportLayer.getSingleton().sendNewMessage(message, 7);
    }

    private String getInputMessage() {
        System.out.print("Enter the message you want to send: ");
        String message = "";
        while (message.isEmpty()) {
            try {
                message = scanner.nextLine(); // Get message from user input
            } catch (InputMismatchException e) {
                System.out.println("Invalid input, try again");
            }
            if (message.isEmpty()) {
                System.out.println("Don't enter an empty message:)");
                message = "";
            }
        }
        return message;
    }

    public static void main(String[] args) {
        TUI.getSingleton().run();
    }
}