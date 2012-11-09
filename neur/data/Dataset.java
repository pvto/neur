
package neur.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

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
            Set<Integer> test = new HashSet<Integer>();
            while(test.size() < testSetSize)
                test.add((int)(Math.random() * data.length));
            T = new TrainingSet();
            V = new TrainingSet();
            for(int i = 0; i < data.length; i++)
                if (!test.contains(i))
                    T.addSample(data[i]);
                else
                    V.addSample(data[i]);
        }
        
    }
}
