package LinkLayer;

import Model.Exceptions.InvalidMessageType;
import Model.Exceptions.NetworkException;
import Model.LAYER;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <h1>CSMA/CA Medium Access Control</h1>
 * <p>
 * This class handles the CSMA/CA MAC. It receives {@link Message messages}, waits for the network to
 * become free, then awaits another random amount of time after which, if the network is still free it
 * will send the message. In the case the network is not free at any point, the process is restarted.
 */
public class CSMA extends Thread {

    // This is where messages are put when they are sent
    private final BlockingQueue<Message> sendList;
    // This is where messages are put when they're waiting to be sent
    private final BlockingQueue<Message> waitingQueue;
    private LinkLayer linkLayer;

    /**
     * Constructor for the CSMA Thread Object.
     *
     * @param sendingQueue the same sending queue as the {@link Client} object that is
     *                     connected to the server.
     */
    public CSMA(BlockingQueue<Message> sendingQueue) {
        this.sendList = sendingQueue;
        this.waitingQueue = new LinkedBlockingQueue<Message>();
        this.linkLayer = LinkLayer.getSingleton();
    }

    /**
     * <h3>The main way to send messages</h3>
     * <p>
     * This is the method for sending something on the network.This method will initiate
     * the CSMA/CA process with the provided message. This process is not instant so the
     * transmission could take a while. However, <b>this method does not block</b> as the process
     * is handled in a separate thread to which this method just passes the message.
     * <p>
     * The CSMA/CA process is detailed in the javadoc of {@link CSMA CSMA/CA}
     *
     * @param message Typically achieved by passing the output
     *                of  to
     *                the {@link Message Message} constructor. Can only be of
     *                type {@link MessageType#DATA_SHORT DATA_SHORT} or {@link MessageType#DATA DATA}
     * @throws Model.Exceptions.NetworkException if passed another message type than {@link MessageType#DATA_SHORT DATA_SHORT}
     *                                           or {@link MessageType#DATA DATA}
     */
    public void sendMessage(Message message) throws NetworkException {
        if (message.getType() != MessageType.DATA && message.getType() != MessageType.DATA_SHORT) {
            throw new InvalidMessageType(message.getType(), LAYER.LINK);
        }
        waitingQueue.add(message);
    }


    public void run() {
        try {
            while (true) { // Main loop of the thread
                Message pendingMessage = waitingQueue.take();

                while (true) { // per package loop

                    linkLayer = LinkLayer.getSingleton();


                    if (!linkLayer.isNetworkFree()) {
                        linkLayer.waitForNetworkFree();
                    }

                    waitRandomTime();

                    if (linkLayer.isNetworkFree()) {
                        sendList.put(pendingMessage);
                        break;

                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitRandomTime() throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextInt(10, 30) * 100L);
    }


}
