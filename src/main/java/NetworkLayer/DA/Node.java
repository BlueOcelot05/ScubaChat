package NetworkLayer.DA;

/**
 * Represents a node in the ad-hoc network with an assigned IP address and a time-to-live (TTL).
 * The TTL is used to measure how long this node is still considered "alive."
 * Once TTL expires, the node may be removed.
 */
public class Node {
    private int ipAddress;
    private int TTL;

    /**
     * Constructs a new Node with IP address and TTL.
     * @param ipAddress the IP (0–15).
     * @param TTL       forced at 60.
     */
    public Node(int ipAddress, int TTL) {
        this.ipAddress = ipAddress;
        this.TTL = 60;
    }

    /**
     * Returns the node's IP address.
     * @return the IP address of this node.
     */
    public int getIpAddress() {
        return ipAddress;
    }

    /**
     * Gets TTL.
     * @return the remaining TTL.
     */
    public int getTTL() {
        return TTL;
    }

    /**
     * Sets the IP address of the node.
     * @param ipAddress the new IP address to assign to this node.
     */
    public void setIpAddress(int ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Sets the TTL .
     * If set to zero or negative, the node is considered expired or unreachable.
     * @param TTL the new value.
     */
    public void setTTL(int TTL) {
        this.TTL = TTL;
    }

}
