
package neur.auto;

import java.math.BigDecimal;
import neur.auto.SearchDimension.Parameterised;
import neur.learning.LearnParams;

/**
 *
 * @author Paavo Toivanen
 */
public abstract class SearchSpace {

    public SearchDimension.Parameterised[] parameterisedDimensions;
    public SearchDimension[] simpleDimensions;

    public abstract LearnParams getTopologyForFlattenedIndex(LearnParams templ, int index, BigDecimal quantiser);
    
    
    
    // if you get numeric overflow from this, your quantiser is probably too small...
    public int linearEstimateForSize(BigDecimal quantiser)
    {
        int ret = 1;
        if (parameterisedDimensions != null)
            for (int i = 0; i < parameterisedDimensions.length; i++)
                ret *= linearEstimateForSize(quantiser, parameterisedDimensions[i]);
        if (simpleDimensions != null)
            for (int i = 0; i < simpleDimensions.length; i++)
                ret *= linearEstimateForSize(quantiser, simpleDimensions[i]);
        return ret;
    }
    
    public int linearEstimateForSize(BigDecimal quantiser, SearchDimension s)
    {
        int ret = s.getDiscretePoints().size();
        for (BigDecimal[] range : s.getContinuousRanges())
            ret += ( range[1].subtract(range[0]).add(quantiser).divide(quantiser) )
                    .intValue();
        return ret;
    }

    public int linearEstimateForSize(BigDecimal quantiser, Parameterised ps)
    {
        int ret = 0;
        for(BigDecimal b : ps.keys.getDiscretePoints())
            ret += linearEstimateForSize(quantiser, ps.forKey(b));
        for(BigDecimal[] range : ps.keys.getContinuousRanges())
            ret += ( range[1].subtract(range[0]).add(quantiser).divide(quantiser) )
                    .multiply(new BigDecimal( linearEstimateForSize(quantiser, ps.forKey(range[0]) )))
                    .intValue();
        return ret;
    }
}
