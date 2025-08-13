package Model;

import Model.Exceptions.PayloadException;
import Model.Exceptions.RoutingException;
import NetworkLayer.AddressHeader;
import NetworkLayer.DA_Header;
import NetworkLayer.DV_Header;
import TransportLayer.TCPHeader;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/*
https://stackoverflow.com/questions/10178980/how-to-convert-a-binary-string-to-a-base-10-integer-in-java
https://mkyong.com/java/java-convert-an-integer-to-a-binary-string/
 */


/**
 * Provides utility methods for parsing network packet headers from binary data (ByteBuffer or bit strings)
 * into corresponding header objects (AddressHeader, TCPHeader, DV_Header, DA_Header) and for
 * constructing binary representations (ByteBuffer) from these header objects.
 * This class handles the low-level conversion between Java objects and the specific bit/byte layouts
 * defined by the custom network protocol.
 */

public class PacketParser {

    public PacketParser() {
    }

    /**
     * Parses a network packet from a ByteBuffer and determines if it's a short (2-byte) or long (32-byte) packet.
     * Based on the size, it delegates to the appropriate parser method.
     *
     * @param received The ByteBuffer containing the raw packet data
     * @return A Packet object representing the parsed packet
     * @throws PayloadException If the ByteBuffer has an invalid size or format
     */
    public static Packet parser(ByteBuffer received) throws PayloadException {
        if (received.capacity() == 2) {
            return parser_short(received);
        } else if (received.capacity() == 32) {
            return parser_long(received);
        } else {
            throw new PayloadException(LAYER.LINK, null, "Received ByteBuffer is cursed");
        }
    }

    /**
     * Creates a ByteBuffer containing the binary representation of a packet.
     * Delegates to the appropriate maker method based on the packet type.
     *
     * @param header The Packet object to serialize
     * @return A ByteBuffer containing the serialized binary data
     * @throws PayloadException If there's an error during payload serialization
     * @throws RoutingException If there's an error related to routing information
     */
    public static ByteBuffer maker(Packet header) throws PayloadException, RoutingException {
        if (header instanceof AddressHeader) {
            return maker_long((AddressHeader) header);
        } else if (header instanceof DA_Header) {
            return maker_short((DA_Header) header);
        } else {
            throw new PayloadException(LAYER.LINK, header, "You can't make a packet with this header as input?");
        }
    }

    /**
     * Parses a complete network packet from a ByteBuffer. It first extracts the 32-bit address header,
     * determines the next protocol (TCP or DV based on the first 4 bits), parses the corresponding
     * upper-layer header, and constructs the final {@link AddressHeader} object containing the
     * parsed upper header.
     *
     * @param data_long The ByteBuffer containing the raw bytes of the entire packet (Address Header + Upper Header).
     *                  The buffer's position and limit will be modified (flipped) during parsing.
     * @return An {@link AddressHeader} object representing the parsed packet structure.
     */
    public static AddressHeader parser_long(ByteBuffer data_long) throws PayloadException {
        //TODO: IF TCP split data_long into 2 buffers or arrays. second buffer is always the last 24 bytes.
        //Then process the 24 byte one with the native UTF8 byte thingy and pass it to the parseTCPHeader
        //parseTCPHeader(tcp_payload, destionation, Message as string)
        String entire_packet = bytes_to_bits(data_long);
        String address = entire_packet.substring(0, 32); //
        if (address.startsWith("0000")) {
            String tcp_and_payload = entire_packet.substring(32);
            Integer destination = bit_to_int(address.substring(20, 24));
            TCPHeader tcpHeader = parseTCPHeader(tcp_and_payload, destination);
            return parseADHeader(address, tcpHeader);
        } else {
            int payload_length = bit_to_int(address.substring(8, 16));
            String dv = entire_packet.substring(32, 32 + payload_length * 8);
            DV_Header dvHeader = parseDVHeader(dv);
            return parseADHeader(address, dvHeader);
        }
    }

    /**
     * Parses a 16-bit (2 bytes) packet into a DA_Header object.
     * This method is used for short packets like acknowledgments.
     *
     * @param data_short The ByteBuffer containing the raw 16-bit packet data
     * @return A DA_Header object representing the parsed packet
     */
    public static DA_Header parser_short(ByteBuffer data_short) {
        return parseDAHeader(bytes_to_bits(data_short));
    }


