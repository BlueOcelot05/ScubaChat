package LinkLayer;

import Model.Exceptions.NetworkException;
import Model.Exceptions.PayloadException;
import Model.Exceptions.RoutingException;
import Model.LayerModel;
import Model.Packet;
import Model.PacketParser;
import NetworkLayer.AddressHeader;
import NetworkLayer.DA_Header;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static LinkLayer.LinkLayer.Constants.*;

/**
 * <h1>LINK LAYER</h1>
 * <p>
 * This is the singleton class of the link layer.
 */
public enum LinkLayer implements LayerModel {
    INSTANCE(SERVER_IP, SERVER_PORT, frequency);


    /**
     * @return this singleton
     */
    public static synchronized LinkLayer getSingleton() {
        return INSTANCE;
    }

    /**
     * Class containing the static variables of the connection. This is only necessary
     * because of funky things about instantiating the singleton.
     */
    static class Constants {
        // The host to connect to. Set this to localhost when using the audio interface tool.
        static final String SERVER_IP = "netsys.ewi.utwente.nl"; //"127.0.0.1";
        // The port to connect to. 8954 for the simulation server.
        static final int SERVER_PORT = 8954;
        // The frequency to use.
        static final int frequency = 725;
        // View the simulator at https://netsys.ewi.utwente.nl/integrationproject/
        // The token you received for your frequency range
        static final String token = "java-02-FE202AF81C2245C45F";
    }


    private final BlockingQueue<Message> receivedQueue;
    private final BlockingQueue<Message> sendingQueue;
    private final CSMA MAC;
    private final Lock lock = new ReentrantLock();
    private final Condition waitForNetworkFree = lock.newCondition();

    private boolean networkFree = true; // Used to indicate if the network is busy at the moment

    LinkLayer(String server_ip, int server_port, int frequency) {
        receivedQueue = new LinkedBlockingQueue<Message>();
        sendingQueue = new LinkedBlockingQueue<Message>();

        new Client(SERVER_IP, SERVER_PORT, frequency, token, receivedQueue,
                sendingQueue); // Give the client the Queues to use

        new Receiver(receivedQueue).start(); // Start thread to handle received messages!
        MAC = new CSMA(sendingQueue);
        MAC.start();
    }


    /**
     * Thread safe method to check if the network is busy.
     *
     * @return <b>True</b> if a package is currently being sent
     * be it by this node or another. Otherwise, it returns false.
     */
    public boolean isNetworkFree() {
        lock.lock();
        try {
            return networkFree;
        } finally {
            lock.unlock();
        }
    }


    /**
     * Setter method for the status of the network.
     *
     * @param sock boolean representing the <b>true/false</b> value of the
     *             {@link LinkLayer#isNetworkFree() isNetworkFree()} return value.
     *             Should be <b>True</b> if the network is <b>free</b> otherwise <b>False</b>
     */
    public void setNetworkFree(boolean sock) {
        networkFree = sock; // Network is now a free elf

        // if the network was set to free unblock the threads waiting on waitForNetworkFree
        if (sock) {
            lock.lock();
            try {
                waitForNetworkFree.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Calling this method blocks until the network becomes free.
     * All waiting threads are unblocked at the same time. No guarantees about
     * the order at which this happens are made.
     */
    public void waitForNetworkFree() throws InterruptedException {
        lock.lock();
        try {
            waitForNetworkFree.await();
        } finally {
            lock.unlock();
        }
    }


    /**
     * The method used to communicate with the layer.
     *
     * @param header the message that it's supposed to process.
     * @throws NetworkException when something goes wrong with either the processing of the header
     *                          or the network
     */
    @Override
    public void ReceiveHeader(Packet header) throws NetworkException {
        ByteBuffer payload = null;
        try {
            switch (header) {
                case AddressHeader addressHeader -> payload = PacketParser.maker_long(addressHeader);
                case DA_Header daHeader -> payload = PacketParser.maker_short(daHeader);
                default -> throw new IllegalStateException("Unexpected value: " + header);
            }
        } catch (PayloadException e) {
            System.out.println(e.getMessage());
        } catch (IllegalStateException | NullPointerException e) {
            System.out.println(e.getMessage());
        } catch (RoutingException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Message message;
        if (Objects.requireNonNull(payload).capacity() > 2) {
            message = new Message(MessageType.DATA, payload);
        } else {
            message = new Message(MessageType.DATA_SHORT, payload);
        }

        MAC.sendMessage(message);
    }


}

