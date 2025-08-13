package Model.Exceptions;

import Model.LAYER;
import Model.Packet;

/**
 * Exception thrown when an error occurs during the routing process.
 * This can include issues such as invalid destination addresses, routing loops,
 * unreachable hosts, or errors in routing table management.
 */
public class RoutingException extends NetworkException {

    private final LAYER layer;
    private final Packet header;
    private final String message;

    /**
     * Constructs a {@code RoutingException} with the specified layer, faulty packet, and message.
     *
     * @param layerWhereItHappened the {@link LAYER} where the routing exception occurred.
     * @param malformedPacket      the {@link Packet} that caused the routing exception.
     * @param message              a detailed message describing the routing error.
     */
    public RoutingException(LAYER layerWhereItHappened, Packet malformedPacket, String message) {
        this.layer = layerWhereItHappened;
        this.header = malformedPacket;
        this.message = message;
    }

    /**
     * Gets the faulty header that caused this routing exception.
     *
     * @return the {@link Packet} that was malformed or caused the routing error.
     */
    public Packet getFaultyHeader() {
        return header;
    }

    /**
     * Gets the {@link LAYER} where this routing exception occurred.
     *
     * @return the {@link LAYER} enum representing the layer where the exception was thrown.
     */
    public LAYER getLayerWhereTheAuchieBoboed() {
        return layer;
    }


}
