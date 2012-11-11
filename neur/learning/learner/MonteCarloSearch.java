
package neur.learning.learner;

import neur.NeuralNetwork;
import neur.learning.LearnParams;
import neur.learning.LearnRec;
import neur.learning.LearningAlgorithm;

/** A stochastic brute force approach for finding solutions to a network learning task.
 *
 * @author Paavo Toivanen
 */
public class MonteCarloSearch {

    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void learn(LearnParams<T,U> p, LearnRec r)
    {
        T nnw = p.nnw;
        for(int i = 0; i < p.RANDOM_SEARCH_ITERS; i++)
        {
            nnw = nnw.newNetwork(p);
            for(int j = 0; j < 1 + Math.random()*7; j++)
            {
                r.lastTrainres = p.D.T.trainEpoch(nnw, p.L, p.MODE, new Object[]{p.LEARNING_RATE_COEF});
            }
            p.L.clear();      
            int testCorrect = p.CF.correctCount(p.D.V, nnw);
            int vldCorrect = p.CF.correctCount(p.D.T, nnw);
            if (testCorrect > r.okBest)
            {
                r.okBest = testCorrect;
                r.rndBestis = i;
                r.best = nnw.copy();
                if (r.okBest == p.D.V.set.size())
                {
                    break;
                }
            }
        }        
    }
}
