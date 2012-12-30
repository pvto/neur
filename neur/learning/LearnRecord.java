
package neur.learning;

import java.io.Serializable;
import neur.NeuralNetwork;
import neur.data.Trainres;

/** A record from one teaching session (session is a sequence of teaching epochs on a unitary network.)
 *
 * @author Paavo Toivanen
 */
public class LearnRecord<T extends NeuralNetwork> implements Serializable {

    public LearnParams p;
    public int okBest;
    /** the best achieved network for a*/
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
    /** fitness is calculated by an evaluator of the fitness of this result */
    public float fitness;
    /** the number of learning records summed within */
    public int rounds;

    
    
    public float vldCorrRate()
    {
        return vldsetCorrect / (float)p.D.TRAIN.set.size();
    }
    
    public float testCorrRate()
    {
        return testsetCorrect / (float)p.D.TEST.set.size();
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
