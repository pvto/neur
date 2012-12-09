
package neur.auto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import neur.util.Arrf;

/** Enumerates a list of possible values for a single parameter.
 *
 * @author Paavo Toivanen
 */
public interface SearchDimension {

    
    List<BigDecimal> getDiscretePoints();
    /** @return a list of ranges of form {min,max}. Ranges are assumed to be inclusive of their terminal points. */
    List<BigDecimal[]> getContinuousRanges();
    
    
    
    
    public static class Discrete implements SearchDimension
    {
        List points = new ArrayList();
        public Discrete(){}
        public Discrete(List<BigDecimal> points) {                      this.points.addAll(points); }                   
        @Override public List<BigDecimal> getDiscretePoints() {         return points; }
        @Override public List<BigDecimal[]> getContinuousRanges() {     return java.util.Collections.emptyList(); }

    }
    public static class Continuous extends Discrete
    {
        List ranges = new ArrayList();
        @Override public List<BigDecimal[]> getContinuousRanges() {     return ranges; }
    }
    
    public class Parameterised
    {
        public SearchDimension keys = new Continuous();
        private Map<Object,SearchDimension> map = new HashMap();
        public SearchDimension forKey(BigDecimal key)
        { 
            SearchDimension ret = map.get(key);
            // check if it was a discretely defined point
            if (ret != null)    
                return ret;
            // else work for the range
            for(BigDecimal[] range : keys.getContinuousRanges())
            {
                if (range[0].compareTo(key) <= 0 && key.compareTo(range[1]) <= 0)
                    return map.get(range);
            }
            // this should never happen!
            throw new RuntimeException("SearchDimension.Parameterised was constructed in a bad way or it does not contain a single point nor a range for " + key);
        }
    }
    
    
    
    
    
    // ----- implementations for discrete search dimensions ----- //
    
    public static final class discrete
    {
        
        public static SearchDimension create(BigDecimal start, BigDecimal end, BigDecimal step)
        {
            int size = end.subtract(start).divide(step).intValue() + 1;
            ArrayList points = new ArrayList(size);
            BigDecimal v = start.add(start.ZERO);
            int i = 0;
            while(i < size)
            {
                points.add(v);
                v = v.add(step);
            }
            return new Discrete(points);
        }

        public static SearchDimension combine(SearchDimension... ss)
        {
            int size = Arrf.combinedSize(ss);
            List S = ss[0].getDiscretePoints();
            List ret = new ArrayList(S);
            for (int i = 1; i < ss.length; i++)
            {
                ret.addAll(ss[i].getDiscretePoints());
            }
            return new Discrete(ret);
        }
    }
    
    
    
    
    
    public static final class parameterised
    {
        
        public static Parameterised compose(Object[] keyDimensionPairs)
        {
            Parameterised ret = new Parameterised();
            Object[] o = keyDimensionPairs;
            for (int i = 0; i < keyDimensionPairs.length; i += 2)
            {
                if (!(o[i] instanceof Integer) && !(o[i] instanceof BigDecimal) && !(o[i] instanceof Double)
                        && !(o[i].getClass().isArray() && ((BigDecimal[])o[i]).length < 2))
                {
                    throw new RuntimeException("Param.Dim - key must resolve to int,BigDecimal,double,or {BigDecimal,BigDecimal}");
                }
                if (o[i].getClass().isArray())
                {
                    ret.keys.getContinuousRanges().add((BigDecimal[])o[i]);
                }
                else
                {
                    BigDecimal key = (o[i] instanceof Integer? new BigDecimal((Integer)o[i])
                            :(o[i] instanceof Double? new BigDecimal((Double)o[i])
                            :(BigDecimal)o[i]
                            ));
                    ret.keys.getDiscretePoints().add((BigDecimal)key);
                }
                SearchDimension d = (SearchDimension)o[i+1];
                ret.map.put(o[i], d);
            }
            return ret;
        }
    }
}
