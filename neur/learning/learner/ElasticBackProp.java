
package neur.learning.learner;

import java.io.Serializable;
import neur.MLP;
import neur.Neuron;
import neur.learning.LearningAlgorithm;

/** An implementation of elastic error back propagation algorithm.
 * Adapted from the neuroph project, http://neuroph.svn.sourceforge.net/viewvc/neuroph/trunk/neuroph-2.7/Core/src/main/java/org/neuroph/nnet/learning/ResilientPropagation.java?revision=1338&content-type=text%2Fplain .
 *
 * @author Paavo Toivanen
 */
public final class ElasticBackProp extends BackPropagation implements Serializable {

    public float decreaseFactor = 0.5f;
    public float increaseFactor = 1.2f;
    public float initialDelta = 0.1f;
    public float maxDelta = 1f;
    public float minDelta = 1e-6f;

    Retained[][][] data;
    { clear(); }
    
    static final class Retained implements Serializable  // for a weight
    {  float gradient = 1f, prevGrad = 0f, prevWgCh = 0f, prevDelta = 0f;  }
    
    @Override
    public void clear() { data = new Retained[7][][]; }

    
    
    protected final void updateWeights(float[][] W, Neuron[] LRi, float[] Pj_tmp, int layer)
    {
        initLayer(layer, W);
        for (int i = 0; i < LRi.length; i++)
            for(int j = 0; j < W[i].length; j++)
                data[layer][i][j].gradient += Pj_tmp[j] * LRi[i].activation;

    }

    @Override
    public void finishEpoch(MLP n, Object[] params)
    {
        for (int lr = n.weights.length - 2; lr >= 0; lr--)
            for (int i = 0; i < n.layers[lr].length; i++)
                for(int j = 0; j < n.weights[lr][i].length; j++)
                    resilientUpdate(n, lr, i, j);
    }

    
    private void resilientUpdate(MLP n, int lr, int i, int j)
    {
        Retained param = this.data[lr][i][j];
        int gradientSignChange = sign(param.prevGrad * param.gradient);

        float weightChange = 0f;
        float delta; //  adaptation factor

        if (gradientSignChange > 0)
        {
            // if gradient sign has not changed from last epoch, increase learning speed
            delta = Math.min(
                    param.prevDelta * increaseFactor,
                    maxDelta);
            weightChange = sign(param.gradient) * delta;
            param.prevDelta = delta;
        } 
        else if (gradientSignChange < 0)
        {
            delta = Math.max(
                    param.prevDelta * decreaseFactor,
                    minDelta);
            weightChange = -param.prevWgCh;
            // avoid double punishment
            param.gradient = 0;
            param.prevGrad = 0;
            param.prevDelta = delta;
        }
        else if (gradientSignChange == 0)
        {
            delta = param.prevDelta;
            weightChange = sign(param.gradient) * delta;
        }

        n.weights[lr][i][j] += weightChange;
        param.prevWgCh = weightChange;
        param.prevGrad = param.gradient;
        param.gradient = 0;
    }


    
    
    private int sign(float val)
    {
        if (val > 0f) return 1;
        if (val < 0f) return -1;
        return 0;
    }
    
    private void initLayer(int layer, float[][] W)
    {
        if (!(data[layer] == null || data[layer].length != W.length || data[layer][0].length != W[0].length) )
            return;
        data[layer] = new Retained[W.length][];
        for (int i = 0; i < data[layer].length; i++)
        {
            data[layer][i] = new Retained[W[i].length];
            for (int j = 0; j < W[i].length; j++)
                data[layer][i][j] = new Retained();
        }
    }


    @Override
    public LearningAlgorithm<MLP> copy()
    {   
        ElasticBackProp p = new ElasticBackProp();
        p.decreaseFactor = this.decreaseFactor;
        p.increaseFactor = this.increaseFactor;
        p.initialDelta = this.initialDelta;
        p.maxDelta = this.maxDelta;
        p.minDelta = this.minDelta;
        return p;
    }
}
