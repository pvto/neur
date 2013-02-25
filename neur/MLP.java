package neur;

import java.io.Serializable;
import java.util.Arrays;
import neur.learning.LearnParams;
import neur.struct.ActivationFunction;
import neur.struct.LinearFunc;
import neur.struct.SigmoidalFunc;

public class MLP implements NeuralNetwork, Serializable {

    private static final long serialVersionUID = 20121016L;

    public Neuron[][]   layers;
    public Neuron[]     outv() {return layers[layers.length - 1];}    
    public float[][][] weights;

    
    @Override 
    public MLP newNetwork(LearnParams p) {return new MLP(
                    p.NNW_DIMS, 
                    ActivationFunction.Types.create(p.NNW_AFUNC, p.NNW_AFUNC_PARAMS));}



    
    private MLP(){}
    public  MLP(LearnParams p)
    { 
        this(p.NNW_DIMS, ActivationFunction.Types.get(p));
    }
    
    
    @Override
    public MLP copy()
    {
        MLP b = new MLP();
        b.layers = new Neuron[layers.length][];
        b.weights = new float[weights.length][][];
        for (int lr = 0; lr < layers.length; lr++)
        {
            b.layers[lr] = new Neuron[layers[lr].length];
            for (int j = 0; j < b.layers[lr].length; j++)
            {
//                if (layers[lr][j] != null)
                b.layers[lr][j] = layers[lr][j].copy();
            }
            if (lr < weights.length)
            {
                b.weights[lr] = new float[weights[lr].length][];
                for(int j = 0; j < b.weights[lr].length; j++)
                {
//                    if (weights[lr] != null && weights[lr][j] != null)
                    b.weights[lr][j] = Arrays.copyOf(weights[lr][j], weights[lr][j].length);
                }
            }
        }
        return b;
    }

    public MLP newNetwork()
    {
        return new MLP(getLayerSizes(), layers[1][0].ACT);
    }


//    public static class Perceptron extends Neuron implements Serializable 
//    {}


    public Neuron newNeuronForLayer(int layer, ActivationFunction afunc)
    {
        return new Neuron(layer == 0 ? new LinearFunc() : (afunc == null ? new SigmoidalFunc() : afunc));
    }



    public MLP(int[] layerSizes, ActivationFunction activationFunction)
    {
        layers = new Neuron[layerSizes.length][];
        for (int i = 0; i < layerSizes.length; i++)
        {
            layers[i] = new Neuron[layerSizes[i]];
        }
        
        // create units and a bias unit as last to each layer except for the output layer
        for (int layer = 0; layer < layerSizes.length; layer++)
        {
            for (int i = 0; i < layerSizes[layer]; i++)
            {
                addUnit(layer, i, activationFunction);
            }
            if (layer < layerSizes.length - 1)  // bias
                addUnit(layer, layerSizes[layer], activationFunction)
                        .netInput = 1f;
        }
        weights = new float[layerSizes.length - 1][][];
        for(int i = 0; i < layerSizes.length - 1; i++)
        {
            connect(i, layerSizes[i] + 1, layerSizes[i + 1], activationFunction);
        }
    }
    
    public void connect(int layer, int unitCount, int nextlayerCount, ActivationFunction activationFunction)
    {
        float[][] W = weights[layer] = new float[unitCount][];
        for(int i = 0; i < W.length; i++)
        {
            W[i] = new float[nextlayerCount];
            for (int j = 0; j < nextlayerCount; j++)
            {
                float glorot = preTrainInit(activationFunction, layer);
                W[i][j] = (float) (Math.random() - 0.5f) * glorot;
            }
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
    
    public Neuron addUnit(int layer, int unit, ActivationFunction activationFunction)
    {
        Neuron p = newNeuronForLayer(layer, activationFunction);
        setUnit(layer, unit, p);
        return p;
    }

    // Glorot & Bengio [2010]
    private float preTrainInit(ActivationFunction activationFunction, int layer)
    {
        float glorot = 1f;
        if (layers.length > 3 & layer > 0)
        {
            glorot = (float) (Math.sqrt(6.0) 
                    / Math.sqrt(layers[layer - 1].length + layers[layer].length)) * 2f;
            if (activationFunction instanceof SigmoidalFunc)
                glorot *= 4f;
        }
        return glorot;
    }

    /** feeds given data to the input layer and propagates */
    @Override
    public synchronized float[] feedf(float[] data)
    {
        Neuron[] Li = layers[0];
        // feed input
        // slot [n] contains bias, do not overwrite
        for (int i = 0; i < data.length; i++)
        {
            Li[i].netInput = data[i];
            Li[i].activation();
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
            float[][] feeds = weights[layer];
            for (int j = 0; j < feeds[0].length; j++)
            {
                float net = 0f;
                for (int i = 0; i < Li.length; i++)
                    net += Li[i].activation * feeds[i][j];
                Lj[j].netInput = net;
                Lj[j].activation();
            }
        }
    }

    public float[] getActivation()
    {
        Neuron[] out = outv();
        float[] ret = new float[out.length];
        for(int i = 0; i < out.length; i++)
        {
            ret[i] = out[i].activation;
        }
        return ret;
    }

    
    
    public int[] getLayerSizes()
    {
        int[] ret = new int[layers.length];
        for (int i = 0; i < ret.length - 1; i++)
            ret[i] = layers[i].length - 1;
        ret[layers.length - 1] = layers[layers.length - 1].length;
        return ret;
    }
    
}