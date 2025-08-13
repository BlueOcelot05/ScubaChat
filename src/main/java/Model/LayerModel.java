package Model;

import Model.Exceptions.NetworkException;

/**
 * Top-Level interface to generalize all layers in a way that they can communicate easily.
 */
public interface LayerModel {

    /**
     * The method used to communicate with the layer.
     * @param header the message that it's supposed to process.
     * @throws NetworkException when something goes wrong with either the processing of the header
     *          or the network
     */
    void ReceiveHeader(Packet header) throws NetworkException;
}
