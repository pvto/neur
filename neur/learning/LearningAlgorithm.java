
package neur.learning;

import neur.NeuralNetwork;


public interface LearningAlgorithm<T extends NeuralNetwork> {

    /**
     * 
     * @param n network ready to be teached
     * @param outputErrorTerms list of error terms corresponding to network
     * output neurons
     */
    void learn(T n, float[] outputErrorTerms, Object[] params);
    
    void finishEpoch(T n, Object[] params);
    
    void clear();
}
