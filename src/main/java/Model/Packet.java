package Model;


/**
 * Top-Level class for all the types of headers. All headers should inherit from this
 * to allow for proper communication in between the systems as well as implement their
 * layer specific characters.
 */
public interface Packet {

    /**
     * @return the header before this one or the payload (as in the data from the app layer)
     */
    Packet getUpperHeader();

    /**
     * Recursively calls the same function from its upper header until
     * getting the address specified by the user.
     * @return the address of the destination
     */
    int getDestination();
}