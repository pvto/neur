
package neur;

import neur.learning.LearnParams;

/**
 *
 * @author paavoto
 */
public class BoltzmannMachine implements NeuralNetwork {

    public Neuron[] neurons;
    public double[][] weights;
    public int[] 
            in,
            out
            ;
    
    public BoltzmannMachine(int size)
    {
        neurons = new Neuron[size];
        weights = new double[size][size];
        for(int i = 0; i < size; i++)
        {
            neurons[i] = newNeuron();
        }
        for(int i = 0; i < size; i++)
        {
            for (int j = 0; j < neurons.length; j++)
            {
                weights[i][j] = newWeight();
            }
        }
    }
    public Neuron newNeuron()   {       return new Neuron(); }
    public double newWeight()   {       return Math.random() - 0.5; }
    
    @Override
    public float[] feedf(float[] data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Neuron[] outv() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends NeuralNetwork> T newNetwork(LearnParams p) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends NeuralNetwork> T copy() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
