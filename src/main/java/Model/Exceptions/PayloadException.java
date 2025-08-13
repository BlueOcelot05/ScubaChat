package Model.Exceptions;

import Model.Packet;
import Model.LAYER;

/**
 * Exception thrown when an error occurs related to the payload of a packet.
 * This could include issues with the content, format, or integrity of the data
 * being transmitted.
 */
public class PayloadException extends NetworkException {

    private final Packet whichHeader;
    private final String message;
    private final LAYER layerWhereTheAuchieBoboed;

    /**
     * Constructs a {@code PayloadException}.
     *
     * @param layer   the {@link LAYER} where the payload error occurred.
     * @param header  the {@link Packet} header associated with the error.
     * @param massage a detailed message describing the payload error.
     */
    public PayloadException(LAYER layer, Packet header, String massage) {
        layerWhereTheAuchieBoboed = layer;
        whichHeader = header;
        message = massage;
    }

    /**
     * Gets the header associated with the payload error.
     *
     * @return the {@link Packet} header that caused the exception.
     */
    @Override
    public Packet getFaultyHeader() {
        return whichHeader;
    }

    /**
     * Gets the detailed message of this exception.
     *
     * @return the exception's message.
     */
    @Override
    public String getMessage() {
        return message;
    }


    /**
     * Gets the {@link LAYER} where this exception occurred.
     *
     * @return the {@link LAYER} enum representing the layer where the exception was thrown.
     */
    @Override
    public LAYER getLayerWhereTheAuchieBoboed() {
        return layerWhereTheAuchieBoboed;
    }

}
