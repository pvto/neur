
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

}
