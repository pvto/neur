
package neur.auto;

import java.math.BigDecimal;
import neur.data.Dataset;
import neur.data.TrainMode;
import neur.learning.LearnParams;
import neur.learning.LearningAlgorithm;
import neur.learning.learner.BackPropagation;
import neur.learning.learner.ElasticBackProp;
import neur.learning.learner.MomentumEBP;
import static neur.struct.ActivationFunction.Types.*;
import neur.util.sdim.SearchDimension;
import neur.util.sdim.SearchDimension.Parameterised;
import neur.util.sdim.SearchDimension.TargetGenerator;



public class SearchSpaceForClassifierMLPs extends NNSearchSpace {

    public final SearchDimension 
            hiddenLayerSize,
            hiddenLayerCount,
            stochasticSearchSize,
            learningAlgorithm
            ;
    public final Parameterised
            activationFunction
            ;

    
    public SearchSpaceForClassifierMLPs(Dataset D)
    {
        simpleDimensions = new SearchDimension[]
        {
            hiddenLayerSize = SearchDimension.create.discrete(1, Math.min(100, D.data.length / 6), 1)
                .setName(Dim.HIDDEN_LR_SIZE),
            
            hiddenLayerCount = SearchDimension.create.dispersed(0, 1, 2, 3, 4)
                .setName(Dim.HIDDEN_LR_COUNT),
            
            stochasticSearchSize = SearchDimension.create.dispersed(0, 100, 1000)
                .setName(Dim.STOCHASTIC_SEARCH_SIZE),
            
            learningAlgorithm = SearchDimension.create.discrete(0, 15, 1)
                .setName(Dim.LEARNING_ALGORITHM)
                .setTargetGenerator(new TargetGenerator() {
                    @Override public Object generate(int index)
                    {
                        switch(index) {
                        case 0:
                            return new Object[]{ new BackPropagation(), 0.001f, false, TrainMode.SUPERVISED_MINIBATCH };
                        case 1:
                            return new Object[]{ new BackPropagation(), 0.001f, false, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 2:
                            return new Object[]{ new BackPropagation(), 0.001f, true, TrainMode.SUPERVISED_MINIBATCH };
                        case 3:
                            return new Object[]{ new BackPropagation(), 0.001f, true, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 4:
                            return new Object[]{ new BackPropagation(), 0.01f, false, TrainMode.SUPERVISED_MINIBATCH };
                        case 5:
                            return new Object[]{ new BackPropagation(), 0.01f, false, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 6:
                            return new Object[]{ new BackPropagation(), 0.01f, true, TrainMode.SUPERVISED_MINIBATCH };
                        case 7:
                            return new Object[]{ new BackPropagation(), 0.01f, true, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 8:
                            return new Object[]{ new BackPropagation(), 0.1f, false, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 9:
                            return new Object[]{ new BackPropagation(), 0.1f, false, TrainMode.SUPERVISED_MINIBATCH };
                        case 10:
                            return new Object[]{ new BackPropagation(), 0.1f, true, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 11:
                            return new Object[]{ new BackPropagation(), 0.1f, true, TrainMode.SUPERVISED_MINIBATCH };
                        case 12:
                            return new Object[]{ new ElasticBackProp(), 0.1f, true, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 13:
                            return new Object[]{ new ElasticBackProp(), 0.1f, true, TrainMode.SUPERVISED_MINIBATCH };
                        case 14:
                            return new Object[]{ new MomentumEBP(), 0.1f, true, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 15:
                            return new Object[]{ new MomentumEBP(), 0.1f, true, TrainMode.SUPERVISED_MINIBATCH };

                        }
                        return null;
                    }
                })
        };
        
        parameterisedDimensions = new Parameterised[]
        { 
            activationFunction = SearchDimension.create.parameterised( new Object[]{
                AFUNC_TANH,     SearchDimension.create.dispersed(1.0),
                AFUNC_SOFTSIGN, SearchDimension.create.dispersed(1.0),
                AFUNC_SIGMOID,  SearchDimension.create.dispersed(0.4, 1.0, 3.0, 9.0),
            })
                .attachName(Dim.ACTIVATION_FUNC),
        };
    }
    
    @Override
    public LearnParams resolveTopologyFromFlattenedIndex(LearnParams templ, int offset)
    {
        int range = offset;
        LearnParams ret = templ.copy();
        int h;
        int hc = super.linearEstimateForSize(hiddenLayerCount);
        int a = super.linearEstimateForSize(activationFunction);
        int s = super.linearEstimateForSize(stochasticSearchSize);
        int la = super.linearEstimateForSize(learningAlgorithm);
        // resolve hidden neuron count per layer
        h = range / (hc * a * s * la);
        range %= (hc * a * s * la);
        // resolve hidden layer count
        int hcount = hiddenLayerCount.getDiscretePoints().get(range / (a * s * la)).intValue();
        int[] NNWD = new int[hcount+2];
        NNWD[0] = ret.NNW_DIMS[0];  NNWD[NNWD.length - 1] = ret.NNW_DIMS[ret.NNW_DIMS.length - 1];
        for(int i = hcount; i > 0; i--)
            NNWD[i] = h;
        ret.NNW_DIMS = NNWD;
        range %= (a * s * la);
        // resolve activation function and parameters
        BigDecimal[] keyval = super.indexedClassKey_value(activationFunction, range / (s * la));
        ret.NNW_AFUNC = keyval[0].intValue();
        ret.NNW_AFUNC_PARAMS = new float[]{ keyval[1].floatValue()};
        range %= (s * la);
        // resolve stochastic search iteration count
        ret.STOCHASTIC_SEARCH_ITERS = stochasticSearchSize.getDiscretePoints().get(range / la).intValue();
        range %= la;
        // resolve learning algorithm
        Object[] oo = learningAlgorithm.getTargetGenerator().generate(range);;
        ret.L = (LearningAlgorithm) oo[0];//o.L;
        ret.LEARNING_RATE_COEF = (float) oo[1]; //o.LEARNING_RATE_COEF;
        ret.DYNAMIC_LEARNING_RATE = (boolean) oo[2]; //o.DYNAMIC_LEARNING_RATE;
        ret.MODE = (TrainMode) oo[3]; //o.MODE;
        return ret;
    }
    
    public boolean equal(LearnParams a, LearnParams b)
    {
        if (Math.abs( a.NNW_DIMS[1] - b.NNW_DIMS[1]) == 0
                && a.NNW_DIMS.length == b.NNW_DIMS.length
                && a.STOCHASTIC_SEARCH_ITERS == b.STOCHASTIC_SEARCH_ITERS
                && a.L.getClass() == b.L.getClass() && a.LEARNING_RATE_COEF == b.LEARNING_RATE_COEF && a.MODE == b.MODE 
                && a.NNW_AFUNC == b.NNW_AFUNC && a.NNW_AFUNC_PARAMS[0] == b.NNW_AFUNC_PARAMS[0])
            return true;
        return false;
    }
}
