
package neur;

import java.io.Serializable;
import neur.learning.LearnParams;


public interface NeuralNetwork extends Serializable {

    
    
    float[]             feedf(float[] data);
    Neuron[]            outa();
    
    <T extends NeuralNetwork> 
     T                  newNetwork(LearnParams p);
    
    <T extends NeuralNetwork> 
     T                  copy();
    
}
