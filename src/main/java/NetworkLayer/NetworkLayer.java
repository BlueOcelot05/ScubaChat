package NetworkLayer;

import Model.Exceptions.NetworkException;
import Model.Exceptions.RoutingException;
import Model.LAYER;
import Model.Packet;
import Model.LayerModel;
//import NetworkLayer.DA.DynamicAddressing;
import NetworkLayer.DA.DynamicAddressing;
import TransportLayer.TCPHeader;
import TransportLayer.TransportLayer;
import LinkLayer.LinkLayer;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton implementation of the Network Layer in the network protocol stack.
 * Handles routing, forwarding, and dynamic address management for network packets.
 * Implements distance vector routing algorithm with periodic updates and route maintenance.
 */
public enum NetworkLayer implements LayerModel {
    INSTANCE;
    //Singleton

    /**
     * Represents an entry in the routing table.
     * Contains information about next hop, cost, time-to-live, and whether this is a self-route.
     * Self-routes represent routes to the local node and are handled differently than other routes.
     */
    public static class RouteEntry{
        public final int nextHop;
        public int cost;
        private long TTL;
        private boolean selfRoute;

        /**
         * Constructs a new routing table entry with default settings (not a self route).
         *
         * @param nextHop The next hop address to reach the destination
         * @param cost The cost (metric) to reach the destination
         */
        public RouteEntry(int nextHop, int cost) {
            this.nextHop = nextHop;
            this.cost = cost;
            this.TTL = ROUTE_TTL;
            selfRoute = false;
        }

        /**
         * Constructs a new routing table entry with specified self-route status.
         *
         * @param nextHop The next hop address to reach the destination
         * @param cost The cost (metric) to reach the destination
         * @param selfRoute Whether this entry represents a route to the local node
         */
        public RouteEntry(int nextHop, int cost, boolean selfRoute) {
            this.nextHop = nextHop;
            this.cost = cost;
            this.TTL = ROUTE_TTL;
            this.selfRoute = selfRoute;
        }

        /**
         * Decrements the TTL of this route entry and increases the cost as the route ages.
         * Self-routes are not affected by TTL decrements.
         *
         * @return true if the TTL has reached zero (route has expired), false otherwise
         */
        public synchronized boolean decrementTTL(){
            if(!selfRoute) {
                if (TTL == ((int) ROUTE_TTL / 2)) cost++;
                if (TTL == ((int) ROUTE_TTL / 3)) cost++;
                if (TTL == ((int) ROUTE_TTL / 4)) cost++;
                if (TTL == ((int) ROUTE_TTL / 5)) cost++;
                if (TTL == ((int) ROUTE_TTL / 6)) cost++;

                return --TTL == 0;
            }else return false;
        }

        /**
         * Refreshes the TTL of this route entry back to the maximum value.
         * Self-routes are not affected by TTL refreshes.
         */
        public synchronized void refreshTTL(){
            if(!selfRoute) TTL = ROUTE_TTL;
        }

    }

    /**
     * Time-to-live for route entries, calculated as 4 times the DV update interval.
     * After this period, a route will be removed if not refreshed.
     */
    private static final int ROUTE_TTL = INSTANCE.DV_UPDATE_INTERVAL*4;

    /**
     * Gets the singleton instance of the NetworkLayer.
     *
     * @return The singleton NetworkLayer instance
     */
    public static NetworkLayer getSingleton() {
        return INSTANCE;
    }

    /**
     * The last used ID for broadcast packets.
     * Incremented for each new broadcast packet to ensure uniqueness.
     */
    private int lastUsedId = 1;

    /**
     * Interval in seconds between Distance Vector routing updates.
     */

    private final Integer DV_UPDATE_INTERVAL = 60;


    /**
     * Cost threshold beyond which a node is considered unreachable.
     * Routes with costs greater than or equal to this value will be removed.
     */
    private static final int INFINITY = 8;

    /**
     * Routing table that maps destination addresses to RouteEntry objects.
     * Contains all known routes to other nodes in the network.
     */
    public final Map<Integer, RouteEntry> neighbourTable = new HashMap<>();

