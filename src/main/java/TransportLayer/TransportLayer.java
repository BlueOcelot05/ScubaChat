package TransportLayer;

import Model.Packet;
import Model.LayerModel;
import NetworkLayer.NetworkingDAO;

import java.util.*;

/**
 * Represents the Transport Layer in a networking stack, implementing the {@link LayerModel} interface.
 * This layer is responsible for reliable message transfer, handling segmentation, acknowledgment,
 * and reassembly of messages for communication between nodes.
 * Utilizes a Singleton design pattern to ensure a single instance of the Transport Layer exists.
 */
public enum TransportLayer implements LayerModel {
    // Singleton
    INSTANCE;

    // Get the singleton instance
    public static TransportLayer getSingleton() {
        return INSTANCE;
    }

    // Prevent instantiation
    TransportLayer() {
    }

    private final Map<Integer, Set<Integer>> usedSeqNo = new HashMap<>();
    private final Map<Integer, Set<Connection>> connections = new HashMap<>();


    /**
     * Method used to keep track of the sequence numbers of all connections.
     * @param destination the sequence numbers only need to differ for connections to the same other node.
     * @return set of all sequence numbers used in communication with the destination node
     */
    public synchronized Set<Integer> getUsedSeq(int destination) {
        if (!usedSeqNo.containsKey(destination)) {
            usedSeqNo.put(destination, new HashSet<>());
            usedSeqNo.get(destination).add(0);
            return usedSeqNo.get(destination);
        }
        return usedSeqNo.get(destination);
    }


    public synchronized void addUsedSeq(int destination, int usedSeq) {
        if (!usedSeqNo.containsKey(destination)) {
            usedSeqNo.put(destination, new HashSet<>());
        }
        usedSeqNo.get(destination).add(usedSeq);
    }


    /**
     * Splits the message to be sent into 24-byte parts.
     *
     * @param message the message to be sent
     * @return the split message
     */
    public static List<String> splitMessage(String message) {
        return Arrays.asList(message.split("(?<=\\G.{" + 24 + "})"));
    }

    @Override
    public void ReceiveHeader(Packet packet) {
    }

    /**
     * Processes an incoming app header by adding a tcp header to it.
     *
     * @param message     the message that needs to be sent
     * @param destination the node to send the message to
     */
    public void sendNewMessage(String message, int destination) {
        // Split the message into 24-byte parts

        if (destination == 7) { // Initialize message sending for evey node in range
            for (int node : NetworkingDAO.getInstance().getKnownNodes()) {
                if (!connections.containsKey(node)) {
                    connections.put(node, new HashSet<>());
                }
                connections.get(node).add(new Sender(node, message));
            }
        } else { // Initialize message sending for the whisper destination
            if (!connections.containsKey(destination)) {
                connections.put(destination, new HashSet<>());
            }
            connections.get(destination).add(new Sender(destination, message));
        }
    }


    /**
     * Processes an incoming tcp header.
     *
     * @param header the tcp header that came in
     * @param source the node that send this packet
     */
    public void receiveTcpHeader(TCPHeader header, int source) {
        if (!connections.containsKey(source)) {
            connections.put(source, new HashSet<>());
        }

        if (connections.get(source).stream().noneMatch(s -> s.receivePacket(header))) {
            if (!header.isAck()) {
                connections.get(source).add(new Receiver(source, header));
            }
        }
    }


    /**
     * Used when the connection to a node should be terminated.
     * @param conn to be removed
     */
    public synchronized void removeConnection(Connection conn) {
        connections.get(conn.getPeer()).remove(conn);

    }


    public synchronized void removeUsedSeq(int peer, int seq) {
        usedSeqNo.get(peer).remove(seq);
    }
}