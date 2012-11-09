package neur.learning.learner;

import neur.MLP;
import neur.Neuron;
import neur.learning.SupervisedLearner;


public class BackPropagation implements SupervisedLearner<MLP> {

    /**
     * @param params {[0] = (Float)learningRateCoef, }
     */
    public final void learn(MLP nw, float[] errorTerm, Object[] params)
    {
        Neuron[][] nwLayers = nw.getLayers();
        float[][][] nwFeedWeights = nw.getFeedWeights();
        float learningRateCoef = (Float)params[0];
        
        // preprocess derivatives for activations
        for(int lr = 0; lr < nwLayers.length; lr++)
        {
            Neuron[] L = nwLayers[lr];
            for(Neuron p : L)
            {
                p.derivativeActivation();
            }
        }
        // deltaWij = coef*delta_j*o_i
        //     (for output layer)
        //          = coef * (f_j'(a_j)(t_j - o_j)) * o_i
        //     (for hidden layers)
        //          = coef * (f_j'(a_j) * sum(delta_k * w_jk)) * o_i
        // (for sigmoid function, output layer)
        //          = coef * (k*o_j*(1-o_j)) * (t_j - o_j) * o_i
        int x = 1;
        Neuron[] LRi = nwLayers[nwLayers.length - 1 - x];
        Neuron[] LRj = nwLayers[nwLayers.length - x];
        float[] delta_k = new float[LRj.length];// store output layer deltas for calculation of hidden layer deltas
        // preprocess output based coefficients for output layer
        float[] Pj_tmp = new float[LRj.length];
        for (int j = 1; j < LRj.length; j++)
        {
            // compute and store:  coef * (k*o_j*(1-o_j)) * (t_j - o_j)
            Neuron Pj = LRj[j];
            delta_k[j] = (Pj.derivativeActivation * (errorTerm[j - 1]));
            Pj_tmp[j] =
                    learningRateCoef
                    * delta_k[j];
        }
        // update weights from hidden layer to output layer
        float[][] W = nwFeedWeights[nwLayers.length - 1 - x];
        updateWeights(W, LRi, Pj_tmp, nwLayers.length - 1 - x);


        // preprocess output based coefficients for hidden layer

        x = 2;
        while (x < nwLayers.length)
        {
            Neuron[] LRk = LRj;
            LRj = LRi;
            LRi = nwLayers[nwLayers.length - 1 - x];
            Pj_tmp = new float[LRj.length];
            float[] delta_j = new float[LRj.length];	// this will be delta_k for next layer up
            //     (for hidden layers deltaWij)
            //          = coef * (f_j'(a_j) * sum(delta_k * w_jk)) * o_i
            //          = coef * (k*o_j*(1-o_j)) * sum(delta_k * w_jk) * o_i
            for (int j = 0; j < LRj.length; j++)
            {
                Neuron Pj = LRj[j];
                float sum = 0f;
                for(int k = 1; k < LRk.length; k++)
                {
                    sum += delta_k[k] * W[k - 1][j];
                }
                delta_j[j] = Pj.derivativeActivation * sum;
                Pj_tmp[j] =
                        learningRateCoef
                        * delta_j[j];
            }
            // update weights from input to hidden layer
            W = nwFeedWeights[nwLayers.length - 1 - x];
            updateWeights(W, LRi, Pj_tmp, nwLayers.length - 1 - x);
            // prepare for next layer up
            delta_k = delta_j;
            x++;
        }
    }

    protected void updateWeights(float[][] W, Neuron[] LRi, float[] Pj_tmp, int layer)
    {
        for (int i = 0; i < LRi.length; i++)
        {
            for(int j = 0; j < W.length; j++)
            {
                W[j][i] = W[j][i] + Pj_tmp[j + 1] * LRi[i].activation;
            }
        }
    }

    @Override
    public void finishEpoch(MLP n, Object[] params)
    {
        //System.out.println("TODO: BackPropagation batch mode - finishEpoch()");
    }

    @Override
    public void clear() { // stateless
    }


}
