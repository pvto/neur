
package neur.util.sdim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import neur.util.Arrf;

/** Enumerates a list of possible values for a single parameter.
 *
 * @author Paavo Toivanen
 */
public interface SearchDimension {

    
    List<BigDecimal> getDiscretePoints();
    /** @return a list of ranges of form {min,max}. Ranges are assumed to be inclusive of their terminal points. */
    List<BigDecimal[]> getContinuousRanges();
    
    BigDecimal getQuantiser();
    
    
    
    public static class Discrete implements SearchDimension
    {
        public Discrete(){}
        public Discrete(List<BigDecimal> points) {                      this.points.addAll(points); }                   
        @Override public List<BigDecimal> getDiscretePoints() {         return points; }
        @Override public List<BigDecimal[]> getContinuousRanges() {     return java.util.Collections.emptyList(); }
        @Override public BigDecimal getQuantiser() {                    return quantiser; }
        public BigDecimal quantiser = BigDecimal.ONE;
        List points = new ArrayList();
    }
    public static class Continuous extends Discrete
    {
        public Continuous(){}
        public Continuous(BigDecimal quantiser) {                       this.quantiser = quantiser; }
        @Override public List<BigDecimal[]> getContinuousRanges() {     return ranges; }
        List ranges = new ArrayList();
    }
    
    public class Parameterised
    {
        public SearchDimension keys = new Continuous();
        private LinkedHashMap<Object,SearchDimension> map = new LinkedHashMap();
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
            throw new RuntimeException("SearchDimension.Parameterised was constructed in a bad way or it does not "
                    +"contain a single point nor a range for " + key);
        }

        public int classCount()
        {
            return keys.getDiscretePoints().size() + keys.getContinuousRanges().size();
        }

    }
    
    
    
    
    
    // ----- implementations for simple search dimensions ----- //
    
    public static final class create
    {
        public static SearchDimension       discrete(double start, double end, double step)
        {   return discrete(new BigDecimal(start), new BigDecimal(end), new BigDecimal(step)); }
        public static SearchDimension       discrete(int start, int end, int step)
        {   return discrete(new BigDecimal(start), new BigDecimal(end), new BigDecimal(step)); }
        public static SearchDimension       discrete(BigDecimal start, BigDecimal end, BigDecimal step)
        {
            int size = end.subtract(start).divide(step).intValue() + 1;
            ArrayList points = new ArrayList(size);
            BigDecimal v = start.add(start.ZERO);
            int i = size;
            while(i-- > 0)
            {
                points.add(v);
                v = v.add(step);
            }
            return new Discrete(points);
        }
        public static SearchDimension       dispersed(double... entries)
        {
            ArrayList points = new ArrayList();
            for(Double d : entries)
                points.add(new BigDecimal(d));
            return new Discrete(points);
        }

        public static SearchDimension       combine(SearchDimension... ss)
        {
            List S = ss[0].getDiscretePoints();
            ArrayList points = new ArrayList(S);
            List ranges = new ArrayList(ss[0].getContinuousRanges());
            for (int i = 1; i < ss.length; i++)
            {
                points.addAll(ss[i].getDiscretePoints());
                ranges.addAll(ss[i].getContinuousRanges());
            }
            if (ranges.size() == 0)
                return new Discrete(points);
            Continuous c = new Continuous();
            c.points = points;
            c.ranges = ranges;
            return c;
        }
        
        /** @param keyDimensionPairs a list of pairs {key:number, SearchDimension} */
        public static Parameterised         parameterised(Object[] keyDimensionPairs)
        {
            Parameterised ret = new Parameterised();
            Object[] o = keyDimensionPairs;
            for (int i = 0; i < keyDimensionPairs.length; i += 2)
            {
                if (!(o[i] instanceof Integer) && !(o[i] instanceof BigDecimal) && !(o[i] instanceof Double)
                        && !(o[i].getClass().isArray() && ((BigDecimal[])o[i]).length < 2))
                {
                    throw new RuntimeException("Param.Dim - key must resolve to int,BigDecimal,double,or BigDecimal[2]");
                }
                SearchDimension d = (SearchDimension)o[i+1];
                
                if (o[i].getClass().isArray())
                {
                    ret.keys.getContinuousRanges().add((BigDecimal[])o[i]);
                    ret.map.put(o[i], d);
                }
                else
                {
                    BigDecimal key = (o[i] instanceof Integer? new BigDecimal((Integer)o[i])
                            :(o[i] instanceof Double? new BigDecimal((Double)o[i])
                            :(BigDecimal)o[i]
                            ));
                    ret.keys.getDiscretePoints().add((BigDecimal)key);
                    ret.map.put(key, d);
                }
            }
            return ret;
        }
    }
}