    /**
     * Constructs a ByteBuffer representing a complete network packet from an {@link AddressHeader} object.
     * It serializes the Address Header first, then determines the type of the upper-layer header
     * (TCP or DV based on the {@code next_Protocol} field) and serializes it. The resulting
     * ByteBuffers are concatenated.
     *
     * @param address_header The {@link AddressHeader} object to serialize, which must contain the
     *                       appropriate upper-layer header object ({@link TCPHeader} or {@link DV_Header}).
     * @return A ByteBuffer containing the serialized binary data for the complete packet. The buffer
     * is flipped and ready for reading/transmission.
     */
    public static ByteBuffer maker_long(AddressHeader address_header) throws PayloadException, RoutingException {
        if (address_header.getNext_Protocol() == 0) {
            TCPHeader tcp = (TCPHeader) address_header.getUpperHeader();
            int letterCount = tcp.getPayloadLength();
            int totalSize = 8 + letterCount;
            ByteBuffer packet = ByteBuffer.allocate(totalSize);
            ByteBuffer adHeader = makeADHeader(address_header); // returns ByteBuffer
            ByteBuffer tcpHeader = makeTCPHeader(tcp);   // returns ByteBuffer

            adHeader.rewind(); // Make sure position is at 0
            tcpHeader.rewind();

            packet.put(adHeader);   // This works if adHeader is small and fits
            packet.put(tcpHeader);

            packet.flip();
            return packet;
        } else {
            DV_Header dv = (DV_Header) address_header.getUpperHeader();
            int dvCount = dv.getDistanceVector().size();
            int totalSize = 4 + dvCount;
            ByteBuffer packet = ByteBuffer.allocate(totalSize);
            ByteBuffer adHeader = makeADHeader(address_header); // returns ByteBuffer
            ByteBuffer dvHeader = makeDVHeader(dv);   // returns ByteBuffer

            adHeader.rewind(); // Make sure position is at 0
            dvHeader.rewind();

            packet.put(adHeader);   // This works if adHeader is small and fits
            packet.put(dvHeader);
            packet.flip();
            return packet;
        }
    }

    /**
     * Creates a ByteBuffer containing the binary representation of a DA_Header (short packet).
     * Converts the DA_Header object into a 16-bit (2 bytes) binary structure.
     *
     * @param da_header The DA_Header object to serialize
     * @return A ByteBuffer containing the serialized 16-bit binary data, flipped and ready for reading
     * @throws PayloadException If there's an error during serialization
     */
    public static ByteBuffer maker_short(DA_Header da_header) throws PayloadException {
        ByteBuffer packet = ByteBuffer.allocate(2);
        packet.put(makeDAHeader(da_header));
        packet.flip();
        return packet;
    }

    /**
     * Serializes a {@link TCPHeader} object into a ByteBuffer containing its binary representation.
     * This method encodes the sequence number (12 bits), acknowledgment number (12 bits),
     * ACK flag (1 bit), FIN flag (1 bit), and payload length (6 bits) into a 32-bit structure.
     * Note: The actual payload data associated with the TCP header is *not* included in the output ByteBuffer.
     *
     * @param tcp The {@link TCPHeader} object to serialize.
     * @return A ByteBuffer containing the 32-bit (4 bytes) binary representation of the TCP header fields.
     * The buffer is flipped and ready for reading.
     */
    public static ByteBuffer makeTCPHeader(TCPHeader tcp) throws PayloadException {
        byte[] message_in_bytes = tcp.getMessage().getBytes(StandardCharsets.UTF_8);
        int totalSize = 4 + message_in_bytes.length; // 4 bytes for the 32-bit header
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        String bit_buffer = int_to_bit(tcp.getSequence(), 23) + int_to_bit(tcp.isSYN() ? 1 : 0,
                1) + (tcp.isAck() ? 1 :
                0) + (tcp.isFin() ? 1 : 0) + int_to_bit(tcp.getPayloadLength(), 6);
        //https://stackoverflow.com/questions/5368704/appending-a-byte-to-the-end-of-another-byte
        buffer.put(bits_to_bytes(bit_buffer));
        //Buffer automaticly gets padded with bytes if you send less than 32 bytes. See document on canvas.
        buffer.put(message_in_bytes);
        return buffer;
    }

    //first 4 bytes addressing header, first 4 bits show what is following, if TCP for example next is app

    //first 4 bytes addressing header, first 4 bits show what is following, if TCP for example next is app

    //general parser to check first 4 bits of the address header, then parse the entire packet.