    /**
     * Lock for thread-safe access to the routing table.
     */
    private final Lock neighbourLock = new ReentrantLock();

    /**
     * Constructor for the NetworkLayer singleton.
     * Initializes the routing table with a self-route and sets up scheduled tasks
     * for DV updates and route maintenance.
     */
    NetworkLayer() {
        // private constructor to enforce singleton pattern

        // Initialize empty routing table
        int addr = NetworkingDAO.getInstance().getLocalAddress();
        neighbourLock.lock();
        neighbourTable.put(addr, new RouteEntry(addr, 0,true));
        neighbourLock.unlock();

        // Send DV updates
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        executor.scheduleAtFixedRate(
                this::sendUpdate,
                1,
                DV_UPDATE_INTERVAL,
                TimeUnit.SECONDS
        );
        executor.scheduleAtFixedRate( () -> {
                Set<Integer> removed = new HashSet<>();
                neighbourLock.lock();
                for (Map.Entry<Integer, RouteEntry> entry : neighbourTable.entrySet())
                    if (entry.getValue().decrementTTL()) removed.add(entry.getKey());
                for (Integer id : removed)
                    removeRoute(id);
                neighbourLock.unlock();
                },
                100, //TODO increase number after testing
                1,
                TimeUnit.SECONDS
        );
    }





    /**
     * This function receives the packet object and based on what kind of packet is applies logic
     * to that packet to send or receive it correctly.
     * Currently, accepts TCP, DV and ADDRESS packets.
     * TCP packets will get an address header and be sent to the LINK layer
     * ADDRESS packets will be checked and given to the correct layer depending on what the next protocol is.
     *
     * @param packet The packet given to the network layer.
     * @throws NetworkException Given packet is from a class that this method doesn't understand.
     */
    @Override
    public void ReceiveHeader(Packet packet) throws NetworkException {
        switch (packet) {
            case TCPHeader tcpHeader -> {
                //Received header is TCP. It's data coming from us to the outside.
                //Create an Address header and send it to the LinkLayer.

                int next_Protocol = 0;  // ID for TCP is 0
                int TTL = 10; //Time to live for every new packet is 4.
                int payload_Length = 4 + tcpHeader.getPayloadLength();
                int source = NetworkingDAO.getInstance().getLocalAddress();
                int destination = packet.getDestination();
                int next_hop;
                try {
                    next_hop = getRoute(destination).nextHop;
                } catch (NullPointerException e) {
                    return;
                }
                int id = 0; //Header is a whisper TCP. So no ID
                Packet address_Packet = new AddressHeader(next_Protocol, TTL, payload_Length,
                        source, destination, next_hop, id,
                        packet);
                LinkLayer.getSingleton().ReceiveHeader(address_Packet);

            }
            case AddressHeader addressHeader -> {
                //Received header is an Address header. Packet came from outside.
                //Unpack and see what header is next
                switch (addressHeader.getNext_Protocol()) {
                    case 0 -> {
                        //Next header is TCP. Check if we need to resend and bring down to TCP_layer
                        if (addressHeader.getDestination() == NetworkingDAO.getInstance()
                                .getLocalAddress()) {
                            //This packet is for us. Bring it down to TCP
                            TransportLayer.getSingleton()
                                    .receiveTcpHeader((TCPHeader) addressHeader.getUpperHeader(),
                                            addressHeader.getSource());
                        }  else {
                            //This packet is not for us. We may need to send it forward.

                            int source = addressHeader.getSource();
                            int destination = addressHeader.getDestination();


                            if (addressHeader.getTTL() > 0 &&
                                    addressHeader.getNext_Hop() == NetworkingDAO.getInstance().getLocalAddress()) {
                                int next_Protocol = 0;
                                int TTL = addressHeader.getTTL() - 1;
                                int payload_Length = addressHeader.getPayload_Length();
                                int next_hop = 0;
                                try {
                                    next_hop = getRoute(destination).nextHop;
                                } catch (RoutingException _) {
                                    return;
                                }
                                int id = 0; //Packet is a whisper. So ID = 0.
                                Packet address_Packet = new AddressHeader(next_Protocol, TTL,
                                        payload_Length, source,
                                        destination, next_hop,
                                        id,
                                        addressHeader.getUpperHeader());
                                LinkLayer.getSingleton().ReceiveHeader(address_Packet);
                            }
                        }
                    }
                    case 2 -> {
                        // Header is a DV header. Update the neighbour table. And resend if table updated.
                        DV_Header dv = (DV_Header) addressHeader.getUpperHeader();
                        int neighborId = addressHeader.getSource();

                        // Make a *copy* of the current table to compare later
                        Map<Integer, RouteEntry> oldTable = new HashMap<>(getAllRoutes());
                        Map<Integer, Integer> receivedDV = dv.getDistanceVector();
                        handleDVUpdate(neighborId, receivedDV);

                        // Compare the old and new table (by content, not reference)
                        if (!oldTable.equals(getAllRoutes())) {
                            // Table was updated — construct a DV packet and broadcast

                            LinkLayer.getSingleton().ReceiveHeader(makeUpdatePacket());

                        }
                    }
                    default -> throw new IllegalStateException(
                            "Unexpected Header ID: " + addressHeader.getNext_Protocol());
                }
            }
            case DA_Header daHeader -> {
                if (daHeader.isConflict()) {
                    removeRoute(daHeader.getAddress());
                }
                DynamicAddressing.getSingleton().ReceiveHeader(daHeader);
            }
            case DV_Header dv_header -> {
                //The header is a Dynamic Vector header. Send it forward to the LinkLayer.
                int source = NetworkingDAO.getInstance().getLocalAddress();
                int nextProtocol = 2; // DV Header ID
                int TTL = 1; // Only actual neighbours need to know
                int payloadLength = dv_header.getDistanceVector().size();
                int destination = 7; // broadcast
                int nextHop = 0; // for broadcast, not used
                lastUsedId++;
                int id = lastUsedId; // new unique ID for broadcast
                Packet broadcastPacket = new AddressHeader(nextProtocol, TTL, payloadLength,
                        source, destination, nextHop, id,
                        dv_header);

                LinkLayer.getSingleton().ReceiveHeader(broadcastPacket);
            }
            case null, default -> throw new IllegalStateException("Unexpected packet Class: " + packet);
        }
    }


