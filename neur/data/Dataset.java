
package neur.data;

import java.io.Serializable;
import java.util.BitSet;
import neur.data.slice.RandomSlicing;
import neur.data.slice.RandomSlicingDueClassification;
import neur.data.slice.SystematicSlicing;

/** Data and how to name it; a division into test and training sets.
 *
 * @author Paavo Toivanen
 */
public class Dataset implements Serializable {

    public enum Slicing implements Serializable
    {
        Systematic,
        RandomDueClassification,
        Random,
    }
    /** use this name to identify the generating data distribution of the data in this set */
    public String DATA_GEN = "x";
    /** numeric id for this dataset */
    public int DATASET = 1;
    /** use this to identify between different samples drawn from the same generating data distribution */
    public String SAMPLE = "A";
    public float[][][] data;
    public volatile TrainingSet TRAIN;
    public volatile TrainingSet TEST;
    public volatile BitSet istest;
//    public volatile TrainingSet ALL;
 
    
    
    private transient SystematicSlicing sysl;
    
    
    public void initTrain_Test_Sets(int testSetSize, final Slicing slicing)
    {

        TrainingSet[] t_v = null;
        istest = new BitSet(data.length);
        
        if (slicing == Slicing.Systematic)
        {
            if (sysl == null)
                sysl = new SystematicSlicing();
             t_v = sysl.slice(testSetSize, data, istest);
            
        }
        else if (slicing == Slicing.RandomDueClassification)
        {
            t_v = new RandomSlicingDueClassification().slice(testSetSize, data, istest);
        }
        else if (slicing == Slicing.Random)
        {
            t_v = new RandomSlicing().slice(testSetSize, data, istest);
        }
        TRAIN = t_v[0];  TEST = t_v[1];
    }


    public Dataset copy()
    {
        Dataset D = new Dataset();
        D.DATA_GEN = DATA_GEN;
        D.DATASET = DATASET;
        D.SAMPLE = SAMPLE;
        D.data = data;
        D.istest = (BitSet)istest.clone();
        return D;
    }
    

//    public int 
//            RANDOM_PARTITION_MAX_DISCARD = 100,
//            SET_SIZE_TO_PARAM_CLASS_COUNT_MIN_RATIO = 4
//            ;
//
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

    
    
}
