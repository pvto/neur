
package neur.learning.clf;

import java.io.Serializable;
import neur.learning.Classifier;



public class Fast1Of2Classifier extends Classifier implements Serializable
{
    //private float THRSH = 0f;
    @Override
    public int correctness(float[][] sample, float[] result) {
        return (sample[1][0] > sample[1][1] && result[0]>result[1]//result[0] >= THRSH && result[1] < THRSH
                || sample[1][1] > sample[1][0] && result[1]>result[0]//result[1] >= THRSH && result[0] < THRSH
                ) ? 1 : 0;
    }

    @Override
    public final int TYPE() {
        return Classifier.ClassifierType.CHOOSE_1_OF_2;
    }

    @Override
    public float[] normalisedClassification(float[][] sample, float[] nnwOutput)
    {
        int ok = correctness(sample, nnwOutput);
        if (nnwOutput[0] > nnwOutput[1])
        {
            return new float[]{ok, ok-1f};
        }
        else if (nnwOutput[1] > nnwOutput[0])
        {
            return new float[]{ok-1f, ok};
        }
        return new float[]{0f,0f};
    }

}
