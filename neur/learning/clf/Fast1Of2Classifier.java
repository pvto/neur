
package neur.learning.clf;

import java.io.Serializable;
import neur.learning.Classifier;



public class Fast1Of2Classifier extends Classifier implements Serializable
{
    //private float THRSH = 0f;
    @Override
    public boolean correctness(float[][] sample, float[] result) {
        return (sample[1][0] > sample[1][1] && result[0]>result[1]//result[0] >= THRSH && result[1] < THRSH
                || sample[1][1] > sample[1][0] && result[1]>result[0]//result[1] >= THRSH && result[0] < THRSH
                );
    }

    @Override
    public final int TYPE() {
        return Classifier.ClassifierType.CHOOSE_1_OF_2;
    }

    @Override
    public float[] normalisedClassification(float[][] sample, float[] nnwOutput)
    {
        boolean ok = correctness(sample, nnwOutput);
        if (nnwOutput[0] > nnwOutput[1])
        {
            return new float[]{ok?1f:0f,ok?0f:1f};
        }
        else if (nnwOutput[1] > nnwOutput[0])
        {
            return new float[]{ok?0f:1f,ok?1f:0f};
        }
        return new float[]{0f,0f};
    }

}
