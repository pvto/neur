
package neur.auto.routine;

import java.util.TreeSet;
import neur.learning.LearnRecord;

/**
 *
 * @author Paavo Toivanen
 */
public class RelativeMLPFitnessComparator {

    
    private static class Rec implements Comparable<Rec>
    {
        LearnRecord r;
        
        @Override  public int compareTo(Rec o)
        {
            float i = r.averageTestsetCorrect - o.r.averageTestsetCorrect;
            if (i != 0f) 
                return (int)Math.signum(i);
            if (r.averageSummedError > o.r.averageSummedError)
            {
                return 1;
            }
            else if (r.averageSummedError < o.r.averageSummedError)
            {
                return -1;
            }
            return 0;
        }
        
    }
    private TreeSet<Rec> seq = new TreeSet<Rec>();
    
    
    private final float 
            BIG = Float.MAX_VALUE / 8f,
            SMALL = 1f / 64f
            ;
    public synchronized LearnRecord putFitness(LearnRecord r)
    {

        Rec R = new Rec();
        R.r = r;
        Rec higher = seq.higher(R);
        Rec lower = seq.lower(R);
        if (higher == null)
        {
            if (lower == null)
            {
                r.averageFitness = 1f;
            }
            else
            {
                r.averageFitness = lower.r.averageFitness * (lower.r.averageFitness > BIG ? 1.1f : 2f);  // make the relative space more dense near overflow space
            }
        }
        else
        {
            if (lower == null)
            {
                r.averageFitness = higher.r.averageFitness * (higher.r.averageFitness < SMALL ? 0.95f : 0.5f); // make the relative space more dense near zero
            }
            else
            {
                r.averageFitness = (higher.r.averageFitness - lower.r.averageFitness) / 2f;
            }
        }
        seq.add(R);
        return r;
    }

    
    
}
