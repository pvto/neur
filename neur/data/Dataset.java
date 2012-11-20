
package neur.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import static neur.util.Arrf.*;

/** Data and how to name it; a division into test and training sets.
 *
 * @author Paavo Toivanen
 */
public class Dataset implements Serializable {

    public enum Slicing 
    {
        TakeLast,
        TakeRandom
    }
    public String DATA_GEN = "x";
    public int DATASET = 1;
    public String SAMPLE = "A";
    public float[][][] data;
    public TrainingSet T;
    public TrainingSet V;
 
    public int RANDOM_PARTITION_MAX_DISCARD = 100;
    
    
    public void initTestVldSets(int testSetSize, final Slicing slicing)
    {
        if (slicing == Slicing.TakeLast)
        {
            int slice = data.length - testSetSize;   // partition into test set and teaching set

            T = new TrainingSet();
            V = new TrainingSet();
            for(int i = 0; i < data.length; i++)
                if (i < slice)
                    T.addSample(data[i]);
                else
                    V.addSample(data[i]);
        }
        else if (slicing == Slicing.TakeRandom)
        {
            for(int i = 0; i < RANDOM_PARTITION_MAX_DISCARD; i++)
            {
                TrainingSet[] t_v = randomPartition(testSetSize);
                if (!testsetMeansWithinExpected(t_v[0]))
                    continue;
                T = t_v[0];  V = t_v[1];
                break;
            }
        }
    }

    
    public boolean testsetMeansWithinExpected(TrainingSet T)
    {
        boolean nok = false;
        float error = 0f;
        float[][] exemplar = data[0];
        for (int j = 0; j < exemplar.length; j++)
            for (int i = 0; i < exemplar[j].length; i++)
            {
                float[] col = col(data, j, i),
                        tcol = col(T.set, j, i);
                float mean = evdist_mean(col);
                float var = evdist_variance(col);
                float tmean = evdist_mean(tcol);
                float tvar = evdist_variance(tcol);
                error += (tmean - mean);
                 
            }
        return nok;
    }

    
    
    private TrainingSet[] randomPartition(int testSetSize)
    {
        TrainingSet[] ret = new TrainingSet[2];
        Set<Integer> test = new HashSet<Integer>();
        while(test.size() < testSetSize)
            test.add((int)(Math.random() * data.length));
        ret[0] = new TrainingSet();
        ret[1] = new TrainingSet();
        for(int i = 0; i < data.length; i++)
            if (!test.contains(i))
                ret[0].addSample(data[i]);
            else
                ret[1].addSample(data[i]);
        return ret;
    }
    
}
