package TransportLayer;

import Model.Exceptions.NetworkException;
import Model.Packet;
import NetworkLayer.NetworkLayer;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implements the sending end of a TCP connection.
 * Handles splitting messages into packets, sending them, and managing retransmissions
 * when acknowledgments are not received within the timeout period.
 */
public class Sender implements Connection {
    private static final long TCP_RETRANSMIT_TIME = 25;
    private final int destination;
    private final int firstSeq;
    private final int lastSeq;
    private final Map<Integer, String> messages;
    private final Set<Integer> allUsedSeq; // All the ones ever used. For deconstruction
    private final ScheduledExecutorService scheduler;

    /** I think this better
     * Constructs a new Sender for sending a message to a specific destination.
     * Splits the message into appropriate sized packets and begins transmission.
     * Sets up retransmission timers for reliability.
     *
     * @param destination The address to send the message to
     * @param message The complete message to be sent
     */
    public Sender(int destination, String message) {
        this.messages = new HashMap<>();
        this.destination = destination;
        this.scheduler = Executors.newScheduledThreadPool(1);

        List<String> subMessages = TransportLayer.splitMessage(message);
        this.firstSeq = Collections.max(TransportLayer.getSingleton().getUsedSeq(destination)) + 1;
        this.lastSeq = firstSeq + subMessages.size() - 1;
        for (int i = firstSeq; i < this.lastSeq + 1; i++) {
            TransportLayer.getSingleton().addUsedSeq(destination, i);
            messages.put(i, subMessages.get(i - firstSeq));
        }

        for (Integer seq : messages.keySet()) {
            try {
                Packet tcpHeader = new TCPHeader(seq, seq == firstSeq, false,
                        seq == lastSeq, messages.get(seq), messages.get(seq).length(), destination);
                NetworkLayer.getSingleton().ReceiveHeader(tcpHeader); // Send it and set a timer for retransmission

                scheduler.schedule(
                        () -> resendPacket(seq),
                        TCP_RETRANSMIT_TIME + seq * 2 - firstSeq,
                        TimeUnit.SECONDS);
            } catch (NetworkException e) {
                System.out.println(e.getMessage());
            }
        }

        allUsedSeq = messages.keySet();
    }

    /**
     * Resends a packet if it hasn't been acknowledged.
     * Schedules another retransmission if needed or completes the connection if all packets are acknowledged.
     *
     * @param seq The sequence number of the packet to resend
     */
    private synchronized void resendPacket(int seq) {
        if (messages.containsKey(seq)) {
            Packet tcpHeader = new TCPHeader(seq, seq == firstSeq, false,
                    seq == lastSeq, messages.get(seq), messages.get(seq).length(), destination);
            try {
                NetworkLayer.getSingleton().ReceiveHeader(tcpHeader); // Send it and set a timer for retransmission
            } catch (NetworkException e) {
                throw new RuntimeException(e);
            }

            scheduler.schedule(
                    () -> resendPacket(seq),
                    TCP_RETRANSMIT_TIME,
                    TimeUnit.SECONDS);
        } else if (messages.isEmpty()) {
            TransportLayer.getSingleton().removeConnection(this);
            doneSending();
        }
    }

    /**
     * Finalizes the connection by sending a FIN packet and cleaning up resources.
     * Sends a final packet with both ACK and FIN flags set and removes used sequence numbers.
     */
    private void doneSending() {
        Packet tcpHeader = new TCPHeader(lastSeq, firstSeq == lastSeq, true,
                true, messages.get(lastSeq), messages.get(lastSeq).length(), destination);
        try {
            NetworkLayer.getSingleton().ReceiveHeader(tcpHeader); // Send it and set a timer for retransmission
        } catch (NetworkException e) {
            throw new RuntimeException(e);
        }

        for (int seq : allUsedSeq) {
            TransportLayer.getSingleton().removeUsedSeq(destination, seq);
        }
        TransportLayer.getSingleton().removeConnection(this);

    }

    /**
     * Processes an acknowledgment packet from the receiver.
     * Removes acknowledged packets from the retransmission queue.
     *
     * @param packet The TCP header received from the network layer
     * @return true if the acknowledgment was processed successfully, false otherwise
     */
    public synchronized boolean receivePacket(TCPHeader packet) {
        if (!packet.isAck()) {
            return false;
        }
        if (packet.getSequence() < firstSeq || packet.getSequence() > lastSeq) {
            return false;
        }
        if (packet.isFin()) {
            messages.clear();
        }
        messages.remove(packet.getSequence());

        return true;
    }


    /**
     * @return the other side of this connection.
     */
    @Override
    public Integer getPeer() {
        return destination;
    }

}
