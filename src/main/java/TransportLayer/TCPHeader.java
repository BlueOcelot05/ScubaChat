package TransportLayer;

import Model.Exceptions.PayloadException;
import Model.Packet;
import Model.LAYER;

/**
 * Represents a simplified TCP (Transmission Control Protocol) header within the Transport Layer.
 * This class models essential TCP header fields like sequence number, acknowledgment number,
 * and the FIN flag, and links to the upper layer packet (payload).
 * It extends the {@link Packet} class, forming part of a packet structure.
 * <p>
 * Note: This implementation uses 12-bit sequence and acknowledgment numbers (0-4095)
 * for simplicity, which differs from the standard 32-bit TCP fields.
 */
public class TCPHeader implements Packet {
    /**
     * Sequence number (0-4095). Identifies the sequence of data in the stream.
     */
    private int seq_no;

    /**
     * The message for the packet.
     */
    private final String message;

    /**
     * The destination of the packet.
     */
    private int destination;

    /**
     * ACK flag indicating it's an acknowledgement packet.
     */
    private boolean Ack;
    /**
     * FIN flag indicating the end of the data transmission from the sender.
     */
    private boolean Fin;

    /**
     * Length of the attached message in bytes (characters).
     */
    private final int payloadLength;
    private boolean syn;

    /**
     * Constructs a new TCP_Header.
     * @param seq_no      The initial sequence number (should be 0-4095).
     * @param is_Fin      The state of the FIN flag (true if set, false otherwise).
     */
    public TCPHeader(int seq_no, boolean is_SYN, boolean is_Ack, boolean is_Fin, String message, int payloadLength, int destination) {
        this.seq_no = seq_no; // Consider validating initial values here too, or ensure setters are called.
        this.syn = is_SYN;
        this.Ack = is_Ack;
        this.Fin = is_Fin;
        this.message = message;
        this.payloadLength = payloadLength;
        this.destination = destination;
    }

    /**
     * Get the message of the packet.
     * @return the message of the packet
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Checks the length of the attached payload.
     * @return the length of the payload
     */
    public int getPayloadLength() {
        return payloadLength;
    }

    /**
     * Gets the sequence number of this TCP header.
     * @return The sequence number (0-4095).
     */
    public int getSequence() {
        return seq_no;
    }

    /**
     * Sets the sequence number for this TCP header.
     * The sequence number must be between 0 and 4095 (inclusive).
     * @param seq_no The new sequence number.
     * @throws PayloadException If the sequence number is negative or exceeds the 4095 threshold.
     */
    public void setSequence(int seq_no) throws PayloadException {
        if (seq_no > 4095) {
            throw new PayloadException(LAYER.TRANSPORT, this,
                    "Sequence Number is higher than the threshold (4095)");
        }
        if (seq_no < 0) {
            throw new PayloadException(LAYER.TRANSPORT, this, "Sequence Number can't be negative");
        }
        this.seq_no = seq_no;
    }


    /**
     * Checks if the FIN (Finish) flag is set.
     * The FIN flag indicates that the sender has finished sending data.
     * @return true if the FIN flag is set, false otherwise.
     */
    public boolean isFin() {
        return Fin;
    }

    public void setDestination(int Destination) {
        this.destination = Destination;
    }

    /**
     * Sets the state of the FIN (Finish) flag.
     * @param is_Fin The new state for the FIN flag (true to set, false to clear).
     */
    public void setFin(boolean is_Fin) {
        this.Fin = is_Fin;
    }

    /**
     * @return the header before this one or the payload (as in the data from the app layer)
     */
    @Override
    public Packet getUpperHeader() {
        return null;
    }

    /**
     * Gets the destination identifier by delegating the call to the upper header.
     * This assumes the upper header/packet contains the ultimate destination information.
     * Behavior is undefined if the upperHeader does not properly implement getDestination()
     * or if upperHeader is the internal placeholder (`Fix_IT`) without a destination.
     * @return The destination identifier obtained from the upper header (result of {@code getUpperHeader().getDestination()}).
     */
    public int getDestination() {
        return this.destination;
    }

    /**
     * Checks whether the packet is an acknowledgment.
     * @return true if it's an ack, false otherwise
     */
    public boolean isAck() {
        return Ack;
    }

    /**
     * Sets the flag to the specified boolean.
     * @param ack the value Ack should be set to
     */
    public void setAckFlag(boolean ack) {
        Ack = ack;
    }

    public boolean isSYN() {
        return this.syn;
    }
}