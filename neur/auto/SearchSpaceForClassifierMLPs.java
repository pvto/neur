
package neur.auto;

import java.math.BigDecimal;
import neur.auto.SearchDimension.Parameterised;
import neur.data.Dataset;
import neur.learning.LearnParams;
import static neur.struct.ActivationFunction.Types.AFUNC_SIGMOID;
import static neur.struct.ActivationFunction.Types.AFUNC_TANH;



public class SearchSpaceForClassifierMLPs extends SearchSpace {

    public SearchDimension 
            hiddenLayerSize
            ;
    public Parameterised
            activationFunction
            ;

    
    public SearchSpaceForClassifierMLPs(Dataset D)
    {
        simpleDimensions = new SearchDimension[]
        {
            hiddenLayerSize = SearchDimension.discrete.create(
                new BigDecimal(1), 
                new BigDecimal(D.data.length), 
                BigDecimal.ONE),
        };
        
        parameterisedDimensions = new Parameterised[]
        { 
            activationFunction = SearchDimension.parameterised.compose( new Object[] {
                AFUNC_TANH, 
                    SearchDimension.discrete.create(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE),
                AFUNC_SIGMOID, 
                    SearchDimension.discrete.combine(
                        SearchDimension.discrete.create(BigDecimal.ONE, new BigDecimal(5), BigDecimal.ONE),
                        SearchDimension.discrete.create(new BigDecimal(10), new BigDecimal(50), new BigDecimal(10)),
                        SearchDimension.discrete.create(new BigDecimal(100), new BigDecimal(1000), new BigDecimal(100))
                    ),
            }),
        };
    }
    
    @Override
    public LearnParams getTopologyForFlattenedIndex(LearnParams templ, int index, BigDecimal quantiser)
    {
        LearnParams ret = templ.copy();
        
        return ret;
    }
    
}
