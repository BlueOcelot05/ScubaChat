package Model.Exceptions;

import LinkLayer.MessageType;
import Model.LAYER;
import Model.Packet;

/**
 * Exception thrown when a {@link LinkLayer.Message} of an incompatible type is received in
 * the {@link LinkLayer.LinkLayer LinkLayer}.
 */
public class InvalidMessageType extends NetworkException {

    private final MessageType messageType;
    private final LAYER layer;

    /**
     * Constructs an {@code InvalidMessageType} exception.
     *
     * @param messageType the invalid message type that was received.
     * @param layer       the {@link LAYER} where this exception occurred.
     */
    public InvalidMessageType(MessageType messageType, LAYER layer) {
        this.messageType = messageType;
        this.layer = layer;

    }

    /**
     * Gets the invalid message type that caused this exception.
     *
     * @return the invalid {@link MessageType}.
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * This exception is not directly related to a specific header.
     *
     * @return {@code null} as there is no faulty header associated with this exception.
     */
    public Packet getFaultyHeader() {
        return null;
    }

    /**
     * Gets the {@link LAYER} where this exception occurred.
     *
     * @return the {@link LAYER} enum representing the layer where the exception was thrown.
     */
    public LAYER getLayerWhereTheAuchieBoboed() {
        return this.layer;
    }

}
