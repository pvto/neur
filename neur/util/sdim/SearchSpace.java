
package neur.util.sdim;

import java.math.BigDecimal;
import static neur.util.Arrf.first;
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
            ret += quantisedSize(range, first( quantiser, s.getQuantiser() ))
                    .intValue();
        return ret;
    }

    public int linearEstimateForSize(BigDecimal quantiser, Parameterised ps)
    {
        int ret = 0;
        for(BigDecimal b : ps.keys.getDiscretePoints())
            ret += linearEstimateForSize(quantiser, ps.forKey(b));
        for(BigDecimal[] range : ps.keys.getContinuousRanges())
        {
            SearchDimension d = ps.forKey(range[0]);
            ret += quantisedSize(range, first( quantiser, d.getQuantiser()))
                    .multiply(new BigDecimal( linearEstimateForSize(quantiser, d)))
                    .intValue();
        }
        return ret;
    }

    public BigDecimal getIndexedPoint(BigDecimal quantiser, SearchDimension s, int ind)
    {
        if (ind < s.getDiscretePoints().size())
            return s.getDiscretePoints().get(ind);
        ind -= s.getDiscretePoints().size();
        for(BigDecimal[] range : s.getContinuousRanges())
        {
            ind -= quantisedSize(range, first( quantiser, s.getQuantiser() ))
                    .intValue();
            if (ind <= 0)
                return range[0];
        }
        return null;
    }
    
    public BigDecimal classKeyForIndex(BigDecimal quantiser, Parameterised ps, int i)
    {
        int offset = 0;
        int classInd = 0;
        
        for(BigDecimal b : ps.keys.getDiscretePoints())
        {
            offset += linearEstimateForSize(quantiser, ps.forKey(b));
            if (i < offset)
                return b;
        }
        for (BigDecimal[] range : ps.keys.getContinuousRanges())
        {
            SearchDimension d = ps.forKey(range[0]);
            offset += quantisedSize(range, first( quantiser, d.getQuantiser()))
                    .multiply(new BigDecimal( linearEstimateForSize(quantiser, d)))
                    .intValue();
            if (i < offset)
                return range[0];
        }
        return null;
    }
    public BigDecimal classValueForIndex(BigDecimal quantiser, Parameterised ps, int i)
    {
        int left = i;
        int classInd = 0;
        
        for(BigDecimal b : ps.keys.getDiscretePoints())
        {
            SearchDimension d = ps.forKey(b);
            int size = linearEstimateForSize(quantiser, d); 
            if (left < size)
                return getIndexedPoint(quantiser, d, left);
            left -= size;
        }
        for (BigDecimal[] range : ps.keys.getContinuousRanges())
        {
            SearchDimension d = ps.forKey(range[0]);
            int rsize = quantisedSize(range, first( quantiser, d.getQuantiser())).intValue();
            int dsize = linearEstimateForSize(quantiser, d);
            for(; rsize > 0; rsize--)
            {
                if (left < dsize)
                {
                    return getIndexedPoint(quantiser, d, left);
                }
                left -= dsize;
            }
        }
        return null;
    }
//    
//    
//    public int offsetInIndex(BigDecimal[] simpDimCoords, BigDecimal[][] paramDimCoords, BigDecimal quantiser)
//    {
//        int result = 1;
//        for(int i = 0; i < simpDimCoords.length; i++)
//        {
//            result *= offsetInIndex(simpleDimensions[i], simpDimCoords[i], quantiser);
//        }
//        for (int i = 0; i < paramDimCoords.length; i++)
//        {
//            result *= offsetInIndex(parameterisedDimensions[i], paramDimCoords[i], quantiser);
//        }
//        return result - 1;  // shift to array position [0]
//    }
//    
//    private int offsetInIndex(SearchDimension d, BigDecimal scalar, BigDecimal quantiser)
//    {
//        List<BigDecimal> L = d.getDiscretePoints();
//        int offset = 0;
//        for (; offset < L.size(); offset++)
//            if (scalar.compareTo(L.get(offset)) == 0)
//                return offset;
//        for(BigDecimal[] range : d.getContinuousRanges())
//        {
//            if (scalar.compareTo(range[0]) < 0 || scalar.compareTo(range[1]) > 0)
//            {
//                offset += quantisedSize(range, quantiser).intValue();
//                continue;
//            }
//            return offset + quantumIndex(scalar, range, quantiser);
//        }
//        return 0;
//    }
//    
//    private int offsetInIndex(Parameterised p, BigDecimal[] pscalar, BigDecimal quantiser)
//    {
//        
//        int offset = 0;
//        int threshold = p.keys.getDiscretePoints().size();        
//        for(int i = 0; i < p.classCount(); i++)
//        {
//            SearchDimension d;
//            BigDecimal[] prange = null;
//            if (i < threshold)
//            {
//                d = p.forKey(p.keys.getDiscretePoints().get(i));
//            }
//            else
//            {
//                prange = p.keys.getContinuousRanges().get(i - threshold);
//                d = p.forKey(prange[0]);
//            }
//            if (pscalar[0].intValue() == i)
//            {
//                return offset 
//                        + offsetInIndex(d, pscalar[1], quantiser);
//            }
//            else
//            {
//                if (i < threshold)
//                {
//                    offset += linearEstimateForSize(quantiser, d);
//                }
//                else
//                {
//                    offset += linearEstimateForSize(quantiser, d)
//                            * quantisedSize(prange, first( quantiser, d.getQuantiser() )).intValue();
//                }
//            }
//        }
//        return 1;
//    }
}
