
package neur;

import java.io.Serializable;
import neur.learning.LearnParams;


public interface NeuralNetwork extends Serializable {

    
    /** Feeds a vector of data into the network and returns the network result in another vector.
     * If the network is of stabilising type, like Hopfield nets are, this routine should wait for the
     * network to stabilise.
     */
    float[]             feedf(float[] data);
    /** Returns a vector of output neurons for the network. In case of a MLP, it is the output layer; 
     * in case of other networks like a Bolzmann machine, a Hopfield net or a Kohonen's network, 
     * it may comprise of the whole network.
     */
    Neuron[]            outv();
    
    <T extends NeuralNetwork> 
     T                  newNetwork(LearnParams p);
    
    <T extends NeuralNetwork> 
     T                  copy();
    
}
