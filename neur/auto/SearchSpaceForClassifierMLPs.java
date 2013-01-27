
package neur.auto;

import java.math.BigDecimal;
import neur.util.sdim.SearchDimension.Parameterised;
import neur.data.Dataset;
import neur.data.TrainMode;
import neur.learning.LearnParams;
import neur.learning.LearningAlgorithm;
import neur.learning.learner.BackPropagation;
import neur.learning.learner.ElasticBackProp;
import static neur.struct.ActivationFunction.Types.*;
import neur.util.sdim.SearchDimension;
import neur.util.sdim.SearchDimension.TargetGenerator;



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
                .setTargetGenerator(new TargetGenerator() {
                    @Override public Object generate(int index)
                    {
                        switch(index) {
                        case 0: 
                            return new Object[]{ new BackPropagation(), 0.1f, false, TrainMode.SUPERVISED_BATCH_MODE };
                        case 1:
                            return new Object[]{ new BackPropagation(), 0.1f, false, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 2:
                            return new Object[]{ new BackPropagation(), 0.1f, true, TrainMode.SUPERVISED_ONLINE_MODE };
                        case 3:
                            return new Object[]{ new ElasticBackProp(), 0.1f, false, TrainMode.SUPERVISED_ONLINE_MODE };
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
        NNSearchSpace.LearningAlgorithmParameters o = learningAlgorithm.getTargetGenerator().generate(range);
        ret.L = o.L;
        ret.LEARNING_RATE_COEF = o.LEARNING_RATE_COEF;
        ret.DYNAMIC_LEARNING_RATE = o.DYNAMIC_LEARNING_RATE;
        ret.MODE = o.MODE;
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
