package neur;

import java.io.Serializable;
import java.util.Arrays;
import static neur.struct.ActivationFunction.Types.AFUNC_LINEAR;
import static neur.struct.ActivationFunction.Types.AFUNC_SIGMOID;

public class MLP implements NeuralNetwork, Serializable {

    private static final long serialVersionUID = 20121016L;

    @Override public Neuron[][] getLayers() {           return layers; }
    @Override public float[][][] getFeedWeights() {     return feedWeights; }
    @Override public <T extends NeuralNetwork> 
            T newNetwork(int[] dims, int AFUNC) {       return (T)new MLP(dims, AFUNC); }

    public Neuron[] IN() {                              return layers[0]; }
    public Neuron[] OUT() {                             return layers[layers.length - 1]; }

    
    public Neuron[][] layers;
    public float[][][] feedWeights;
    
    
    private MLP(){}
    
    @Override
    public MLP copy()
    {
        MLP b = new MLP();
        b.layers = new Neuron[layers.length][];
        b.feedWeights = new float[feedWeights.length][][];
        for (int lr = 0; lr < layers.length; lr++)
        {
            b.layers[lr] = new Neuron[layers[lr].length];
            for (int j = 0; j < b.layers[lr].length; j++)
            {
                b.layers[lr][j] = layers[lr][j].copy();
            }
            b.feedWeights[lr] = new float[feedWeights[lr].length][];
            for(int j = 0; j < b.feedWeights[lr].length; j++)
            {
                if (feedWeights[lr] != null && feedWeights[lr][j] != null)
                b.feedWeights[lr][j] = Arrays.copyOf(feedWeights[lr][j], feedWeights[lr][j].length);
            }
        }
        return b;
    }

    public MLP newNetwork()
    {
        return new MLP(getLayerSizes(), layers[1][0].activationFunction);
    }


//    public static class Perceptron extends Neuron implements Serializable 
//    {}


    public Neuron newNeuronForLayer(int layer, int afunc)
    {
        return new Neuron(layer == 0 ? AFUNC_LINEAR : (afunc < 0 ? AFUNC_SIGMOID : afunc));
    }


    
    public MLP(int[] layerSizes, int activationFunction)
    {
        layers = new Neuron[layerSizes.length][];
        for (int i = 0; i < layerSizes.length; i++)
        {
            layers[i] = new Neuron[layerSizes[i]];
            // add bias
            Neuron p = newNeuronForLayer(i, activationFunction);
            p.netInput = 1f;
            setUnit(i,0,p);
        }
        
        feedWeights = new float[layerSizes.length][][];
        int layer = 0;
        for (int s : layerSizes)
        {
            int weightCount = 
                    (layer < layerSizes.length -1 ? layerSizes[layer + 1] : 1);
            feedWeights[layer] = new float[weightCount][];
            for (int i = 1; i <= s; i++)
            {
                addUnit(layer, i, activationFunction);
            }
            layer++;
        }
    }


    
    public void setUnit(int layer, int unit, Neuron p)
    {
        Neuron[] L = layers[layer];
        if (L.length <= unit)
        {
            Neuron[] M = new Neuron[unit + 1];
            System.arraycopy(L, 0, M, 0, L.length);
            L = layers[layer] = M;
        }
        L[unit] = p;        
    }
    
    public Neuron addUnit(int layer, int unit, int activationFunction)
    {
        Neuron p = newNeuronForLayer(layer, activationFunction);
        setUnit(layer, unit, p);
        // add weights for connections from existing perceptrons in previous layer to this unit
        if (layer > 0)
        {
            float[][] LL = feedWeights[layer - 1];
            float[] L = new float[layers[layer - 1].length];
            LL[unit-1] = L;
            for (int i = 0; i < L.length; i++)
            {
                L[i] = (float) Math.random() - 0.5f;
            }
        }
//        // add weights for connections from this layer to existing perceptrons in next layer
//        if (layer < layers.length - 1)
//        {
//            // add weights for connections omitting bias at [0]
//            for (int i = 0; i < feedWeights[layer].length; i++)
//            {
//                float[] L = feedWeights[layer][i];
//                float[] M = new float[L.length + 1];
//                System.arraycopy(L, 0, M, 0, L.length);
//                M[L.length] = (float)Math.random() - 0.5f;
//                feedWeights[layer][i] = M;
//            }
//        }
        return p;
    }


    /** feeds given data to the input layer and propagates */
    public synchronized float[] feedf(float[] data)
    {
        Neuron[] Li = IN();
        // feed input
        // slot 0 contains bias, do not overwrite
        for (int i = 0; i < data.length; i++)
        {
            Li[i+1].netInput = data[i];
            Li[i+1].activation();
        }
        propagate();
        return getActivation();
    }

    /** prapagate activation from the input layer onwards towards the output layer */
    public void propagate()
    {
        for (int layer = 0; layer < layers.length - 1; layer++)
        {
            Neuron[] Li = layers[layer];
            Neuron[] Lj = layers[layer + 1];
            float[][] feeds = feedWeights[layer];
            for (int j = 1; j < Lj.length; j++)
            {
                float sum = 0f;
                for (int i = 0; i < feeds[j - 1].length; i++)
                {
                    sum += Li[i].activation * feeds[j - 1][i];
                }
                Lj[j].netInput = sum;
                Lj[j].activation();
            }
        }
    }

    public float[] getActivation()
    {
        Neuron[] out = OUT();
        float[] ret = new float[out.length - 1];
        for(int i = 1; i < out.length; i++)
        {
            ret[i-1] = out[i].activation;
        }
        return ret;
    }

    
    
    public int[] getLayerSizes()
    {
        int[] ret = new int[layers.length];
        for (int i = 0; i < ret.length; i++)
            ret[i] = layers[i].length - 1;
        return ret;
    }
    
}