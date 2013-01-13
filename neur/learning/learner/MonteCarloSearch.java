
package neur.learning.learner;

import neur.NeuralNetwork;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.learning.LearnRecord.Item;
import neur.learning.LearningAlgorithm;

/** A stochastic brute force approach for finding solutions to a network learning task.
 *
 * @author Paavo Toivanen
 */
public class MonteCarloSearch {

    public int MAX_INDEPTH_LEARNING_ITERS = 8;
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
    void learn(LearnParams<T,U> p, LearnRecord r)
    {
        T nnw = p.nnw;
        Item record = r.createItem();
        Item bestRec = null;
        for(int i = 0; i < p.RANDOM_SEARCH_ITERS; i++)
        {
            nnw = nnw.newNetwork(p);
            if (p.L != null)
            {   // run some iterations of depth first learning, if enabled
                int iters = (int) (Math.random()*MAX_INDEPTH_LEARNING_ITERS);
                for(int j = 0; j < iters; j++)
                {
                    p.D.TRAIN.trainEpoch(nnw, p.L, p.MODE, new Object[]{p.LEARNING_RATE_COEF});
                }
                p.L.clear();
            }
            record.finish(nnw);
            if (bestRec == null || record.testsetCorrect > bestRec.testsetCorrect)
            {
                record.bestIteration = i;
                bestRec = record;
                r.best = nnw;
                if (bestRec.testsetCorrect == p.D.TEST.set.size())
                {
                    break;
                }
            }
        }
        r.items.add(bestRec);
    }
}
