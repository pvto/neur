package neur.auto;

import neur.NeuralNetwork;
import neur.learning.LearningAlgorithm;
import neur.learning.LearnParams;

/**
 *
 * @author Paavo Toivanen
 */
public interface TopologySearch<T extends NeuralNetwork> {
    
    
    TopologySearchResult<T> search(LearnParams templParams);
}
