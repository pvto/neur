package neur.auto;

import neur.NeuralNetwork;
import neur.learning.LearnParams;

/**
 *
 * @author Paavo Toivanen
 */
public interface TopologySearchRoutine<T extends NeuralNetwork> {
    
    
    TopologyFinding<T> search(LearnParams templParams, NNSearchSpace searchSpace);
}
