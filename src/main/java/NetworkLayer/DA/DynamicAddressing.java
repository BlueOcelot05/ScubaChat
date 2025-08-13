package NetworkLayer.DA;

import LinkLayer.LinkLayer;
import Model.Exceptions.NetworkException;
import Model.LayerModel;
import Model.Packet;
import NetworkLayer.DA_Header;
import NetworkLayer.NetworkingDAO;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

/** Implementation Class For Dynamic Adressing */
public enum DynamicAddressing implements LayerModel {

    INSTANCE;

    private final Random random = new Random();
    private final Map<Integer, Set<Integer>> seqMap = new HashMap<>(); // Mapping between node -> last seq no seen
    private final Integer KEEP_ALIVE_INTERVAL = 30;
    private final ScheduledExecutorService executor;
    private final int SEQ_NO_EXPIRY_TIME = 2;


    /**
     * Private constructor for singleton.
     * Initializes local address from the NetworkingDAO and
     * starts with TTL=60 for ourselves.
     */
    DynamicAddressing() {
        executor = Executors.newScheduledThreadPool(2);
        executor.scheduleAtFixedRate(
                () -> {
                    try {
                        LinkLayer.getSingleton().ReceiveHeader(makeHelloPacket());
                    } catch (NetworkException e) {
                        throw new RuntimeException(e);
                    }
                },
                1,
                KEEP_ALIVE_INTERVAL,
                TimeUnit.SECONDS
        );
    }

    private int getLocalAddress() {
        return NetworkingDAO.getInstance().getLocalAddress();
    }

    /**
     * Gets the singleton instance.
     */
    public static DynamicAddressing getSingleton() {
        return INSTANCE;
    }

    /**
     * Method used to remove sees from the list of seen sequence numbers. Allows for reusing them.
     *
     * @param seq to be removed
     */
    public synchronized void removeSeq(int seq) {
        seqMap.get(getLocalAddress()).remove(seq);
    }

    /**
     * Method used to remove sees from the list of seen sequence numbers. Allows for reusing them.
     *
     * @param seq to be removed
     */
    public synchronized void removeSeq(int addr, int seq) {
        seqMap.get(addr).remove(seq);
    }

    /**
     * @return a new random seq whose timeout is already handled
     */
    private int getSeq() {
        int randomNum = ThreadLocalRandom.current().nextInt(0, 0b11111111 + 1);
        if (!seqMap.containsKey(getLocalAddress())) {
            seqMap.put(getLocalAddress(), new HashSet<>());
        }
        seqMap.get(getLocalAddress()).add(randomNum);
        executor.schedule(() -> removeSeq(randomNum), SEQ_NO_EXPIRY_TIME, TimeUnit.MINUTES);

        return randomNum;
    }

    /**
     * Builds a HELLO packet
     * sequence auto-increments, address=our local address.
     *
     * @return a DA_Header object representing the packet
     */
    public DA_Header makeHelloPacket() {
        return new DA_Header(0, false, getSeq(), getLocalAddress());
    }

    /**
     * Builds a CONFLICT packet (conflict=true).
     *
     * @return a DA_Header object representing the conflict packet
     */
    private DA_Header makeConflictPacket() {
        return new DA_Header(0, true, getSeq(), getLocalAddress());
    }

    /**
     * Called when we receive a DA_Header from the network.
     * Distinguishes HELLO vs. CONFLICT and updates state accordingly.
     *
     * @param header The inbound DA_Header
     */
    @Override
    public void ReceiveHeader(Packet header) {
        DA_Header packet = (DA_Header) header;

        if (!seqMap.containsKey(packet.getAddress())) {
            seqMap.put(packet.getAddress(), new HashSet<>());
        }
        if (seqMap.get(packet.getAddress()).contains(packet.getSequence())) {
            return;
        }
        seqMap.get(packet.getAddress()).add(packet.getSequence());
        executor.schedule(() -> removeSeq(packet.getAddress(), packet.getSequence()), SEQ_NO_EXPIRY_TIME, TimeUnit.MINUTES);


        if (packet.getAddress() == NetworkingDAO.getInstance().getLocalAddress()) {
            if (packet.isConflict()) {
                handleConflict(packet.getAddress());
            } else {
                try {
                    LinkLayer.getSingleton().ReceiveHeader(makeConflictPacket());
                } catch (NetworkException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            handleHello(packet);
        }

    }

    /**
     * Conflict logic
     * if the conflict is for us => renew address,
     * otherwise ignore.Renews our local address if we detect conflict for ourselves.
     * Clears old neighbor info, re-adds ourselves with TTL=60.
     */
    private void handleConflict(int senderAddr) {
        int newAddr;
        do {
            newAddr = random.nextInt(16);
        } while (newAddr == senderAddr);

        NetworkingDAO.getInstance().setLocalAddress(newAddr);

    }


    /**
     * HELLO logic: add or refresh TTL for the sender.
     */
    private void handleHello(DA_Header hello) {
        //Should we rebroadcast
        try {
            LinkLayer.getSingleton().ReceiveHeader(hello);
        } catch (NetworkException e) {
            throw new RuntimeException(e);
        }
        NetworkingDAO.getInstance().updateNode(hello.getAddress());


    }

}