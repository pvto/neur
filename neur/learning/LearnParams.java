
package neur.learning;

import java.io.Serializable;
import neur.NeuralNetwork;
import neur.data.Dataset;
import neur.data.TrainMode;
import neur.struct.ActivationFunction;

/**
 *
 * @author Paavo Toivanen
 */
public class LearnParams<T extends NeuralNetwork, U extends LearningAlgorithm> 
        implements Serializable
{

    public int[] NNW_DIMS;
    public int NNW_AFUNC = ActivationFunction.Types.AFUNC_SIGMOID;
    public T nnw;
    public Dataset D;
    public Classifier CF;    
    public U L;
    public float LEARNING_RATE_COEF = 0.5f;    
    public TrainMode MODE;
    public int RANDOM_SEARCH_ITERS = 100;
    public int TEACH_MAX_ITERS = 1000;
    public int TEACH_TARRY_NOT_CONVERGING = 1;
    public boolean DYNAMIC_LEARNING_RATE = true;
    public float TRG_ERR = 0.01f;
    public int DIVERGENCE_PRESUMED = 1000;

    public LearnParams copy()
    {
        LearnParams<T,U> p = new LearnParams<T,U>();
        p.NNW_DIMS = NNW_DIMS;
        p.NNW_AFUNC = NNW_AFUNC;
        p.nnw = (T)nnw.copy();
        p.D = D;
        p.CF = CF;
        p.L = L;
        p.LEARNING_RATE_COEF = LEARNING_RATE_COEF;
        p.MODE = MODE;
        p.RANDOM_SEARCH_ITERS = RANDOM_SEARCH_ITERS;
        p.TEACH_MAX_ITERS = TEACH_MAX_ITERS;
        p.TEACH_TARRY_NOT_CONVERGING = TEACH_TARRY_NOT_CONVERGING;
        p.DYNAMIC_LEARNING_RATE = DYNAMIC_LEARNING_RATE;
        p.TRG_ERR = TRG_ERR;
        p.DIVERGENCE_PRESUMED = DIVERGENCE_PRESUMED;
        return p;
    }
    
}