    /**
     * Handles a Distance Vector update received from a neighbor.
     * Updates the routing table based on the received information,
     * implementing the Distance Vector routing algorithm.
     *
     * @param neighborId The address of the neighbor that sent the update
     * @param receivedDV The distance vector received from the neighbor
     */
    public void handleDVUpdate(int neighborId, Map<Integer, Integer> receivedDV) {
        // Refresh old entries
        receivedDV.forEach((key, value) -> {
            neighbourLock.lock();
            neighbourTable.entrySet().stream().filter( // If it's the same entry refresh it
                    e -> Objects.equals(e.getKey(), key) &&
                            value + 1 == e.getValue().cost &&
                            neighborId == e.getValue().nextHop
            ).forEach(
                    entry1 -> neighbourTable.get(entry1.getKey()).refreshTTL());
            neighbourLock.unlock();
        });

        // Loop over all advertised destinations from the neighbor
        for (Map.Entry<Integer, Integer> entry : receivedDV.entrySet()) {
            int destination = entry.getKey();
            int advertisedCost = entry.getValue();

            int totalCostViaNeighbor = 1 + advertisedCost;

            //Ignore all updates about ourself.
            if (destination == NetworkingDAO.getInstance().getLocalAddress()) {
                continue;
            }



            try {
                // Get our current route (if any)
                RouteEntry currentRoute = getRoute(destination);
                boolean routeViaThisNeighbor = currentRoute.nextHop == neighborId;

                if (routeViaThisNeighbor && currentRoute.cost != totalCostViaNeighbor) {
                    // We're using this neighbor, but cost changed → update
                    updateRoute(destination, neighborId, totalCostViaNeighbor);
                } else if (!routeViaThisNeighbor && totalCostViaNeighbor < currentRoute.cost) {
                    // Found a cheaper path through this neighbor → update
                    updateRoute(destination, neighborId, totalCostViaNeighbor);
                }
                // Else: we ignore the update (we already have a better or same route)

            } catch (RoutingException e) {
                updateRoute(destination, neighborId, totalCostViaNeighbor);
            }

            try {
                if (getRoute(destination).cost >= INFINITY) {
                    removeRoute(destination);
                }
            } catch (RoutingException _) {}
        }


    }