    /**
     * Parses a {@link TCPHeader} from a binary string representation.
     * Assumes the input string starts with the 32-bit TCP header fields followed by payload data bits.
     * It extracts the sequence number, acknowledgment number, ACK flag, FIN flag, payload length,
     * and the remaining part of the input string is treated as the raw payload message (as a bit string).
     *
     * @param bits The binary string representing the TCP header (first 32 bits) followed by payload bits.
     * @return A new {@link TCPHeader} object populated with the parsed fields. The {@code message} field
     * will contain the remainder of the input {@code bits} string after the header.
     */
    //general parser to check first 4 bits of the address header, then parse the entire packet.
    public static TCPHeader parseTCPHeader(String bits, int destination) throws PayloadException {
        //String bits = bytes_to_bits(buffer);

        Integer seq = bit_to_int(bits.substring(0, 23));
        boolean isS = bits.charAt(23) == '1';
        boolean isA = bits.charAt(24) == '1';
        boolean isF = bits.charAt(25) == '1';
        //TODO: Changed last index from 32bit included instead of skipped 31 - 32
        int payLength = Integer.parseInt(bits.substring(26, 32), 2);
        // Use the payload length from the header to find out how many  bits to parse.
        // Get the bit string that holds the actual message
        String messageBits = bits.substring(32, 32 + (payLength * 8));
        // Convert bits to bytes
        ByteBuffer byteBuffer = bits_to_bytes(messageBits);
        // Decode the bytes to a UTF-8 string
        String msg = new String(byteBuffer.array(), StandardCharsets.UTF_8);

        //packet class that basically contains all the bytebuffers, then we do our shit
        return new TCPHeader(seq, isS, isA, isF, msg, payLength, destination);
    }

    /**
     * Serializes a {@link DA_Header} (Data Acknowledgment or similar custom header) object into a ByteBuffer.
     * Encodes the protocol (3 bits), conflict flag (1 bit), sequence number (8 bits), and address (4 bits)
     * into a 16-bit (2 bytes) structure.
     *
     * @param da The {@link DA_Header} object to serialize.
     * @return A ByteBuffer containing the 16-bit binary representation of the DA header.
     * The buffer is flipped and ready for reading.
     */
    public static ByteBuffer makeDAHeader(DA_Header da) throws PayloadException {
        String bit_buffer = int_to_bit(da.getProtocol(), 3) + (da.isConflict() ? '1' :
                '0') + int_to_bit(da.getSequence(), 8) + int_to_bit(da.getAddress(), 4);
        return bits_to_bytes(bit_buffer);
    }

    /**
     * Parses a {@link DA_Header} from its 16-bit binary string representation.
     * Extracts protocol, conflict flag, sequence number, and address fields.
     *
     * @param bits The 16-bit binary string representing the DA header.
     * @return A new {@link DA_Header} object populated with the parsed fields.
     * @throws NumberFormatException     if binary substrings cannot be parsed into integers.
     * @throws IndexOutOfBoundsException if the input string `bits` is not 16 characters long.
     */
    public static DA_Header parseDAHeader(String bits) {
        //String bits = bytes_to_bits(buffer);

        Integer protocol = bit_to_int(bits.substring(0, 3));
        boolean isConf = bits.charAt(3) == '1';
        Integer seq = bit_to_int(bits.substring(4, 12));
        Integer address = bit_to_int(bits.substring(12, 16));

        return new DA_Header(protocol, isConf, seq, address);
    }


    // there is nothing under the DV Header (Comment from original code)
    //https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map (Comment from original code)

    /**
     * Serializes a {@link DV_Header} (Distance Vector) object into a ByteBuffer.
     * Iterates through the distance vector map provided by the {@code DV_Header}. Each map entry
     * (destination, cost) is encoded as an 8-bit structure (4 bits for destination, 4 bits for cost).
     * All entries are concatenated together.
     *
     * @param dv The {@link DV_Header} object containing the distance vector map to serialize.
     * @return A ByteBuffer containing the concatenated binary representations of all distance vector entries.
     * The buffer is flipped and ready for reading.
     */
    // there is nothing under the DV Header
    //https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
    public static ByteBuffer makeDVHeader(DV_Header dv) throws RoutingException, PayloadException {
        if (dv.getDistanceVector().isEmpty()) {
            throw new RoutingException(LAYER.LINK, dv, "Distance Vector Table Does Not Exist");
        }
        Map<Integer, Integer> table = dv.getDistanceVector();
        String bit_buffer = "";
        int dest;
        int cost;
        for (var entry : table.entrySet()) {
            dest = entry.getKey();
            cost = entry.getValue();
            if (dest > 15 || cost > 15) {
                throw new RoutingException(LAYER.LINK, dv, "Destination or Cost in DV Table is Too High");
            }
            bit_buffer += int_to_bit(dest, 4);
            bit_buffer += int_to_bit(cost, 4);
        }
        return bits_to_bytes(bit_buffer);
    }

