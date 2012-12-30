
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
        Systematic,
        RandomDueClassification,
    }
    public String DATA_GEN = "x";
    public int DATASET = 1;
    public String SAMPLE = "A";
    public float[][][] data;
    public volatile TrainingSet TRAIN;
    public volatile TrainingSet TEST;
    public volatile TrainingSet ALL;
 
    public int 
            RANDOM_PARTITION_MAX_DISCARD = 100,
            SET_SIZE_TO_PARAM_CLASS_COUNT_MIN_RATIO = 4
            ;
    
    public int systematicOffset = 0;
    
    public void initTestVldSets(int testSetSize, final Slicing slicing)
    {
        if (ALL == null)
        {
            ALL = new TrainingSet();
            for (int i = 0; i < data.length; i++)
                ALL.addSample(data[i]);
        }
        if (slicing == Slicing.TakeLast)
        {
            int slice = data.length - testSetSize;   // partition into test set and teaching set

            TRAIN = new TrainingSet();
            TEST = new TrainingSet();
            for(int i = 0; i < data.length; i++)
                if (i < slice)
                    TRAIN.addSample(data[i]);
                else
                    TEST.addSample(data[i]);
        }
        else if (slicing == Slicing.Systematic)
        {
            int upperBoundary = (systematicOffset + testSetSize) % data.length;
            TRAIN = new TrainingSet();
            TEST = new TrainingSet();
            for(int i = 0; i < data.length; i++)
                if (i < upperBoundary && (systematicOffset > upperBoundary || i >= systematicOffset))
                    TEST.addSample(data[i]);
                else
                    TRAIN.addSample(data[i]);
            systematicOffset = ++systematicOffset % data.length;
        }
        else if (slicing == Slicing.RandomDueClassification)
        {
            TrainingSet[] t_v = randomSlicingDueClassification(testSetSize);
            TRAIN = t_v[0];  TEST = t_v[1];
        }
    }

//    /** this gives an estimate for the nearness of the distribution of given test set and the data in this dataset:
//    *  1) with discrete parameters p of T, the class distribution in T.TEST[p] is not too far from that of T.ALL[p]
//    *  2) with continuous parameters p of T, the mean of T.TEST[p] is not too far from that of T.ALL[p] 
//    **/
//    public float distanceFromCurrentDistribution(TrainingSet T)
//    {
//        float[][] exemplar = data[0];
//        float confidence = 0.95f;
//        float grandVariance = 0f,
//                boundary = 0f;
//        for (int j = 0; j < exemplar.length; j++)
//            for (int i = 0; i < exemplar[j].length; i++)
//            {
//                float[] col = col(data, j, i),
//                        tcol = col(T.set, j, i);
//                float[][] classes = classes(col);
//                
//                if (classes.length < Math.min(T.set.size(), data.length / SET_SIZE_TO_PARAM_CLASS_COUNT_MIN_RATIO))
//                    // given that the count of classes could fit into the TrainingSet
//                {   // assume discrete parameters, check for 
//                    float[][] tclasses = classes(tcol);
//                    if (tclasses.length < classes.length)
//                        grandVariance += sum(col(classes,0));
//                    for (int k = 0; k < tclasses.length; k++)
//                    {
//                        float dev = Math.abs(tclasses[k][1]*data.length/T.set.size() - classes[k][1]);
//                        grandVariance += dev * dev;
//                    }
//                    continue;
//                }
//                float min = min(col),
//                        max = max(col);
//                float mean = evdist_mean(col);
//                float tmean = evdist_mean(tcol);
//                float distanceNormalised = Math.abs(tmean - mean) / (max - min);
//                grandVariance += distanceNormalised * distanceNormalised;
//            }
//        return grandVariance;
//    }

    
    
    private TrainingSet[] randomSlicingDueClassification(int testSetSize)
    {
        TrainingSet[] ret = new TrainingSet[]{new TrainingSet(), new TrainingSet()};
        
        float[] item = data[0][1];
        float[][][] classDistributions = new float[item.length][][];
        float[][] testSetDs = new float[item.length][];
        for(int j = 0; j < item.length; j++)
        {
            classDistributions[j] = classes(col(data, 1, j));
            float classSum = nonZero(sum(col(classDistributions[j], 1)), 1f);
            testSetDs[j] = new float[classDistributions[j].length];
            for (int i = 0; i < testSetDs[j].length; i++)
            {
                testSetDs[j][i] = classDistributions[j][i][1] / classSum * (float)testSetSize / (float)data.length;
            }
        }
        // random pickup from classes
        float[] thresholds = flatten(testSetDs); // maximums for item pickup from classes
        int offset = (int)(Math.random() * (float)thresholds.length);
        int phase = 0;
        int[] phases = copy(0, thresholds.length);
        
        int[] consumed = new int[thresholds.length];
        Set<Integer> test = new HashSet<Integer>();
        int k;
        while(ret[1].set.size() < testSetSize)
        {
            if (allEqual(phases, phase+1))
            {   // lever the threshold uniformly, if required 
                thresholds = add(thresholds, 1f);
                phase++;
            }
            while (consumed[offset] >= thresholds[offset])
            {
                offset = (int)(Math.random() * (float)thresholds.length);
            }
            int[] ji = deflatInd(testSetDs, offset);
            int j = ji[0],  i = ji[1];
            float[] classItem = classDistributions[j][i];
            for(;;)
            {
                k = (int)(Math.random() * data.length);
                if (data[k][1][j] != classItem[0])
                    continue;
                if (test.contains(k))
                    continue;
                
                test.add(k);
                ret[1].addSample(data[k]);
                
                for (int x = 0; x < classDistributions.length; x++)
                {
                    if (x == j)
                    {
                        consumed[offset]++;
                        if (phases[offset] == phase && consumed[offset] > thresholds[offset])
                            phases[offset]++;
                    }
                    else for (int y = 0; y < classDistributions[x].length; y++)
                    {
                        if (classDistributions[x][y][0] == data[k][1][x])
                        {
                            int ind = flatInd(testSetDs, new int[]{x,y});
                            consumed[ind]++;
                            if (phases[ind] == phase && consumed[ind] > thresholds[ind])
                                phases[ind]++;
                            break;
                        }
                    }
                }
                break;
            }
            
            offset = (int)(Math.random() * (float)thresholds.length);
        }
        
        for(k = 0; k < data.length; k++)
            if (!test.contains(k))
                ret[0].addSample(data[k]);
        
        return ret;
    }
    
}
