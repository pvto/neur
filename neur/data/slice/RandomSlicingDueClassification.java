
package neur.data.slice;

import annot.Stateless;
import java.util.BitSet;
import neur.data.TrainingSet;
import static neur.util.Arrf.*;


/**Creates a slicing of a Dataset into a training set (TRAIN) and a test set (TEST), 
 * both having stochastically the same distribution of classes.
 * 
 * (Classes here are the output classes of a classification task.)
 * 
 *
 * @author Paavo Toivanen
 */
@Stateless
public class RandomSlicingDueClassification {

    /**
     * @param istest this routine sets bits corresponding to test set entries
     * @return {TRAIN, TEST}
     */
    public static TrainingSet[] slice(int testSetSize, float[][][] data, BitSet istest)
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
            int trials = 0;
            for(;;)
            {
                if (trials++ > data.length * 20)
                    throw new RuntimeException("Can't slice data for classification");
                k = (int)(Math.random() * data.length);
                if (data[k][1][j] != classItem[0])
                    continue;
                if (istest.get(k))
                    continue;
                
                istest.set(k);
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
            if (!istest.get(k))
                ret[0].addSample(data[k]);

        return ret;
    }

}