    /**
     * Parses a {@link DV_Header} from a binary string representation containing concatenated distance vector entries.
     * Assumes the input string consists of multiple 8-bit segments, where each segment represents one
     * distance vector entry (4 bits destination, 4 bits cost).
     *
     * @param bits The binary string representing the serialized distance vector entries. The length must be a multiple of 8.
     * @return A new {@link DV_Header} object populated with the parsed destination and cost arrays.
     * @throws NumberFormatException     if binary substrings cannot be parsed into integers.
     * @throws IndexOutOfBoundsException if the input string length is not a multiple of 8 or substring operations fail.
     * @throws IllegalArgumentException  If the input bit string length is not a multiple of 8 (implied, not explicitly thrown).
     */
    public static DV_Header parseDVHeader(String bits) {
        //String bits = bytes_to_bits(buffer);
        int entries = bits.length() / 8;
        int[] destinations = new int[entries];
        int[] costs = new int[entries];
        for (int i = 0; i < entries; i++) {
            Integer destination = bit_to_int(bits.substring(i * 8, i * 8 + 4));
            destinations[i] = destination;
            Integer cost = bit_to_int(bits.substring(i * 8 + 4, i * 8 + 8));
            costs[i] = cost;
        }
        return new DV_Header(destinations, costs);
    }


    /**
     * Parses an {@link AddressHeader} from its 32-bit binary string representation and associates
     * it with a pre-parsed upper-layer header.
     * Extracts fields: next protocol (4 bits), TTL (4 bits), payload length (8 bits),
     * source address (4 bits), destination address (4 bits), and the final 8 bits which represent
     * either the Next Hop address or a specific ID, depending on whether the destination address is 7.
     *
     * @param bits        The 32-bit binary string representing the Address Header.
     * @param upperHeader The {@link Packet} object (e.g., {@link TCPHeader}, {@link DV_Header}) that represents
     *                    the layer immediately following this address header.
     * @return A new {@link AddressHeader} object populated with parsed fields and linked to the {@code upperHeader}.
     * @throws NumberFormatException     if binary substrings cannot be parsed into integers.
     * @throws IndexOutOfBoundsException if the input string `bits` is not 32 characters long.
     */
    public static AddressHeader parseADHeader(String bits, Packet upperHeader) {
        //String bits = bytes_to_bits(buffer);

        int next_protocol = bit_to_int(bits.substring(0, 4));
        int ttl = bit_to_int(bits.substring(4, 8));
        int payload_length = bit_to_int(bits.substring(8, 16));
        int source = bit_to_int(bits.substring(16, 20));
        int destination = bit_to_int(bits.substring(20, 24));
        int next_hop_or_id = bit_to_int(bits.substring(24, 32));

        if (destination == 7) {
            return new AddressHeader(next_protocol, ttl, payload_length, source, destination, 0,
                    next_hop_or_id, upperHeader);
        } else {
            return new AddressHeader(next_protocol, ttl, payload_length, source, destination,
                    next_hop_or_id, 0, upperHeader);
        }
    }

    /**
     * Serializes an {@link AddressHeader} object into a ByteBuffer containing its 32-bit binary representation.
     * Encodes fields: next protocol, TTL, payload length, source, destination. The final 8 bits
     * are encoded with either the Next Hop value or the ID value from the header, depending on
     * whether the destination address is 7.
     *
     * @param add The {@link AddressHeader} object to serialize.
     * @return A ByteBuffer containing the 32-bit binary representation of the Address Header.
     * The buffer is flipped and ready for reading.
     */
    public static ByteBuffer makeADHeader(AddressHeader add) throws PayloadException {
        int i;
        if (add.getDestination() == 7) {
            i = add.getID();
        } else {
            i = add.getNext_Hop();
        }
        String bit_buffer = int_to_bit(add.getNext_Protocol(), 4) + int_to_bit(add.getTTL(),
                4) + int_to_bit(
                add.getPayload_Length(), 8) + int_to_bit(add.getSource(), 4) + int_to_bit(
                add.getDestination(), 4) + int_to_bit(i, 8);
        return bits_to_bytes(bit_buffer);
    }

