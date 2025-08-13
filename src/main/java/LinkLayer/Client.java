package LinkLayer;

import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;


public class Client {

    private SocketChannel sock;

    private final BlockingQueue<Message> sendingQueue;
    private final String token;


    public Client(String server_ip, int server_port, int frequency, String token, BlockingQueue<Message> receivedQueue, BlockingQueue<Message> sendingQueue) {
        this.sendingQueue = sendingQueue;
        this.token = token;
        SocketChannel sock;
        Sender sender;
        Listener listener;
        try {
            sock = SocketChannel.open();
            sock.connect(new InetSocketAddress(server_ip, server_port));
            listener = new Listener(sock, receivedQueue);
            sender = new Sender(sock, sendingQueue);

            sender.sendConnect(frequency);
            sender.sendToken(token);

            listener.start();
            sender.start();
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e);
            System.exit(1);
        }
    }

    private class Sender extends Thread {
        private final BlockingQueue<Message> sendingQueue;
        private final SocketChannel sock;

        public Sender(SocketChannel sock, BlockingQueue<Message> sendingQueue) {
            super();
            this.sendingQueue = sendingQueue;
            this.sock = sock;
        }

        private void senderLoop() {
            while (sock.isConnected()) {
                try {
                    Message msg = sendingQueue.take();
                    if (msg.getType() == MessageType.DATA || msg.getType() == MessageType.DATA_SHORT) {
                        ByteBuffer data = msg.getData();
                        data.position(0); //reset position just to be sure
                        int length = data.capacity(); //assume capacity is also what we want to send here!
                        ByteBuffer toSend = ByteBuffer.allocate(length + 2);
                        if (msg.getType() == MessageType.DATA) {
                            toSend.put((byte) 3);
                        } else { // must be DATA_SHORT due to check above
                            toSend.put((byte) 6);
                        }
                        toSend.put((byte) length);
                        toSend.put(data);
                        toSend.position(0);
                        sock.write(toSend);
                    } else {
                        System.err.println("Unexpected message type received for sending: " + msg.getType());
                    }
                } catch (IOException e) {
                    System.err.println("Error in socket (sender): " + e);
                } catch (InterruptedException e) {
                    System.err.println("Failed to take from sendingQueue: " + e);
                }
            }
        }

        public void sendConnect(int frequency) {
            ByteBuffer buff = ByteBuffer.allocate(4);
            buff.put((byte) 9);
            buff.put((byte) ((frequency >> 16) & 0xff));
            buff.put((byte) ((frequency >> 8) & 0xff));
            buff.put((byte) (frequency & 0xff));
            buff.position(0);
            try {
                sock.write(buff);
            } catch (IOException e) {
                System.err.println("Failed to send HELLO");
            }
        }

        public void sendToken(String token) {
            byte[] tokenBytes = token.getBytes();
            ByteBuffer buff = ByteBuffer.allocate(tokenBytes.length + 2);
            buff.put((byte) 10);
            buff.put((byte) tokenBytes.length);
            buff.put(tokenBytes);
            buff.position(0);
            try {
                sock.write(buff);
            } catch (IOException e) {
                System.err.println("Failed to send HELLO");
            }
        }

        public void run() {
            senderLoop();
        }

    }


    private class Listener extends Thread {
        private final BlockingQueue<Message> receivedQueue;
        private final SocketChannel sock;

        public Listener(SocketChannel sock, BlockingQueue<Message> receivedQueue) {
            super();
            this.receivedQueue = receivedQueue;
            this.sock = sock;
        }

        private ByteBuffer messageBuffer = ByteBuffer.allocate(1024);
        private int messageLength = -1;
        private boolean messageReceiving = false;
        private boolean shortData = false;

        private void parseMessage(ByteBuffer received, int bytesReceived) {

            try {
                for (int offset = 0; offset < bytesReceived; offset++) {
                    byte d = received.get(offset);

                    if (messageReceiving) {
                        if (messageLength == -1) {
                            messageLength = d;
                            messageBuffer = ByteBuffer.allocate(messageLength);
                        } else {
                            messageBuffer.put(d);
                        }
                        if (messageBuffer.position() == messageLength) {
                            // Return DATA here
                            messageBuffer.position(0);
                            ByteBuffer temp = ByteBuffer.allocate(messageLength);
                            temp.put(messageBuffer);
                            temp.rewind();
                            if (shortData) {
                                receivedQueue.put(new Message(MessageType.DATA_SHORT, temp));
                            } else {
                                receivedQueue.put(new Message(MessageType.DATA, temp));
                            }
                            messageReceiving = false;
                        }
                    } else {
                        if (d == 0x09) { // Connection successfull!
                            receivedQueue.put(new Message(MessageType.HELLO));
                        } else if (d == 0x01) { // FREE
                            receivedQueue.put(new Message(MessageType.FREE));
                        } else if (d == 0x02) { // BUSY
                            receivedQueue.put(new Message(MessageType.BUSY));
                        } else if (d == 0x03) { // DATA!
                            messageLength = -1;
                            messageReceiving = true;
                            shortData = false;
                        } else if (d == 0x04) { // SENDING
                            receivedQueue.put(new Message(MessageType.SENDING));
                        } else if (d == 0x05) { // DONE_SENDING
                            receivedQueue.put(new Message(MessageType.DONE_SENDING));
                        } else if (d == 0x06) { // DATA_SHORT
                            messageLength = -1;
                            messageReceiving = true;
                            shortData = true;
                        } else if (d == 0x08) { // END, connection closing
                            receivedQueue.put(new Message(MessageType.END));
                        } else if (d == 0x0A) { // TOKEN_ACCEPTED
                            receivedQueue.put(new Message(MessageType.TOKEN_ACCEPTED));
                        } else if (d == 0x0B) { // TOKEN_REJECTED
                            receivedQueue.put(new Message(MessageType.TOKEN_REJECTED));
                        }
                    }
                }

            } catch (InterruptedException e) {
                System.err.println("Failed to put data in receivedQueue: " + e);
            }
        }


        public void receivingLoop() {
            int bytesRead;
            ByteBuffer recv = ByteBuffer.allocate(1024);
            try {
                while (sock.isConnected()) {
                    bytesRead = sock.read(recv);
                    if (bytesRead > 0) {
                        parseMessage(recv, bytesRead);
                    } else {
                        break;
                    }
                    recv.clear();
                }
            } catch (IOException e) {
                System.out.println("Error in socket (receiver): " + e);
            }

        }

        public void run() {
            receivingLoop();
        }

    }
}