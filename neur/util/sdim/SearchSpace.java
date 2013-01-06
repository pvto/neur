
package neur.util.sdim;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import static neur.util.Ranges.quantisedSize;
import neur.util.sdim.SearchDimension.Parameterised;

/**
 *
 * @author Paavo Toivanen
 */
public abstract class SearchSpace {

    public SearchDimension.Parameterised[] parameterisedDimensions;
    public SearchDimension[] simpleDimensions;


    

    // if you get numeric overflow from this, your quantiser is probably too small...
    public int linearEstimateForSize()
    {
        int ret = 1;
        if (parameterisedDimensions != null)
            for (int i = 0; i < parameterisedDimensions.length; i++)
                ret *= linearEstimateForSize(parameterisedDimensions[i]);
        if (simpleDimensions != null)
            for (int i = 0; i < simpleDimensions.length; i++)
                ret *= linearEstimateForSize(simpleDimensions[i]);
        return ret;
    }
    
    public int linearEstimateForSize(SearchDimension s)
    {
        int ret = s.getDiscretePoints().size();
        for (BigDecimal[] range : s.getContinuousRanges())
            ret += quantisedSize(range, s.getQuantiser())
                    .intValue();
        return ret;
    }

    public int linearEstimateForSize(Parameterised ps)
    {
        int ret = 0;
        for(BigDecimal b : ps.keys.getDiscretePoints())
            ret += linearEstimateForSize(ps.forKey(b));
        for(BigDecimal[] range : ps.keys.getContinuousRanges())
        {
            SearchDimension d = ps.forKey(range[0]);
            ret += quantisedSize(range, d.getQuantiser())
                    .multiply(new BigDecimal( linearEstimateForSize(d)))
                    .intValue();
        }
        return ret;
    }

    public BigDecimal getIndexedPoint(SearchDimension s, int ind)
    {
        if (ind < s.getDiscretePoints().size())
            return s.getDiscretePoints().get(ind);
        ind -= s.getDiscretePoints().size();
        for(BigDecimal[] range : s.getContinuousRanges())
        {
            ind -= quantisedSize(range, s.getQuantiser() )
                    .intValue();
            if (ind <= 0)
                return range[0];
        }
        return null;
    }
    
    public BigDecimal[] indexedClassKey_value(Parameterised ps, int ind)
    {
        int left = ind;
        int classInd = 0;
        
        for(BigDecimal b : ps.keys.getDiscretePoints())
        {
            SearchDimension d = ps.forKey(b);
            int size = linearEstimateForSize(d); 
            if (left < size)
                return new BigDecimal[]{ b, getIndexedPoint(d, left) };
            left -= size;
        }
        for (BigDecimal[] range : ps.keys.getContinuousRanges())
        {
            SearchDimension d = ps.forKey(range[0]);
            int rsize = quantisedSize(range, d.getQuantiser()).intValue();
            int dsize = linearEstimateForSize(d);
            for(; rsize > 0; rsize--)
            {
                if (left < dsize)
                {
                    return new BigDecimal[]{ range[0], getIndexedPoint(d, left) };
                }
                left -= dsize;
            }
        }
        return null;
    }

    
    
    
    // ----- support for name-based reference of dimensions ----- //
    private Map names;
    private Map getNames()
    {
        if (names != null)
            return names;
        names = new HashMap();
        for(Parameterised p : parameterisedDimensions)
            names.put(p.name, p);
        for(SearchDimension s : simpleDimensions)
            names.put(s.getName(), s);
        return names;
    }
    public Parameterised    parameterisedForName(Object name)   { return (Parameterised) getNames().get(name); }
    public SearchDimension  dimensionForName(Object name)       { return (SearchDimension) getNames().get(name); }

}
