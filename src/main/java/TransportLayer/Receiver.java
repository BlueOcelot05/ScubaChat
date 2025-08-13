package TransportLayer;

import AppLayer.TUI;
import Model.Exceptions.NetworkException;
import NetworkLayer.NetworkLayer;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implements the receiving end of a TCP connection.
 * Handles receiving and ordering packets, acknowledging them, and passing complete messages to the application layer.
 */
public class Receiver implements Connection {

    private int firstSeq;
    private final int source;
    private boolean fullyReceived;
    private Set<Integer> waiting_packets = new HashSet<>();
    private final Map<Integer, String> messages = new HashMap<>();
    private final ScheduledExecutorService scheduler;
    private final static int CONNECTION_TIMEOUT = 100;
    private ScheduledFuture<?> promise;
    private int maxSeq;
    private boolean finack = false;

    /**
     * Constructs a new Receiver for handling incoming TCP packets from a specific source.
     * Initializes sequence numbers and sets up timers based on the first received header.
     *
     * @param source The address of the sender
     * @param firstHeader The first TCP header received from the sender
     */
    public Receiver(int source, TCPHeader firstHeader) {
        if (firstHeader.isSYN())
            this.firstSeq = firstHeader.getSequence();
        else
            this.firstSeq = Collections.max(TransportLayer.getSingleton().getUsedSeq(source));
        this.source = source;
        this.maxSeq = firstHeader.getSequence() + 40;
        fullyReceived = false;
        scheduler = Executors.newScheduledThreadPool(1);

        for (int i = firstSeq; i < this.maxSeq; i++) {
            TransportLayer.getSingleton().addUsedSeq(source, i);
            waiting_packets.add(i);
        }
        receivePacket(firstHeader);
    }

    /**
     * Sends an acknowledgment packet for a received TCP header.
     * Creates a new TCP header with the ACK flag set and sends it back to the source.
     *
     * @param tcpHeader The TCP header to acknowledge
     */
    private void sendAck(TCPHeader tcpHeader) {
        TCPHeader newTCPHeader = new TCPHeader(tcpHeader.getSequence(), false, true, fullyReceived, "", 0, source);
        try {
            NetworkLayer.getSingleton().ReceiveHeader(newTCPHeader);
        } catch (NetworkException _) {
        }
    }


    /**
     * Processes a received TCP packet.
     * Updates internal state, stores message data, acknowledges the packet,
     * and checks if the complete message has been received.
     *
     * @param packet The TCP header received from the network layer
     * @return true if the packet was processed successfully, false if it was outside the expected sequence range
     */
    @Override
    public synchronized boolean receivePacket(TCPHeader packet) {
        if (packet.getSequence() < this.firstSeq || packet.getSequence() > this.maxSeq) {
            return false;
        }
        if (finack) {
            return true; // Connection should be terminated, ignore everything else
        }
        if (packet.isFin()) {
            waiting_packets = waiting_packets.stream().filter(x -> x <= packet.getSequence()).collect(Collectors.toSet());
            maxSeq = packet.getSequence();
        }if (packet.isSYN()){
            int oldSeq = firstSeq;
            firstSeq = packet.getSequence();
            for (int i = oldSeq; i < firstSeq; i++) TransportLayer.getSingleton().removeUsedSeq(source, i);
            waiting_packets.removeIf(x -> x < packet.getSequence());
        }

        if (waiting_packets.contains(packet.getSequence())) {
            messages.put(packet.getSequence(), packet.getMessage());
            waiting_packets.remove(packet.getSequence());
        }
        if (waiting_packets.isEmpty()) {
            if (!fullyReceived) {
                fullyReceived = true;
                passMessageToTUI();
            }
            if (packet.isFin() && packet.isAck()) {
                finack = true;
            }
            startWaitingForFinAck();
        }

        sendAck(packet);

        return true;
    }

    /**
     * @return the other side of this connection.
     */
    @Override
    public Integer getPeer() {
        return source;
    }

    /**
     * Assembles the complete message from all received packets and passes it to the TUI.
     * Message fragments are ordered by sequence number before being combined.
     */
    private void passMessageToTUI() {
        StringBuilder message = new StringBuilder();
        for (int i = firstSeq; i < maxSeq + 1; i++) {
            message.append(messages.get(i));
        }
        TUI.getSingleton().receiveMessage(message.toString(), source);
    }

    /**
     * Starts or resets the timeout timer for waiting for a FIN-ACK packet.
     * If the connection is idle for too long, it will be terminated.
     */
    private void startWaitingForFinAck() {
        if (promise != null) {
            promise.cancel(true);
        }
        promise = scheduler.schedule(this::endConnection,
                CONNECTION_TIMEOUT,
                TimeUnit.SECONDS
        );
    }

    /**
     * Terminates the connection and cleans up resources.
     * Removes this connection from the transport layer and releases used sequence numbers.
     */
    private void endConnection() {
        TransportLayer.getSingleton().removeConnection(this);
        for (Integer i : messages.keySet()) {
            TransportLayer.getSingleton().removeUsedSeq(source, i);
        }

    }
}
