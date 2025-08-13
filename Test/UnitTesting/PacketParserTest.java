package UnitTesting;

import Model.Exceptions.PayloadException;
import Model.Exceptions.RoutingException; // Assuming this exists
import Model.PacketParser;
import NetworkLayer.AddressHeader;
import NetworkLayer.DA_Header;
import NetworkLayer.DV_Header;
import TransportLayer.TCPHeader; // Assuming this exists or is mocked if needed
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PacketParserTest {

    // --- Test Data Setup ---
    TCPHeader sampleTcpHeader;
    DV_Header sampleDvHeader;
    AddressHeader sampleAddressHeaderTCP;
    AddressHeader sampleAddressHeaderDV;
    AddressHeader sampleAddressHeaderDV_ID; // For destination = 7 test
    DA_Header sampleDaHeader;

    Map<Integer, Integer> dvMap;

    @BeforeEach
    void setUp() {
        // Sample TCP Header (Seq=10, Ack=20, ACK=true, FIN=false, PayloadLen=0, No actual payload data)
        // Bit representation fields: seq(12), ack(12), A(1), F(1), len(6) = 32 bits = 4 bytes
        sampleTcpHeader = new TCPHeader(10, 20, true, false, "", 0, 3); // PayloadLen=0, ignore "" msg

        // Sample DV Header Data
        dvMap = new HashMap<>();
        dvMap.put(1, 2); // Dest 1, Cost 2
        dvMap.put(3, 5); // Dest 3, Cost 5

        // --- FIX 1: Convert map to arrays for DV_Header constructor ---
        int numEntries = dvMap.size();
        int[] destinations = new int[numEntries];
        int[] costs = new int[numEntries];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : dvMap.entrySet()) {
            destinations[i] = entry.getKey();
            costs[i] = entry.getValue();
            i++;
        }
        // Use the array constructor
        sampleDvHeader = new DV_Header(destinations, costs);
        // --- End FIX 1 ---

        // Sample Address Header wrapping TCP
        // Fields: nextProto(4)=0, ttl(4)=15, payLen(8)=4 (TCP header size), src(4)=1, dest(4)=2, nextHop(8)=3, id(0)
        sampleAddressHeaderTCP = new AddressHeader(0, 15, 4, 1, 2, 3, 0, sampleTcpHeader);

        // Sample Address Header wrapping DV
        // Fields: nextProto(4)=1 (assuming DV proto=1), ttl(4)=10, payLen(8)=2 (2 DV entries*1 byte), src(4)=4, dest(4)=5, nextHop(8)=6, id(0)
        // Note: payLen should match the size of the *serialized* DV header (number of entries)
        sampleAddressHeaderDV = new AddressHeader(1, 10, dvMap.size(), 4, 5, 6, 0, sampleDvHeader); // payLen=2 entries * 1 byte/entry = 2

        // Sample Address Header with Destination 7 (using ID) wrapping DV
        // Fields: nextProto(4)=1, ttl(4)=8, payLen(8)=2, src(4)=1, dest(4)=7, nextHop(0), id(8)=99
        sampleAddressHeaderDV_ID = new AddressHeader(1, 8, dvMap.size(), 1, 7, 0, 99, sampleDvHeader); // payLen=2

        // Sample DA Header
        // Fields: proto(3)=2, conflict(1)=false, seq(8)=50, addr(4)=9
        // Total 16 bits = 2 bytes
        sampleDaHeader = new DA_Header(2, false, 50, 9);
    }

    // --- Utility Method Tests ---

    @Test
    @DisplayName("int_to_bit converts correctly with padding")
    void testIntToBit() {
        assertEquals("0101", PacketParser.int_to_bit(5, 4));
        assertEquals("00001100", PacketParser.int_to_bit(12, 8));
        assertEquals("0000", PacketParser.int_to_bit(0, 4));
        assertEquals("1111", PacketParser.int_to_bit(15, 4));
        // Test potential edge case if format specifier width was smaller than needed (though unlikely with %s)
        assertEquals("10000", PacketParser.int_to_bit(16, 5));
        // Ensure leading zeros are added correctly
        assertEquals("0000000000001010", PacketParser.int_to_bit(10, 16));
    }

    @Test
    @DisplayName("bit_to_int converts correctly")
    void testBitToInt() {
        assertEquals(5, PacketParser.bit_to_int("0101"));
        assertEquals(12, PacketParser.bit_to_int("1100"));
        assertEquals(0, PacketParser.bit_to_int("0000"));
        assertEquals(15, PacketParser.bit_to_int("1111"));
        assertEquals(255, PacketParser.bit_to_int("11111111"));
    }

    @Test
    @DisplayName("bits_to_bytes converts valid bit string")
    void testBitsToBytes_Valid() throws PayloadException {
        String bits = "0100100001100101"; // "He"
        ByteBuffer expected = ByteBuffer.wrap(new byte[]{(byte) 'H', (byte) 'e'});
        ByteBuffer actual = PacketParser.bits_to_bytes(bits);
        assertEquals(expected, actual);
        // Test with leading zeros preserved by byte conversion
        String bitsZero = "0000000100000010"; // 1, 2
        ByteBuffer expectedZero = ByteBuffer.wrap(new byte[]{1, 2});
        ByteBuffer actualZero = PacketParser.bits_to_bytes(bitsZero);
        assertEquals(expectedZero, actualZero);
    }

    @Test
    @DisplayName("bits_to_bytes throws exception for invalid length")
    void testBitsToBytes_InvalidLength() {
        String invalidBits = "1010101"; // Length 7
        PayloadException ex = assertThrows(PayloadException.class, () -> PacketParser.bits_to_bytes(invalidBits));
        assertTrue(ex.getMessage().contains("incomplete bits to bytes"));
    }

    @Test
    @DisplayName("bytes_to_bits converts buffer content")
    void testBytesToBits() {
        byte[] data = {'H', 'i', '!'}; // 01001000 01101001 00100001
        String expectedBits = "010010000110100100100001";

        // Test with a buffer ready for reading
        ByteBuffer bufferRead = ByteBuffer.wrap(data);
        assertEquals(expectedBits, PacketParser.bytes_to_bits(bufferRead));
        assertEquals(data.length, bufferRead.position(), "Buffer position should be at the end after reading");

        // Test with a buffer partially filled and then flipped
        ByteBuffer bufferPartial = ByteBuffer.allocate(10);
        bufferPartial.put(data);
        bufferPartial.flip(); // Prepare for reading
        assertEquals(expectedBits, PacketParser.bytes_to_bits(bufferPartial));
        assertEquals(data.length, bufferPartial.position(), "Buffer position should be at the limit after reading");

        // Test with an empty buffer
        ByteBuffer emptyBuffer = ByteBuffer.allocate(5);
        emptyBuffer.flip();
        assertEquals("", PacketParser.bytes_to_bits(emptyBuffer));

    }

    // --- Individual Header Make/Parse Tests ---

    @Test
    @DisplayName("makeTCPHeader and parseTCPHeader are consistent")
    void testMakeParseTCPHeader() throws PayloadException {
        // 1. Make Bytes
        ByteBuffer tcpBytes = PacketParser.makeTCPHeader(sampleTcpHeader);
        assertEquals(4, tcpBytes.limit(), "TCP Header should be 4 bytes");

        // 2. Convert bytes to bits (for parsing)
        tcpBytes.rewind(); // Reset position for reading
        String tcpBits = PacketParser.bytes_to_bits(tcpBytes);
        assertEquals(32, tcpBits.length(), "TCP Header bit string should be 32 bits");

        // 3. Parse Bits (add dummy payload bits as parseTCPHeader expects them)
        String bitsWithDummyPayload = tcpBits + "11110000"; // Dummy payload bits
        TCPHeader parsedHeader = PacketParser.parseTCPHeader(bitsWithDummyPayload, 3);

        // 4. Assert Field Equality (excluding payload 'msg' field)
        assertEquals(sampleTcpHeader.getSequence(), parsedHeader.getSequence());
        assertEquals(sampleTcpHeader.getAcknowledgment(), parsedHeader.getAcknowledgment());
        assertEquals(sampleTcpHeader.isAck(), parsedHeader.isAck());
        assertEquals(sampleTcpHeader.isFin(), parsedHeader.isFin());
        assertEquals(sampleTcpHeader.getPayloadLength(), parsedHeader.getPayloadLength());
        // We explicitly DO NOT compare the 'msg' field here due to make/parse asymmetry
        assertEquals("11110000", parsedHeader.getMessage(), "Parsed message should contain remaining bits");

        // 5. Verify made bytes directly (Optional but good)
        // Expected: 000000001010 (10) | 000000010100 (20) | 1 | 0 | 000000 (0)
        String expectedTcpBits = "00000000101000000001010010000000";
        tcpBytes.rewind();
        assertEquals(expectedTcpBits, PacketParser.bytes_to_bits(tcpBytes));
    }

    @Test
    @DisplayName("makeDAHeader and parseDAHeader are consistent")
    void testMakeParseDAHeader() throws PayloadException {
        // 1. Make Bytes
        ByteBuffer daBytes = PacketParser.makeDAHeader(sampleDaHeader);
        assertEquals(2, daBytes.limit(), "DA Header should be 2 bytes");

        // 2. Convert bytes to bits
        daBytes.rewind();
        String daBits = PacketParser.bytes_to_bits(daBytes);
        assertEquals(16, daBits.length(), "DA Header bit string should be 16 bits");

        // 3. Parse Bits
        DA_Header parsedHeader = PacketParser.parseDAHeader(daBits);

        // 4. Assert Equality using equals() (assuming it's implemented correctly)
        // assertEquals(sampleDaHeader, parsedHeader); // Requires DA_Header.equals()

        // 5. Or Assert Field Equality manually
        assertEquals(sampleDaHeader.getProtocol(), parsedHeader.getProtocol());
        assertEquals(sampleDaHeader.isConflict(), parsedHeader.isConflict());
        assertEquals(sampleDaHeader.getSequence(), parsedHeader.getSequence());
        assertEquals(sampleDaHeader.getAddress(), parsedHeader.getAddress());

        // 6. Verify made bytes directly
        // Expected: 010(P=2) | 0(C=f) | 00110010(S=50) | 1001(A=9)
        String expectedDaBits = "0100001100101001";
        daBytes.rewind();
        assertEquals(expectedDaBits, PacketParser.bytes_to_bits(daBytes));
    }

    @Test
    @DisplayName("makeDVHeader and parseDVHeader are consistent")
    void testMakeParseDVHeader() throws RoutingException, PayloadException {
        // --- FIX 2: Use array constructor for empty/bad DV_Headers ---
        // Check empty map exception
        DV_Header emptyDv = new DV_Header(new int[0], new int[0]); // Use empty arrays
        assertThrows(RoutingException.class, () -> PacketParser.makeDVHeader(emptyDv),
                "makeDVHeader should throw RoutingException for empty DV");

        // Check value too high exception
        DV_Header badDv = new DV_Header(new int[]{1}, new int[]{16}); // Use arrays for constructor
        assertThrows(RoutingException.class, () -> PacketParser.makeDVHeader(badDv),
                "makeDVHeader should throw RoutingException for cost > 15");

        DV_Header badDvDest = new DV_Header(new int[]{16}, new int[]{5}); // Use arrays for constructor
        assertThrows(RoutingException.class, () -> PacketParser.makeDVHeader(badDvDest),
                "makeDVHeader should throw RoutingException for destination > 15");
        // --- End FIX 2 ---


        // --- Test with valid sampleDvHeader ---

        // 1. Make Bytes
        // sampleDvHeader was created correctly in setUp using arrays
        ByteBuffer dvBytes = PacketParser.makeDVHeader(sampleDvHeader);
        // Size should be number of entries (bytes) as each entry is 1 byte
        assertEquals(dvMap.size(), dvBytes.limit(), "DV Header size should match number of entries"); // 2 entries * 1 byte/entry = 2 bytes

        // 2. Convert bytes to bits
        dvBytes.rewind();
        String dvBits = PacketParser.bytes_to_bits(dvBytes);
        assertEquals(dvMap.size() * 8, dvBits.length(), "DV Header bit string length incorrect"); // 16 bits

        // 3. Parse Bits -> This will create a DV_Header using the (dest[], cost[]) constructor
        DV_Header parsedHeader = PacketParser.parseDVHeader(dvBits);

        // 4. Assert Equality - Compare the internal maps
        // Retrieve the map from the parsed header
        Map<Integer, Integer> parsedMap = parsedHeader.getDistanceVector();
        assertNotNull(parsedMap, "Parsed DV header map should not be null");
        assertEquals(dvMap.size(), parsedMap.size(), "Parsed map size should match original map size");
        assertEquals(dvMap, parsedMap, "Parsed DV map content mismatch");

        // Optional: If DV_Header implemented equals based on content:
        // assertEquals(sampleDvHeader, parsedHeader); // Requires DV_Header.equals() to compare maps

        // 5. Verify made bytes directly
        // Expected: 0001(D=1) 0010(C=2) | 0011(D=3) 0101(C=5) (Order might vary based on HashMap iteration)
        // Let's build the expected string dynamically based on the original map for robustness
        String expectedDvBitsEntry1 = PacketParser.int_to_bit(1, 4) + PacketParser.int_to_bit(2, 4); // "00010010"
        String expectedDvBitsEntry2 = PacketParser.int_to_bit(3, 4) + PacketParser.int_to_bit(5, 4); // "00110101"
        dvBytes.rewind();
        String actualDvBits = PacketParser.bytes_to_bits(dvBytes);
        // Check if the actual bits contain both expected entry bit patterns, regardless of order
        assertTrue(
                (expectedDvBitsEntry1 + expectedDvBitsEntry2).equals(actualDvBits) ||
                        (expectedDvBitsEntry2 + expectedDvBitsEntry1).equals(actualDvBits),
                "Made DV bits do not match expected entries. Expected E1=" + expectedDvBitsEntry1 + ", E2=" + expectedDvBitsEntry2 + ", Got=" + actualDvBits
        );
    }

    @Test
    @DisplayName("makeADHeader and parseADHeader are consistent (TCP case)")
    void testMakeParseADHeader_TCP() throws PayloadException {
        // 1. Make Bytes (Address Header only)
        ByteBuffer adBytes = PacketParser.makeADHeader(sampleAddressHeaderTCP);
        assertEquals(4, adBytes.limit(), "Address Header should be 4 bytes");

        // 2. Convert bytes to bits
        adBytes.rewind();
        String adBits = PacketParser.bytes_to_bits(adBytes);
        assertEquals(32, adBits.length(), "Address Header bit string should be 32 bits");

        // 3. Parse Bits (pass the original upper header object)
        // parseADHeader only needs the upper header for the final AddressHeader object construction,
        // it doesn't use it during the bit parsing itself.
        AddressHeader parsedHeader = PacketParser.parseADHeader(adBits, sampleTcpHeader);

        // 4. Assert Equality using equals() (assuming deep equals including upper header)
        // assertEquals(sampleAddressHeaderTCP, parsedHeader); // Requires AddressHeader.equals()

        // 5. Or Assert Field Equality manually (Safer if equals() is uncertain)
        assertEquals(sampleAddressHeaderTCP.getNext_Protocol(), parsedHeader.getNext_Protocol());
        assertEquals(sampleAddressHeaderTCP.getTTL(), parsedHeader.getTTL());
        assertEquals(sampleAddressHeaderTCP.getPayload_Length(), parsedHeader.getPayload_Length());
        assertEquals(sampleAddressHeaderTCP.getSource(), parsedHeader.getSource());
        assertEquals(sampleAddressHeaderTCP.getDestination(), parsedHeader.getDestination());
        assertEquals(sampleAddressHeaderTCP.getNext_Hop(), parsedHeader.getNext_Hop());
        assertEquals(sampleAddressHeaderTCP.getID(), parsedHeader.getID());
        assertEquals(sampleAddressHeaderTCP.getUpperHeader(), parsedHeader.getUpperHeader(), "Upper header mismatch");

        // 6. Verify made bytes directly
        // Expected: 0000(P=0) | 1111(T=15) | 00000100(L=4) | 0001(S=1) | 0010(D=2) | 00000011(NH=3)
        String expectedAdBits = "00001111000001000001001000000011";
        adBytes.rewind();
        assertEquals(expectedAdBits, PacketParser.bytes_to_bits(adBytes));
    }

    @Test
    @DisplayName("makeADHeader and parseADHeader are consistent (DV case, Dest=7 ID)")
    void testMakeParseADHeader_DV_ID() throws PayloadException {
        // Test case where Destination is 7, so ID field is used instead of Next Hop
        // sampleAddressHeaderDV_ID uses sampleDvHeader which was setup correctly

        // 1. Make Bytes (Address Header only)
        ByteBuffer adBytes = PacketParser.makeADHeader(sampleAddressHeaderDV_ID);
        assertEquals(4, adBytes.limit());

        // 2. Convert bytes to bits
        adBytes.rewind();
        String adBits = PacketParser.bytes_to_bits(adBytes);
        assertEquals(32, adBits.length());

        // 3. Parse Bits (pass the original upper header object)
        AddressHeader parsedHeader = PacketParser.parseADHeader(adBits, sampleDvHeader); // Pass associated DV header

        // 4. Assert Field Equality manually
        assertEquals(sampleAddressHeaderDV_ID.getNext_Protocol(), parsedHeader.getNext_Protocol()); // 1
        assertEquals(sampleAddressHeaderDV_ID.getTTL(), parsedHeader.getTTL());                     // 8
        assertEquals(sampleAddressHeaderDV_ID.getPayload_Length(), parsedHeader.getPayload_Length()); // 2 (DV size)
        assertEquals(sampleAddressHeaderDV_ID.getSource(), parsedHeader.getSource());               // 1
        assertEquals(sampleAddressHeaderDV_ID.getDestination(), parsedHeader.getDestination());     // 7
        assertEquals(0, parsedHeader.getNext_Hop(), "Next Hop should be 0 when Dest=7");           // 0 (parsed)
        assertEquals(sampleAddressHeaderDV_ID.getID(), parsedHeader.getID());                       // 99
        assertEquals(sampleAddressHeaderDV_ID.getUpperHeader(), parsedHeader.getUpperHeader(), "Upper header mismatch");

        // 5. Verify made bytes directly
        // Expected: 0001(P=1) | 1000(T=8) | 00000010(L=2) | 0001(S=1) | 0111(D=7) | 01100011(ID=99)
        String expectedAdBits = "00011000000000100001011101100011";
        adBytes.rewind();
        assertEquals(expectedAdBits, PacketParser.bytes_to_bits(adBytes));
    }


    // --- Combined Packet Make/Parse Tests ---

    @Test
    @DisplayName("maker_long and parser_long work for Address+TCP")
    void testMakeParseLong_TCP() throws PayloadException, RoutingException {
        // 1. Make combined packet bytes using maker_long
        ByteBuffer combinedBytes = PacketParser.maker_long(sampleAddressHeaderTCP);
        // Expected size: 4 (Addr) + 4 (TCP) = 8 bytes
        // maker_long allocates 32 bytes, which seems excessive? Let's check parser logic.
        // Parser logic reads 32 bits (4 bytes) for Address, then reads based on payload length.
        // Let's recalculate expected size based on ACTUAL headers:
        int expectedSize = 4 + 4; // AD Header (4) + TCP Header (4)
        // Let's re-evaluate maker_long's allocation based on the provided code:
        // public static ByteBuffer maker_long(AddressHeader address_header) throws PayloadException, RoutingException {
        //     ByteBuffer adBytes = makeADHeader(address_header); // 4 bytes
        //     ByteBuffer upperBytes;
        //     if (address_header.getNext_Protocol() == 0) {
        //         upperBytes = makeTCPHeader((TCPHeader) address_header.getUpperHeader()); // 4 bytes
        //     } else {
        //         upperBytes = makeDVHeader((DV_Header) address_header.getUpperHeader()); // N bytes
        //     }
        //     // Allocate exact size needed
        //     ByteBuffer packet = ByteBuffer.allocate(adBytes.limit() + upperBytes.limit());
        //     packet.put(adBytes);
        //     packet.put(upperBytes);
        //     packet.flip();
        //     return packet;
        // }
        // The above is how maker_long SHOULD work. The provided code is:
        // public static ByteBuffer maker_long(AddressHeader address_header) throws PayloadException, RoutingException {
        //    ByteBuffer packet = ByteBuffer.allocate(32); // <-- FIXED 32 BYTE ALLOCATION!
        //    packet.put(makeADHeader(address_header)); // puts 4 bytes
        //    if (address_header.getNext_Protocol() == 0) {
        //        packet.put(makeTCPHeader((TCPHeader) address_header.getUpperHeader())); // puts 4 bytes
        //    } else {
        //        packet.put(makeDVHeader((DV_Header) address_header.getUpperHeader())); // puts N bytes
        //    }
        //    packet.flip(); // limit will be 4+4=8, capacity 32
        //    return packet;
        // }
        // So the limit WILL be correct (8), but capacity is fixed. Let's test against limit.

        assertEquals(expectedSize, combinedBytes.limit(), "Combined Address+TCP packet LIMIT should be " + expectedSize + " bytes");


        // 2. Parse combined packet bytes using parser_long
        combinedBytes.rewind(); // Prepare for reading by parser_long
        AddressHeader parsedPacket = PacketParser.parser_long(combinedBytes);

        // 3. Assert equality (deep check)
        assertNotNull(parsedPacket);
        // assertEquals(sampleAddressHeaderTCP, parsedPacket); // If equals is deep and reliable

        // Manual Field Comparison (safer)
        assertEquals(sampleAddressHeaderTCP.getNext_Protocol(), parsedPacket.getNext_Protocol());
        assertEquals(sampleAddressHeaderTCP.getTTL(), parsedPacket.getTTL());
        assertEquals(sampleAddressHeaderTCP.getPayload_Length(), parsedPacket.getPayload_Length()); // Should be 4 (TCP header size)
        assertEquals(sampleAddressHeaderTCP.getSource(), parsedPacket.getSource());
        assertEquals(sampleAddressHeaderTCP.getDestination(), parsedPacket.getDestination());
        assertEquals(sampleAddressHeaderTCP.getNext_Hop(), parsedPacket.getNext_Hop());
        assertEquals(sampleAddressHeaderTCP.getID(), parsedPacket.getID());

        assertNotNull(parsedPacket.getUpperHeader());
        assertInstanceOf(TCPHeader.class, parsedPacket.getUpperHeader(), "Upper header should be TCPHeader");
        TCPHeader parsedTcp = (TCPHeader) parsedPacket.getUpperHeader();
        assertEquals(sampleTcpHeader.getSequence(), parsedTcp.getSequence());
        assertEquals(sampleTcpHeader.getAcknowledgment(), parsedTcp.getAcknowledgment());
        assertEquals(sampleTcpHeader.isAck(), parsedTcp.isAck());
        assertEquals(sampleTcpHeader.isFin(), parsedTcp.isFin());
        assertEquals(sampleTcpHeader.getPayloadLength(), parsedTcp.getPayloadLength()); // This is payload *within* TCP (0)
        // Check that the payload msg field in the parsed TCP header is empty, as parser_long doesn't handle app payload beyond TCP header
        assertEquals("", parsedTcp.getMessage(), "Parsed TCP msg should be empty");

        // 4. Verify bytes by concatenating individual parts (optional sanity check)
        ByteBuffer adBytes = PacketParser.makeADHeader(sampleAddressHeaderTCP);
        ByteBuffer tcpBytes = PacketParser.makeTCPHeader(sampleTcpHeader);
        ByteBuffer expectedManual = ByteBuffer.allocate(adBytes.limit() + tcpBytes.limit());
        adBytes.rewind(); // ensure reading from start
        tcpBytes.rewind(); // ensure reading from start
        expectedManual.put(adBytes);
        expectedManual.put(tcpBytes);
        expectedManual.flip();

        combinedBytes.rewind();
        assertEquals(expectedManual, combinedBytes, "Maker_long output doesn't match manual concatenation");

    }

    @Test
    @DisplayName("maker_long and parser_long work for Address+DV")
    void testMakeParseLong_DV() throws PayloadException, RoutingException {
        // sampleAddressHeaderDV uses sampleDvHeader (2 entries)

        // 1. Make combined packet bytes using maker_long
        ByteBuffer combinedBytes = PacketParser.maker_long(sampleAddressHeaderDV);
        int dvHeaderSize = dvMap.size(); // Each entry is 1 byte
        int expectedSize = 4 + dvHeaderSize; // 4(Addr) + N*1(DV entries)
        // Again, check maker_long's behavior - it allocates 32, but puts 4 + dvHeaderSize bytes.
        assertEquals(expectedSize, combinedBytes.limit(), "Combined Address+DV packet LIMIT should be " + expectedSize + " bytes"); // 4+2=6 bytes

        // 2. Parse combined packet bytes using parser_long
        combinedBytes.rewind();
        AddressHeader parsedPacket = PacketParser.parser_long(combinedBytes); // parser_long reads AD, checks proto, parses DV

        // 3. Assert equality (deep check)
        assertNotNull(parsedPacket);

        // Manual Field Comparison for Address Header
        assertEquals(sampleAddressHeaderDV.getNext_Protocol(), parsedPacket.getNext_Protocol()); // 1
        assertEquals(sampleAddressHeaderDV.getTTL(), parsedPacket.getTTL());                     // 10
        // NOTE: Payload Length in Address Header refers to the *next* header's size in BYTES
        assertEquals(dvHeaderSize, parsedPacket.getPayload_Length(), "AD Payload length should match DV size in bytes"); // Should be 2
        assertEquals(sampleAddressHeaderDV.getSource(), parsedPacket.getSource());               // 4
        assertEquals(sampleAddressHeaderDV.getDestination(), parsedPacket.getDestination());     // 5
        assertEquals(sampleAddressHeaderDV.getNext_Hop(), parsedPacket.getNext_Hop());           // 6
        assertEquals(sampleAddressHeaderDV.getID(), parsedPacket.getID());                       // 0

        // Manual Field Comparison for DV Header
        assertNotNull(parsedPacket.getUpperHeader());
        assertInstanceOf(DV_Header.class, parsedPacket.getUpperHeader(), "Upper header should be DV_Header");

        // Compare DV content by getting the map from the parsed header
        DV_Header parsedDv = (DV_Header) parsedPacket.getUpperHeader();
        Map<Integer, Integer> parsedMap = parsedDv.getDistanceVector();
        assertEquals(dvMap, parsedMap, "Parsed DV map content mismatch in combined packet");


        // 4. Verify bytes by concatenating individual parts
        ByteBuffer adBytes = PacketParser.makeADHeader(sampleAddressHeaderDV);
        ByteBuffer dvBytes = PacketParser.makeDVHeader(sampleDvHeader);
        ByteBuffer expectedManual = ByteBuffer.allocate(adBytes.limit() + dvBytes.limit());
        adBytes.rewind(); // ensure reading from start
        dvBytes.rewind(); // ensure reading from start
        expectedManual.put(adBytes);
        expectedManual.put(dvBytes);
        expectedManual.flip();

        combinedBytes.rewind();
        assertEquals(expectedManual, combinedBytes, "Maker_long output doesn't match manual concatenation for DV");
    }
}