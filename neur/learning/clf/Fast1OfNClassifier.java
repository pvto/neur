
package neur.learning.clf;

import java.io.Serializable;
import neur.learning.Classifier;


public class Fast1OfNClassifier extends Classifier implements Serializable
{
    //private float THRSH = 0f;
    @Override
    public boolean correctness(float[][] sample, float[] result)
    {
        int bestInd = 0;
        float best = result[0];
        for (int i = 1; i < result.length; i++)
        {
            if (result[i] > best)
            {
                best = result[bestInd = i];
            }
        }
        return (sample[1][bestInd] > 0f);
    }

    @Override
    public final int TYPE() {
        return Classifier.ClassifierType.CHOOSE_1_OF_n;
    }
}