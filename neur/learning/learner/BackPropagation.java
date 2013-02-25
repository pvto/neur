package neur.learning.learner;

import neur.MLP;
import neur.Neuron;
import neur.learning.LearningAlgorithm;
import neur.learning.SupervisedLearner;


public class BackPropagation implements SupervisedLearner<MLP> {

    @Override
    public LearningAlgorithm<MLP> copy()
    {   // stateless
        return this;
    }
    /**
     * @param params {[0] = (Float)learningRateCoef, }
     */
    public final void learn(MLP nw, float[] errorTerm, Object[] params)
    {
        Neuron[][] nwLayers = nw.layers;
        float[][][] nwWeights = nw.weights;
        float learningRateCoef = (Float)params[0];
        
        // preprocess derivatives for activations
        for(Neuron[] L : nwLayers)
            for(Neuron p : L)
                p.derivativeActivation();

        beforeBackpropagation(nw, errorTerm, params);
        
        // deltaWij = coef*delta_j*o_i
        //     (for output layer)
        //          = coef * (f'(a_j)(t_j - o_j)) * o_i
        //     (for hidden layers)
        //          = coef * (f'(a_j) * sum(delta_k * w_jk)) * o_i
        int layer = nwLayers.length - 1;
        Neuron[] LRj = nwLayers[layer];
        float[] delta_k = new float[LRj.length];// store output layer deltas for calculation of hidden layer deltas
        // preprocess output error based corrections for weights into output layer
        float[] corrections = new float[LRj.length];
        for (int j = 0; j < LRj.length; j++)
        {
            // compute and store:  coef * (k*o_j*(1-o_j)) * (t_j - o_j)
            delta_k[j] = (LRj[j].derivativeActivation * (errorTerm[j]));
            corrections[j] =  
                    learningRateCoef * delta_k[j];
        }
        // update hidden->output weights
        layer--;
        updateWeights(nwWeights[layer], nwLayers[layer], corrections, layer);


        // process mediated corrections for hidden layers
        while (layer > 0)
        {
            float[][] Wjk = nwWeights[layer];
            LRj = nwLayers[layer];
            corrections = new float[LRj.length];
            float[] delta_j = new float[LRj.length];	// this will be delta_k for next layer up
            //     for hidden layers d(Wij)
            //         = coef * (f'(a_j) * sum(delta_k * w_jk)) * o_i
            for(int j = 0; j < Wjk.length; j++)
            {
                // calculate the sum of errors on the next lower layer connecting to a neuron
                float net = 0f;
                for (int k = 0; k < Wjk[j].length; k++)
                    net += delta_k[k] * Wjk[j][k];
                delta_j[j] = LRj[j].derivativeActivation * net;
                corrections[j] =
                        learningRateCoef * delta_j[j];
            }
            // update upper->hidden weights
            layer--;
            updateWeights(nwWeights[layer], nwLayers[layer], corrections, layer);
            // prepare for next layer up
            delta_k = delta_j;
        }
        afterBackpropagation(nw, errorTerm, params);
    }

    protected void updateWeights(float[][] W, Neuron[] LRi, float[] corrections, int layer)
    {
        for (int i = 0; i < LRi.length; i++)
            for(int j = 0; j < W[i].length; j++)
                W[i][j] = W[i][j] + corrections[j] * LRi[i].activation;
    }

    @Override
    public void finishEpoch(MLP n, Object[] params)
    {
        //System.out.println("TODO: BackPropagation batch mode - finishEpoch()");
    }

    @Override
    public void clear(){} // stateless

    protected void beforeBackpropagation(MLP nw, float[] errorTerm, Object[] params) {}
    
    protected void afterBackpropagation(MLP nw, float[] errorTerm, Object[] params)
    {
        // experimental: kill NaN/Infinite weights
        for(float[][] lr : nw.weights)
            for(float[] line : lr)
                for(int i = 0; i < line.length; i++)
                    if (Float.isNaN(line[i]) || Float.isInfinite(line[i]))
                            line[i] = (float)Math.random() - 0.5f;
    }

}
