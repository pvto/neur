
package neur.data.slice;

import java.util.BitSet;
import neur.data.TrainingSet;
 
/** Creates a slicing of a data set into a training set (TRAIN) and a test set (TEST) so that upon two sequential calls
 * the created test set differs by one entry from the test set that was created earlier.
 *
 * If test set size is more than one (which is discouraged), test set items are always neighboring items in the original
 * data set.
 * 
 * @author Paavo Toivanen
 */
public class SystematicSlicing {

    public int systematicOffset = 0;
    
    /**
     * @param istest this routines sets bits corresponding to test set entries
     * @return {TRAIN, TEST}
     */
    public TrainingSet[] slice(int testSetSize, float[][][] data, BitSet istest)
    {
        TrainingSet[] ret = new TrainingSet[]{new TrainingSet(), new TrainingSet()};
        int upperBoundary = (systematicOffset + testSetSize) % data.length;
        for(int i = 0; i < data.length; i++)
        {
            if (i < upperBoundary && (systematicOffset > upperBoundary || i >= systematicOffset))
            {
                ret[1].addSample(data[i]);
                istest.set(i);
            }
            else
            {
                ret[0].addSample(data[i]);
            }
        }
        systematicOffset = ++systematicOffset % data.length;
        return ret;
    }
}
