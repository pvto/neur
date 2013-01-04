
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
            int i = r.testsetCorrect - o.r.testsetCorrect;
            if (i != 0) 
                return i;
            if (r.lastTrainres.variance > o.r.lastTrainres.variance)
            {
                return 1;
            }
            else if (r.lastTrainres.variance < o.r.lastTrainres.variance)
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
                r.fitness = 1f;
            }
            else
            {
                r.fitness = lower.r.fitness * (lower.r.fitness > BIG ? 1.1f : 2f);  // make the relative space more dense near overflow space
            }
        }
        else
        {
            if (lower == null)
            {
                r.fitness = higher.r.fitness * (higher.r.fitness < SMALL ? 0.95f : 0.5f); // make the relative space more dense near zero
            }
            else
            {
                r.fitness = (higher.r.fitness - lower.r.fitness) / 2f;
            }
        }
        seq.add(R);
        return r;
    }

    
    
}