    /**
     * Updates or creates a route entry in the routing table.
     *
     * @param destination The destination address
     * @param nextHop The next hop address to reach the destination
     * @param cost The cost to reach the destination
     */
    public synchronized void updateRoute(int destination, int nextHop, int cost) {
        if (destination != NetworkingDAO.getInstance().getLocalAddress()) {
            neighbourLock.lock();
            neighbourTable.put(destination, new RouteEntry(nextHop, cost));
            neighbourLock.unlock();
        }
    }

    /**
     * Updates a route in the routing table with a complete RouteEntry object.
     *
     * @param newAddress The destination address to update
     * @param routeEntry The RouteEntry containing next hop and cost information
     */
    public void updateRoute(int newAddress, RouteEntry routeEntry) {
        neighbourLock.lock();
        neighbourTable.put(newAddress, routeEntry);
        neighbourLock.unlock();
    }

    /**
     * Gets the route entry for a specified destination.
     *
     * @param destination The destination address to look up
     * @return The RouteEntry for the specified destination
     * @throws RoutingException If no route to the destination exists
     */
    public synchronized RouteEntry getRoute(int destination) throws RoutingException {
        if (neighbourTable.get(destination) == null)
            throw new RoutingException(LAYER.NETWORK,null,"Route to %d could not be found".formatted(destination));
        return neighbourTable.get(destination);
    }

    /**
     * Sends a Distance Vector update to all neighbors.
     * This method is called periodically to maintain routing information.
     */
    private void sendUpdate() {

        Packet broadcastPacket = makeUpdatePacket();
        try {
            LinkLayer.getSingleton().ReceiveHeader(broadcastPacket);
        } catch (NetworkException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Creates a packet containing this node's current distance vector information.
     *
     * @return An AddressHeader containing a DV_Header with current routing information
     */
    private synchronized AddressHeader makeUpdatePacket() {
        Map<Integer, RouteEntry> newTable = getAllRoutes();
        int size = newTable.size();
        int[] destinations = new int[size];
        int[] costs = new int[size];
        int i = 0;

        for (Map.Entry<Integer, RouteEntry> entry : newTable.entrySet()) {
            destinations[i] = entry.getKey();
            costs[i] = entry.getValue().cost;
            i++;
        }

        DV_Header newDVHeader = new DV_Header(destinations, costs);

        int source = NetworkingDAO.getInstance().getLocalAddress();
        int nextProtocol = 2; // DV Header ID
        int TTL = 1; // Only actual neighbours need to know
        int destination = 7; // broadcast
        int nextHop = 0; // for broadcast, not used
        lastUsedId++;
        int id = lastUsedId; // new unique ID for broadcast
        return new AddressHeader(nextProtocol, TTL,
                size, source,
                destination, nextHop, id,
                newDVHeader);
    }

    /**
     * Removes a route from the routing table and broadcasts an update.
     *
     * @param destination The destination address to remove
     */
    public synchronized void removeRoute(int destination) {
        // Ensure we don't remove the route to the node itself
        if (destination == NetworkingDAO.getInstance().getLocalAddress()) {
            return; // Do nothing if the destination is the node's own address
        }
        neighbourLock.lock();
        neighbourTable.remove(destination);
        neighbourLock.unlock();
        sendUpdate();
    }

    /**
     * Gets a copy of the current routing table.
     *
     * @return A Map containing all current routes
     */
    public synchronized Map<Integer, RouteEntry> getAllRoutes() {
        return neighbourTable;
    }





}