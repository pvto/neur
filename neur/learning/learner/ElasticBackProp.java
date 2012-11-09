
package neur.learning.learner;

import neur.MLP;
import neur.Neuron;

/** An implementation of elastic error back propagation algorithm.
 * Adapted from the neuroph project, http://neuroph.svn.sourceforge.net/viewvc/neuroph/trunk/neuroph-2.7/Core/src/main/java/org/neuroph/nnet/learning/ResilientPropagation.java?revision=1338&content-type=text%2Fplain .
 *
 * @author Paavo Toivanen
 */
public final class ElasticBackProp extends BackPropagation {

    public float decreaseFactor = 0.5f;
    public float increaseFactor = 1.2f;
    public float initialDelta = 0.1f;
    public float maxDelta = 1f;
    public float minDelta = 1e-6f;

    Retained[][][] data;
    { clear(); }
    
    static final class Retained   // for a weight
    {  float gradient = 1f, prevGrad = 0f, prevWgCh = 0f, prevDelta = 0f;  }
    
    @Override
    public void clear() { data = new Retained[7][][]; }

    
    
    protected final void updateWeights(float[][] W, Neuron[] LRi, float[] Pj_tmp, int layer)
    {
        initLayer(layer, W);
        for (int i = 0; i < LRi.length; i++)
            for(int j = 0; j < W.length; j++)
                data[layer][j][i].gradient += Pj_tmp[j + 1] * LRi[i].activation;

    }

    @Override
    public void finishEpoch(MLP n, Object[] params)
    {
        for (int lr = n.feedWeights.length - 2; lr >= 0; lr--)
            for (int i = 0; i < n.layers[lr].length; i++)
                for(int j = 0; j < n.feedWeights[lr].length; j++)
                    resilientUpdate(n, lr, j, i);
    }

    
    private void resilientUpdate(MLP n, int lr, int j, int i)
    {
        Retained param = this.data[lr][j][i];
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

        n.feedWeights[lr][j][i] += weightChange;
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
        for (int j = 0; j < data[layer].length; j++)
        {
            data[layer][j] = new Retained[W[j].length];
            for (int i = 0; i < W[j].length; i++)
                data[layer][j][i] = new Retained();
        }
    }


}