    //parse Address -> TCP -> App

    //int -> String -> for loop 8bit chunks -> parseInt(x,2) -> cast to byte -> add to byte buffer at a aspecifc index or just put

    //make to string different, cant put chunking and casting again, needs to be a different method
    //private int_to_bit(


    /**
     * Converts a non-negative integer into its binary string representation, padded with leading zeros
     * to ensure the resulting string has a specific total length.
     *
     * @param toBit       The non-negative integer to convert.
     * @param totalDigits The desired fixed length of the output binary string.
     * @return The binary string representation of {@code toBit}, left-padded with '0' characters
     * to reach the length {@code totalDigits}.
     * @throws IllegalArgumentException if {@code toBit} requires more bits than {@code totalDigits}
     *                                  or if {@code toBit} is negative (as negative numbers are not explicitly handled here).
     */
    public static String int_to_bit(Integer toBit, Integer totalDigits) {
        return String.format("%" + totalDigits.toString() + "s", Integer.toBinaryString(toBit))
                .replaceAll(" ", "0");
    }

    /**
     * Converts an integer value to its binary string representation.
     * This is a convenience method that simply delegates to int_to_bit().
     *
     * @param bitString The binary string to convert to an integer
     * @return The integer value represented by the binary string
     */
    public static Integer bit_to_int(String bitString) {
        return Integer.parseInt(bitString, 2);
    }

    /**
     * Converts a binary string (composed of '0's and '1's) into a ByteBuffer.
     * The input string's length must be a multiple of 8, as each 8 bits are grouped into one byte.
     *
     * @param bitString The binary string to convert. Its length must be a multiple of 8.
     * @return A ByteBuffer containing the bytes corresponding to the input bit string.
     * The buffer is flipped and ready for reading.
     */
    //https://www.baeldung.com/java-bytebuffer
    public static ByteBuffer bits_to_bytes(String bitString) throws PayloadException {
        if (bitString.length() % 8 != 0) {
            throw new PayloadException(LAYER.LINK, null, "PacketBuilder tried to convert incomplete bits to bytes");
        }
        ByteBuffer buffer = ByteBuffer.allocate(bitString.length() / 8);
        //Byte.parseByte only works with signed, so had to use casting
        for (int i = 0; i < bitString.length(); i += 8) {
            buffer.put((byte) Integer.parseInt(bitString.substring(i, i + 8), 2));
        }
        //TODO: I do not buffer.flip while creating the bytes because idk if we are gonna stich or wrap
        //TODO: This means returned buffer has its index at the last, can't be read from unless post processed
        buffer.flip();
        return buffer;
    }

    //TODO: Disclamier = Java uses signed bytes no matter what, thus we use &0xFF to consider them as unsigned again
    //TODO: Related to why casting used above but not the same issue


    /**
     * Converts the content of a ByteBuffer into a single binary string representation.
     * Reads bytes from the buffer's current position to its limit. The buffer is flipped internally
     * before reading to ensure it reads the intended data. Each byte is converted to an 8-bit
     * binary string (with leading zeros if necessary), treating bytes as unsigned using {@code & 0xFF}.
     *
     * @param buffer The ByteBuffer to read bytes from. The buffer's state (position, limit) will be modified.
     * @return A String containing the concatenated 8-bit binary representations of the bytes read from the buffer.
     */
    public static String bytes_to_bits(ByteBuffer buffer) {
        StringBuilder bitString = new StringBuilder();
        //ready to read, take index to 0 and limit to what already exist in the buffer
        //buffer.flip();

        //only checks position, recursive so builder is way more efficent
        while (buffer.hasRemaining()) {
            //sayi gelirse basini 8e tamamlanana kadar 0 koy, while loop until 8 bu s = 0 + s
            bitString.append(String.format("%8s", Integer.toBinaryString(buffer.get() & 0xFF))
                    .replace(' ', '0'));
        }
        //no need to implement to respective int, that's what parsing is for
        return bitString.toString();
    }


    /**
     * Main method for simple testing and demonstration of ByteBuffer and string conversions.
     * Creates a string, wraps it in a ByteBuffer, and prints the result of decoding it back.
     * Does not demonstrate the main packet parsing/making functionalities.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        String string = "Hello World";
        ByteBuffer buffer = ByteBuffer.wrap(string.getBytes(StandardCharsets.UTF_8));
        //        buffer.flip();
        String newString = new String(buffer.array(), StandardCharsets.UTF_8);
        System.out.println(newString);

    }
}