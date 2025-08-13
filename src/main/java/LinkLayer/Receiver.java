package LinkLayer;

import Model.Exceptions.NetworkException;
import Model.PacketParser;
import NetworkLayer.NetworkLayer;

import java.util.concurrent.BlockingQueue;

public class Receiver extends Thread {

    private final BlockingQueue<Message> receivedQueue;

    public Receiver(BlockingQueue<Message> receivedQueue) {
        super();
        this.receivedQueue = receivedQueue;
    }

    public void run() {
        while (true) {
            try {
                Message msg = receivedQueue.take();

                switch (msg.getType()) {
                    case FREE, DONE_SENDING -> LinkLayer.getSingleton().setNetworkFree(true);
                    case BUSY, SENDING -> LinkLayer.getSingleton().setNetworkFree(false);
                    case DATA -> { // Parse data and send it to the network layer
                        try {
                            NetworkLayer.getSingleton().ReceiveHeader(PacketParser.parser_long(msg.getData()));
                        } catch (NetworkException e) { // If the packet can't be parsed it's discarded
                            //TODO decide if something else should also happen
                        }
                    }
                    case DATA_SHORT -> { // Parse data and send it to the network layer
                        try {
                            NetworkLayer.getSingleton().ReceiveHeader(PacketParser.parser_short(msg.getData()));
                        } catch (NetworkException e) { // If the packet can't be parsed it's discarded
                            //TODO decide if something else should also happen
                        }
                    }
                    case TOKEN_REJECTED -> {
                        System.err.println("Connection to server rejected");
                        System.exit(1);
                    }
                    case TOKEN_ACCEPTED, HELLO -> {
                    }
                    case END -> {
                        System.out.println("RECEIVER EXITING");
                        System.exit(0);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + msg.getType());
                }

            } catch (InterruptedException e) {
                System.err.println("Failed to take from queue: " + e);
            }
        }
    }
}