package neur.data.slice;

import java.util.BitSet;
import neur.data.TrainingSet;

/**
 *
 * @author Paavo Toivanen
 */
public class RandomSlicing {
    
    /**
     * @param istest this routine sets bits corresponding to test set entries by random
     * @return {TRAIN, TEST}
     */
    public static TrainingSet[] slice(int testSetSize, float[][][] data, BitSet istest)
    {
        TrainingSet[] ret = new TrainingSet[]{new TrainingSet(), new TrainingSet()};
        while(ret[1].set.size() < testSetSize)
        {
            int rnd = (int)(Math.random() * data.length);
            if (istest.get(rnd))
                continue;
            istest.set(rnd);
            ret[1].set.add(data[rnd]);
        }
        for (int i = 0; i < ret.length; i++)
        {
            if (!istest.get(i))
                ret[0].set.add(data[i]);
        }
        return ret;
    }
}
