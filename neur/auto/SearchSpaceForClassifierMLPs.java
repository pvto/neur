
package neur.auto;

import java.math.BigDecimal;
import neur.util.sdim.SearchDimension.Parameterised;
import neur.data.Dataset;
import neur.learning.LearnParams;
import static neur.struct.ActivationFunction.Types.AFUNC_SIGMOID;
import static neur.struct.ActivationFunction.Types.AFUNC_TANH;
import neur.util.sdim.SearchDimension;



public class SearchSpaceForClassifierMLPs extends NNSearchSpace {

    public final SearchDimension 
            hiddenLayerSize
            ;
    public final Parameterised
            activationFunction
            ;

    
    public SearchSpaceForClassifierMLPs(Dataset D)
    {
        simpleDimensions = new SearchDimension[]
        {
            hiddenLayerSize = SearchDimension.create.discrete(1, D.data.length, 1),
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
            }),
        };
    }
    
    @Override
    public LearnParams resolveTopologyFromFlattenedIndex(LearnParams templ, int offset, BigDecimal quantiser)
    {
        LearnParams ret = templ.copy();
        int a = super.linearEstimateForSize(quantiser, activationFunction);
        int h = offset / a;
        ret.NNW_DIMS[1] = h + 1;
        // TODO: combine classKeyForIndex and classValueForIndex
        BigDecimal[] keyval = super.indexedClassKey_value(quantiser, activationFunction, offset % a);
        ret.NNW_AFUNC = keyval[0].intValue();
        ret.NNW_AFUNC_PARAMS = new float[]{ keyval[1].floatValue()};
        return ret;
    }
    
    
}
