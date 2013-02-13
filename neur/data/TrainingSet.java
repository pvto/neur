
package neur.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import neur.NeuralNetwork;
import neur.learning.LearningAlgorithm;
import neur.util.Arrf;

/**
 *
 * @author Paavo Toivanen
 */
public class TrainingSet implements Serializable {

    public volatile List
            <float[][]> set = new ArrayList<float[][]>();
    public int MAX_ITERATIONS = Integer.MAX_VALUE;
    
    
    public void addSample(float[][] sample)
    {
        set.add(sample);
    }

    
    
   
    public Trainres train(NeuralNetwork nnw, LearningAlgorithm L, TrainMode mode, Object[] params)
    {
        Trainres res = null;
        for(int i = MAX_ITERATIONS; i > 0; i--)
        {
            res = trainEpoch(nnw, L, mode, params);
        }
        return res;
    }
    
    
    
    public Trainres trainEpoch(NeuralNetwork nnw, LearningAlgorithm L, TrainMode mode, Object[] params)
    {
        Trainres res = new Trainres();
        if (mode.isSupervised())
        {
            res.errorTerms = new float[nnw.outv().length];
            res.mse = 0f;
        }
        int k = 0;
        for(float[][] d : set)
        {
            float[] outf = nnw.feedf(d[0]);
            if (mode.isSupervised())
            {
                float[] errors = Arrf.subtract(d[1], outf);
                int i = 0;
                for(float e : errors)
                {
                    res.errorTerms[i++] += e;
                    res.mse += e * e;
                }
                if (mode == TrainMode.SUPERVISED_ONLINE_MODE)
                {
                    L.learn(nnw, errors, params);
                }
                else if (mode == TrainMode.SUPERVISED_MINIBATCH)
                {
                    if (k == set.size() - 1 || k % (int)params[1] == 0)
                    {
                        float tmp = res.mse;
                        L.learn(nnw, errors, params);
                        res.mse = tmp + res.mse / (int)params[1];
                    }
                }
                k++;
            }
        }
        if (mode == TrainMode.SUPERVISED_BATCH_MODE)
        {
            res.errorTerms = Arrf.div(res.errorTerms, 2);
            L.learn(nnw, res.errorTerms, params);
            res.mse /= set.size();
        }
        L.finishEpoch(nnw, params);
        return res;
    }
}
