package NetworkLayer;

import Model.Exceptions.PayloadException;
import Model.LAYER;
import Model.Packet;

/**
 * Represents a Dynamic Addressing (DA) header used in the network protocol.
 * This header is used for address allocation, conflict resolution, and network configuration.
 * The DA header is sent as a short packet (16 bits) containing protocol information,
 * conflict flag, sequence number, and address information.
 */
public class DA_Header implements Packet {

    private int protocol;
    private boolean conflict;
    private int sequence;
    private int address;


    /**
     * Constructs a new DA_Header with the specified parameters.
     *
     * @param protocol The protocol identifier (3 bits, range 0-7)
     * @param conflict Flag indicating if there is an address conflict (true) or not (false)
     * @param sequence The sequence number for packet ordering (8 bits, range 0-255)
     * @param address The network address (4 bits, range 0-15)
     */
    public DA_Header(int protocol, boolean conflict, int sequence, int address) {
        this.protocol = protocol;
        this.conflict = conflict;
        this.sequence = sequence;
        this.address = address;
    }

    /**
     * Gets the protocol identifier.
     *
     * @return The protocol identifier (range 0-7)
     */
    public int getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol identifier.
     *
     * @param protocol The protocol identifier to set (must be in range 0-7)
     * @throws PayloadException If the protocol value is outside the valid range
     */
    public void setProtocol(int protocol) throws PayloadException {
        if (protocol < 0 || protocol > 7) {
            throw new PayloadException(LAYER.NETWORK, this, "DA Header, Protocol out of range");
        }
        this.protocol = protocol;
    }

    /**
     * Checks if this header indicates an address conflict.
     *
     * @return true if there is an address conflict, false otherwise
     */
    public boolean isConflict() {
        return conflict;
    }

    /**
     * Sets the conflict flag.
     *
     * @param conflict true to indicate an address conflict, false otherwise
     */
    public void setConflict(boolean conflict) {
        this.conflict = conflict;
    }

    /**
     * Gets the sequence number.
     *
     * @return The sequence number (range 0-255)
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Sets the sequence number.
     *
     * @param sequence The sequence number to set (must be in range 0-255)
     * @throws PayloadException If the sequence value is outside the valid range
     */
    public void setSequence(int sequence) throws PayloadException {
        if (sequence < 0 || sequence > 255) {
            throw new PayloadException(LAYER.NETWORK, this, "DA Header, Sequence out of range");
        }
        this.sequence = sequence;
    }

    /**
     * Gets the network address.
     *
     * @return The network address (range 0-15)
     */
    public int getAddress() {
        return address;
    }

    /**
     * Sets the network address.
     *
     * @param address The address to set (must be in range 0-15)
     * @throws PayloadException If the address value is outside the valid range
     */
    public void setAddress(int address) throws PayloadException {
        if (address < 0 || address > 15) { // Using literal number 15
            throw new PayloadException(LAYER.NETWORK, this, "DA Header, Address is out of range");
        }
        this.address = address;
    }

    /**
     * Gets the upper layer header associated with this header.
     * For DA_Header, there is no upper header.
     *
     * @return null as DA_Header doesn't have an upper header
     */
    @Override
    public Packet getUpperHeader() {
        return null;
    }

    /**
     * Gets the destination address for this packet.
     * For DA_Header, this is always the broadcast address (7).
     *
     * @return The broadcast address (7)
     */
    @Override
    public int getDestination() {
        return 7;
    }
}