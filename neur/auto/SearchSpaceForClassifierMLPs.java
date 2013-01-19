
package neur.auto;

import java.math.BigDecimal;
import neur.util.sdim.SearchDimension.Parameterised;
import neur.data.Dataset;
import neur.data.TrainMode;
import neur.learning.LearnParams;
import neur.learning.learner.BackPropagation;
import neur.learning.learner.ElasticBackProp;
import static neur.struct.ActivationFunction.Types.AFUNC_SIGMOID;
import static neur.struct.ActivationFunction.Types.AFUNC_TANH;
import neur.util.sdim.SearchDimension;



public class SearchSpaceForClassifierMLPs extends NNSearchSpace {

    public final SearchDimension 
            hiddenLayerSize,
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
            hiddenLayerSize = SearchDimension.create.discrete(1, D.data.length, 1)
                .setName(Dim.HIDDEN_LR_SIZE),
            
            stochasticSearchSize = SearchDimension.create.dispersed(1, 100, 1000)
                .setName(Dim.STOCHASTIC_SEARCH_SIZE),
            
            learningAlgorithm = SearchDimension.create.dispersed(0, 1, 2, 3)
                .setName(Dim.LEARNING_ALGORITHM)
        };
        
        parameterisedDimensions = new Parameterised[]
        { 
            activationFunction = SearchDimension.create.parameterised( new Object[]{
                AFUNC_TANH, 
                    SearchDimension.create.dispersed(1.0),
                AFUNC_SIGMOID, 
                    SearchDimension.create.dispersed(
                        0.1, 0.2, 0.4, 0.8,
                        1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 
                        10.0, 20.0, 40.0, 80.0, 
                        100.0, 200.0, 400.0, 800.0
                    ),
            })
                .attachName(Dim.ACTIVATION_FUNC),
        };
    }
    
    @Override
    public LearnParams resolveTopologyFromFlattenedIndex(LearnParams templ, int offset)
    {
        int range = offset;
        LearnParams ret = templ.copy();
        int a = super.linearEstimateForSize(activationFunction);
        int s = super.linearEstimateForSize(stochasticSearchSize);
        int la = super.linearEstimateForSize(learningAlgorithm);
        int h = range / (a * s * la);
        ret.NNW_DIMS[1] = h + 1;
        
        range %= (a * s);
        BigDecimal[] keyval = super.indexedClassKey_value(activationFunction, range / (s * la));
        ret.NNW_AFUNC = keyval[0].intValue();
        ret.NNW_AFUNC_PARAMS = new float[]{ keyval[1].floatValue()};
        range %= (s * la);
        ret.RANDOM_SEARCH_ITERS = stochasticSearchSize.getDiscretePoints().get(range / la).intValue();
        range %= la;
        switch(range) {
            case 0:
                ret.L = new BackPropagation();
                ret.LEARNING_RATE_COEF = 0.1f;
                ret.DYNAMIC_LEARNING_RATE = false;
                ret.MODE = TrainMode.SUPERVISED_BATCH_MODE;
                break;
            case 1:
                ret.L = new BackPropagation();
                ret.LEARNING_RATE_COEF = 0.1f;
                ret.DYNAMIC_LEARNING_RATE = false;
                ret.MODE = TrainMode.SUPERVISED_ONLINE_MODE;
                break;
            case 2:
                ret.L = new BackPropagation();
                ret.LEARNING_RATE_COEF = 0.1f;
                ret.DYNAMIC_LEARNING_RATE = true;
                ret.MODE = TrainMode.SUPERVISED_ONLINE_MODE;
                break;
            case 3:
                ret.L = new ElasticBackProp();
                ret.MODE = TrainMode.SUPERVISED_ONLINE_MODE;
                break;
        }
        return ret;
    }
    
    public boolean equal(LearnParams a, LearnParams b)
    {
        if (Math.abs( a.NNW_DIMS[1] - b.NNW_DIMS[1]) < 1
                && a.RANDOM_SEARCH_ITERS == b.RANDOM_SEARCH_ITERS
                && a.L.getClass() == b.L.getClass() && a.LEARNING_RATE_COEF == b.LEARNING_RATE_COEF && a.MODE == b.MODE 
                && a.NNW_AFUNC == b.NNW_AFUNC && a.NNW_AFUNC_PARAMS[0] == b.NNW_AFUNC_PARAMS[0])
            return true;
        return true;
    }
}
