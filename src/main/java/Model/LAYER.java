package Model;

/**
 * Enum representing the layer. Should make it easier to compare what layer something is related to.
 * The layer can  {@link LAYER#LINK LINK} | {@link LAYER#NETWORK NETWORK} | {@link LAYER#TRANSPORT TRANSPORT} | {@link LAYER#APP APP}
 */
public enum LAYER {
    LINK, NETWORK, TRANSPORT, APP
}