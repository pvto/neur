
package neur.learning;

import java.io.Serializable;
import neur.NeuralNetwork;
import neur.data.Trainres;

/**
 *
 * @author Paavo Toivanen
 */
public class LearnRec<T extends NeuralNetwork> implements Serializable {

    public LearnParams p;

    public int okBest;
    public T best;
    public int rndBestis;
    
    long start = 0;
    public long rndSearchDur;
    public long totalDur;
    
    public int i = 0;

    public Trainres lastTrainres;
    
    public float[][] out;
    
    public float[] sd;
    
    public int imprvEpochs = 0;
    
    public int testsetCorrect = 0,
            vldsetCorrect = 0;


    public float vldCorrRate()
    {
        return vldsetCorrect / (float)p.D.T.set.size();
    }
    
    public float testCorrRate()
    {
        return testsetCorrect / (float)p.D.V.set.size();
    }

    public void computeFinalResults()
    {
        out = new float[p.D.data.length][p.NNW_DIMS[p.NNW_DIMS.length - 1]];
        for (int j = 0; j < out.length; j++)
        {
            float[] tre = best.feedf(p.D.data[j][0]);
            for (int i = 0; i < tre.length; i++)
            {
                out[j][i] = tre[i];
            }
        }        
    }
   
    
}
