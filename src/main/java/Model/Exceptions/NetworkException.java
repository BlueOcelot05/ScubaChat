package Model.Exceptions;

import Model.Packet;
import Model.LAYER;

/**
 * This is the parent exception for all other exceptions that are specific to this system.
 * This is useful for distinguishing if the error was caused by something to do with the
 * design or the code.
 */
public abstract class NetworkException extends Exception {


    private Packet faultyHeader;
    private String message;
    private LAYER layerWhereTheAuchieBoboed;


    /**
     * Gets the faulty header that caused this exception or the layer that was being
     * processed when the exception occurred.
     *
     * @return the {@link Packet} that caused the exception, or {@code null} if the
     * exception isn't directly related to a header.
     */
    public abstract Packet getFaultyHeader();

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
     * Gets the {@link LAYER} enum where the fault happened. Usually the layer where the
     * exception was thrown.
     *
     * @return the {@link LAYER} enum representing the layer where the exception occurred
     * ({@link LAYER#LINK LINK}, {@link LAYER#NETWORK NETWORK},
     * {@link LAYER#TRANSPORT TRANSPORT}, or {@link LAYER#APP APP}).
     */
    public abstract LAYER getLayerWhereTheAuchieBoboed();


}
