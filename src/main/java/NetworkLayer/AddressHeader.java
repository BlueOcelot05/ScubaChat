package NetworkLayer;

import Model.Packet;

public class AddressHeader implements Packet {
    /**
     * Explains what the next header is. See header ID's in design doc
     */
    private final int Next_Protocol;
    /**
     * Time to life in hops.
     */
    private final int TTL;
    /**
     * Length of the payload under this header in bytes.
     */
    private final int Payload_Length;
    /**
     * Address of the source of this packet.
     */
    private final int Source;
    /**
     * Address of the destination of this packet.
     */
    private final int Destination;
    /**
     * Node of the next hop to take for this packet.
     */
    private final int Next_Hop;
    /**
     * ID number (0-4095). Identifies the header for broadcasts.
     */
    private final int ID;
    /**
     * Holds the payload.
     */
    private final Packet upperHeader;

    /**
     * Constructs a new Address_Header.
     *
     * @param next_Protocol  The header ID of the upper header.
     * @param TTL            The time to live for a packet.
     * @param payload_Length Length of the payload in bytes.
     * @param source         The node where this packet came from.
     * @param destination    The node where this packet needs to go to.
     * @param next_Hop       The node where this packet should go to next. (Can be null if broadcast)
     * @param id             The ID of the header for broadcasts.
     * @param upperHeader    The packet from the layer above (payload). Can be null if there's no payload initially.
     */
    public AddressHeader(int next_Protocol, int TTL, int payload_Length,
                         int source, int destination, int next_Hop, int id, Packet upperHeader) {
        this.Next_Protocol = next_Protocol; // Consider validating initial values here too, or ensure setters are called.
        this.TTL = TTL;
        this.Payload_Length = payload_Length;
        this.Source = source;
        this.Destination = destination;
        this.Next_Hop = next_Hop;
        this.ID = id;
        this.upperHeader = upperHeader;
    }

    //Getters and setters

    /**
     * Gets the ID of the next header.
     *
     * @return The next header ID
     */
    public int getNext_Protocol() {
        return Next_Protocol;
    }

    /**
     * Gets the TTL number of this Address header.
     *
     * @return The time to live
     */
    public int getTTL() {
        return TTL;
    }

    /**
     * Gets the Length of the payload in bytes.
     *
     * @return The length of the payload
     */
    public int getPayload_Length() {
        return Payload_Length;
    }

    /**
     * Gets the address of the source.
     *
     * @return the address of the source
     */
    public int getSource() {
        return Source;
    }

    /**
     * Get the next hop.
     *
     * @return the next hop
     */
    public int getNext_Hop() {
        return Next_Hop;
    }

    /**
     * Get the packet ID.
     *
     * @return the packet ID
     */
    public int getID() {
        return ID;
    }

    // --- HeaderType/Packet Interface Implementations ---


    /**
     * Gets the upper layer header or packet (payload) encapsulated by this TCP header.
     *
     * @return The {@link Packet} representing the data from the layer above. Can be the internal placeholder if none was set.
     */
    public Packet getUpperHeader() {
        return upperHeader;
    }

    /**
     * Gets the destination identifier by delegating the call to the upper header.
     * This assumes the upper header/packet contains the ultimate destination information.
     * Behavior is undefined if the upperHeader does not properly implement getDestination()
     * or if upperHeader is the internal placeholder (`Fix_IT`) without a destination.
     *
     * @return The destination identifier obtained from the upper header
     * (result of {@code getUpperHeader().getDestination()}).
     */
    public int getDestination() {
        // Potential NullPointerException if upperHeader could ever become null after construction,
        // or if Fix_IT.getDestination() isn't handled correctly.
        return Destination;
    }
}