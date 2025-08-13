package LinkLayer;

import java.nio.ByteBuffer;

public class Message {
    private final MessageType type;
    private ByteBuffer data;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, ByteBuffer data) {
        this.type = type;
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public ByteBuffer getData() {
        return data;
    }
}