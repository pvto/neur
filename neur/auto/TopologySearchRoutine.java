package neur.auto;

import neur.NeuralNetwork;
import neur.learning.LearnParams;

/**
 *
 * @author Paavo Toivanen
 */
public interface TopologySearchRoutine<T extends NeuralNetwork> {
    
    
    TopologyResult<T> search(LearnParams templParams, SearchSpace searchSpace);
}
