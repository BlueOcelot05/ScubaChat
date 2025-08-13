package TransportLayer;

/**
 * <h1> Connection interface</h1>
 * <p>
 * Has the purpose of separating connections when sending TCP messages.
 */
public interface Connection {



    boolean receivePacket(TCPHeader packet);

    /**
     * @return the other side of this connection.
     */
    Integer getPeer();
}
