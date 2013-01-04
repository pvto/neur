
package neur.data.slice;

import annot.Stateless;
import java.util.HashSet;
import java.util.Set;
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
     * @return {TRAIN, TEST}
     */
    public static TrainingSet[] slice(int testSetSize, float[][][] data)
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
