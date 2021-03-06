
package neur.learning.clf;

import java.io.Serializable;
import neur.learning.Classifier;
import neur.util.Arrf;


public class Fast1OfNClassifier extends Classifier implements Serializable
{
    //private float THRSH = 0f;
    @Override
    public int correctness(float[][] sample, float[] result)
    {
        return (sample[1][Arrf.indexOfGreatest(result)] > 0f ? 1 : 0);
    }

    @Override
    public final int TYPE() {
        return Classifier.ClassifierType.CHOOSE_1_OF_n;
    }

    @Override
    public float[] normalisedClassification(float[][] d, float[] res)
    {
        float[] ret = new float[res.length];
        int winner = Arrf.indexOfGreatest(res);
        for (int i = 0; i < ret.length; i++)
        {
            if (winner == i)
                ret[i] = 1f;
            else
                ret[i] = 0;                
        }
        return ret;
    }
}