package NetworkLayer;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import NetworkLayer.NetworkLayer.RouteEntry;


/**
 * <h1>DAO- Database Access Object</h1>
 * This <b>Database Access Object</b> (DAO) contains networking information that is relevant and should be easily
 * accessible at any layer.
 * <p>
 * Will be used to store:
 * <ul>
 *     <li>Current address of this node</li>
 *     <li>A routing table that contains all reachable nodes and their costs and next hops</li>
 *     <li>Helper functions for manipulating and getting this data</li>
 * </ul>
 */
public enum NetworkingDAO {
    INSTANCE(generateInitialAddress());

    private final int DEFAULT_TTL = 60;
    private int localAddress;

    // Map of node -> TTL of nodes that are known
    private final Map<Integer, Integer> knownNodes;


    /**
     * Private constructor that initialise the DAO only once.
     *
     * @param initialAddress the initial address of any.
     */
    NetworkingDAO(int initialAddress) {
        this.localAddress = initialAddress;
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        this.knownNodes = new HashMap<>();
        executor.scheduleAtFixedRate(
                () -> NetworkingDAO.getInstance().decrementNodes(),
                10,
                3,
                TimeUnit.SECONDS
        );


    }

    /**
     * Generates a random 4-bit initial address between 0 and 15.
     */
    private static int generateInitialAddress() {
        int number = 7;
        while (number == 7) {
            number = (int) (Math.random() * 16);
        }
        return number;
    }

    /**
     * @return this object
     */
    public static NetworkingDAO getInstance() {
        return NetworkingDAO.INSTANCE;
    }

    /**
     * This is the address of the node at the current time. <b>Don't save this address as a var on your layer</b>
     * this could change at any point
     *
     * @return the current local address of this node.
     */
    public synchronized int getLocalAddress() {
        return localAddress;
    }

    /**
     * Sets the current address of the node. This is used when renewing the address due to a conflict.
     *
     * @param newAddress the new address to assign to this node.
     */
    public void setLocalAddress(int newAddress) {
        NetworkLayer.getSingleton().updateRoute(newAddress, new RouteEntry(newAddress, 0,true));
        int oldAddress = this.localAddress;
        this.localAddress = newAddress;
        NetworkLayer.getSingleton().removeRoute(oldAddress);

    }

    public synchronized void updateNode(int node) {
        knownNodes.put(node, DEFAULT_TTL);
    }

    public synchronized void decrementNodes() {
        List<Integer> toBeRemoved = new ArrayList<>();
        knownNodes.forEach((node, ttl) -> {
            knownNodes.put(node, ttl - 1);
            if (knownNodes.get(node) <= 0) {
                toBeRemoved.add(node);
            }
        });
        for (Integer node : toBeRemoved) {
            knownNodes.remove(node);
            NetworkLayer.getSingleton().removeRoute(node);
        }
    }

    public synchronized Set<Integer> getKnownNodes() {
        return knownNodes.keySet();
    }

    public synchronized Set<Integer> getReachableNodes() {
        Map<Integer, RouteEntry> routes = NetworkLayer.getSingleton().getAllRoutes();
        return getKnownNodes().stream().filter(node -> routes.get(node) != null).collect(Collectors.toSet());
    }


}