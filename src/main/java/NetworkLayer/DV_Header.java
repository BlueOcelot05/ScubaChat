package NetworkLayer;


import Model.Packet;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Distance Vector (DV) header used in the network protocol.
 * This header contains routing information used by the Distance Vector routing algorithm,
 * storing mappings between destination addresses and their associated costs.
 * DV headers are typically broadcast to neighboring nodes to share routing information.
 */
public class DV_Header implements Packet {

    private final Map<Integer, Integer> DV = new HashMap<>();

    /**
     * Constructs a new DV_Header with multiple destination-cost pairs.
     * Creates a mapping between destination addresses and their corresponding costs,
     * representing a distance vector table.
     *
     * @param destinations Array of destination addresses
     * @param costs Array of costs corresponding to each destination
     * @throws IndexOutOfBoundsException If the lengths of destinations and costs arrays don't match
     */
    public DV_Header(int[] destinations, int[] costs) {
        for (int i = 0; i < destinations.length; i++) {
            DV.put(destinations[i], costs[i]);
        }
    }

    /**
     * Constructs a new DV_Header with a single destination-cost pair.
     * Creates a distance vector with only one entry, useful for simple updates.
     *
     * @param addr The destination address
     * @param cost The cost to reach the specified destination
     */
    public DV_Header(int addr, int cost) {
        DV.put(addr, cost);
    }


    /**
     * Gets the upper layer header associated with this header.
     * For DV_Header, there is no upper header.
     *
     * @return null as DV_Header doesn't have an upper header
     */
    public Packet getUpperHeader() {
        return null;
    }

    /**
     * Recursively calls the same function from its upper header until
     * getting the address specified by the user.
     *
     * @return the address of the destination
     */
    public int getDestination() {
        return 7;
    }

    /**
     * Gets the distance vector mapping table.
     * This map contains destination addresses as keys and their corresponding costs as values.
     *
     * @return A Map representing the current distance vector with destination-cost pairs
     */
    public Map<Integer, Integer> getDistanceVector() {
        return DV;
    }
}

