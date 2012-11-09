
package neur;


public interface NeuralNetwork {

    float[] feedf(float[] input);
    
    Neuron[][] getLayers();

    float[][][] getFeedWeights();
    
    
    public <T extends NeuralNetwork> T copy();
    
    public <T extends NeuralNetwork> T newNetwork(int[] dims, int AFUNC);
}
