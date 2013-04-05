
package neur.learning;

import java.io.Serializable;
import java.util.Arrays;
import neur.NeuralNetwork;
import neur.data.Dataset;
import neur.data.TrainMode;
import neur.learning.fit.TrainTestStability;
import neur.struct.ActivationFunction;

/**
 *
 * @author Paavo Toivanen
 */
public class LearnParams<T extends NeuralNetwork, U extends LearningAlgorithm> 
        implements Serializable
{

    public int id = 0;  // used for joining remote computations
    public int[] NNW_DIMS;
    public int NNW_AFUNC = ActivationFunction.Types.AFUNC_SIGMOID;
    public float[] NNW_AFUNC_PARAMS = { 1f };
    public T nnw;
    public Dataset D;
    public Dataset.Slicing DATASET_SLICING;
    public int TESTSET_SIZE = 1;
    public int TRAINSET_SIZE()  {   return D.data.length - TESTSET_SIZE; }
    /** how many training sets to create from the dataset; set to 0 (zero) for the number of items n in the dataset,
     and to a negative value for n - x */
    public int NUMBER_OF_TRAINING_SETS = 0;
    public Classifier CF;    
    public U L;
    public float LEARNING_RATE_COEF = 0.5f;    
    public TrainMode MODE;
    public int MINIBATCH_SIZE = 10;
    public int STOCHASTIC_SEARCH_ITERS = 100;
    public int TEACH_MAX_ITERS = 1000;
    public boolean DYNAMIC_LEARNING_RATE = true;
    public float TRG_ERR = 0.01f;
    public int DIVERGENCE_PRESUMED = 1000;
    public LRecFitness FIT_FUNC = new TrainTestStability();

    public LearnParams copy()
    {
        LearnParams<T,U> p = new LearnParams<T,U>();
        p.id = id;
        p.NNW_DIMS = NNW_DIMS;
        p.NNW_AFUNC = NNW_AFUNC;
        p.NNW_AFUNC_PARAMS = Arrays.copyOf(NNW_AFUNC_PARAMS, NNW_AFUNC_PARAMS.length);
        p.nnw = nnw==null?null:(T)nnw.copy();
        p.D = D.copy();
        p.DATASET_SLICING = DATASET_SLICING;
        p.TESTSET_SIZE = TESTSET_SIZE;
        p.NUMBER_OF_TRAINING_SETS = NUMBER_OF_TRAINING_SETS;
        p.CF = CF;
        p.L = L==null ? null : (U)L.copy();
        p.LEARNING_RATE_COEF = LEARNING_RATE_COEF;
        p.MODE = MODE;
        p.MINIBATCH_SIZE = MINIBATCH_SIZE;
        p.STOCHASTIC_SEARCH_ITERS = STOCHASTIC_SEARCH_ITERS;
        p.TEACH_MAX_ITERS = TEACH_MAX_ITERS;
        p.DYNAMIC_LEARNING_RATE = DYNAMIC_LEARNING_RATE;
        p.TRG_ERR = TRG_ERR;
        p.DIVERGENCE_PRESUMED = DIVERGENCE_PRESUMED;
        p.FIT_FUNC = FIT_FUNC;
        return p;
    }

    public int getNumberOfPendingTrainingSets(LearnRecord lrec)
    {
        int max = NUMBER_OF_TRAINING_SETS <= 0 
                ? D.data.length + NUMBER_OF_TRAINING_SETS 
                : NUMBER_OF_TRAINING_SETS;
        int trained = (lrec!=null ? lrec.items.size() : 0);
        return max - trained;
    }
}
