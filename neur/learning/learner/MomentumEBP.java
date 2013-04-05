
package neur.learning.learner;

import java.io.Serializable;
import neur.MLP;
import neur.Neuron;
import static neur.util.Arrf.*;


public class MomentumEBP extends BackPropagation implements Serializable {

    public double 
            learningRate = 0d,
            ebpCounter = 0d,
            epochMSE = 0d,  
            prevEpochMSE = 0d
            ;
    public boolean useDynamicLearningRate = true;
    public double 
            learningRateChange = 0.99926d
            ;
    public boolean useDynamicMomentum = true;
    public double momentum = 0.25d,
            momentumChange = 0.99926d
            ;
    
    public int epoch = 0;
    float[][][] moments;
    { clear(); }
    
    @Override
    public void clear()
    { 
        moments = new float[7][][];
        epoch = 0;
    }
    
    protected void beforeBackpropagation(MLP nw, float[] errorTerm, Object[] params)
    {
        if (learningRate == 0f)
            learningRate = (float)params[0];
        ebpCounter++;
        epochMSE += sum( div(sqr(errorTerm), 2f) );
    }
    
    protected final void updateWeights(float[][] W, Neuron[] LRi, float[] Pj_tmp, int layer)
    {
        initLayer(layer, W);
        for (int i = 0; i < LRi.length; i++)
            for(int j = 0; j < W[i].length; j++)
            {
                float tmp = moments[layer][i][j];
                moments[layer][i][j] = W[i][j];
                W[i][j] += Pj_tmp[j] * LRi[i].activation
                        + momentum * (W[i][j] - tmp);
            }
        
    }
    
    public void initLayer(int layer, float[][] W)
    {
        if (!(moments[layer] == null || moments[layer].length != W.length || moments[layer][0].length != W[0].length) )
            return;
        moments[layer] = new float[W.length][];
        for (int i = 0; i < moments[layer].length; i++)
            moments[layer][i] = new float[W[i].length];
    }

    @Override
    public void finishEpoch(MLP n, Object[] params)
    {
        epochMSE = epochMSE / ebpCounter;
        if (epoch > 0)
        {
            adjustLearningRate();
            adjustMomentum();
        }
        prevEpochMSE = epochMSE;
        ebpCounter = 0f;
    }

    protected void adjustLearningRate()
    {
        double errorChange = prevEpochMSE - epochMSE;
        learningRate = learningRate + (errorChange*learningRateChange);

        learningRate = Math.min(learningRate, 0.9);
        learningRate = Math.max(learningRate, 0.1);
    }
    
    
    protected void adjustMomentum()
    {
        double errorChange = prevEpochMSE - epochMSE;
        momentum = momentum + (errorChange*momentumChange);

        momentum = Math.min(momentum, 0.9d);
        momentum = Math.max(momentum, 0.1d);
    }
    
}
