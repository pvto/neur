
package neur.learning.learner;

import java.util.List;
import neur.MLP;
import neur.data.TrainMode;
import neur.data.TrainingSet;
import neur.data.Trainres;
import static neur.util.Arrf.flatten;

/** 
 *   A stochastic learning algorithm. Call on this iteratively to suggest new MLPs within a given topology. 
 * Uses a list of taboo boxes for tested solutions, maintaining a best-of generated solutions record 
 * in the public member @see #best.
 * 
 *   This is quite like the first phase of a tabu search in 
 * [Sexton RS, Alidaee B, Dorsey RE, Johnson JD. Global optimization for artificial neural networks: a tabu search
 *   application. European Journal of Operational Research 106(1998), 570–84.] 
 * as cited in 
 * [R. Martí, A. El-Fallahi. Multilayer neural networks: an experimental evaluation of on-line training methods. 
 *   Computers & Operations Research 31(2004), 1491–1513.]
 * 
 * 
 *   learnEpoch() can be invoked iteratively to find new solutions, but will not find any after some number of
 * of trials.
 * 
 *   You should use this in combination with a depth-first learning algorithm for developing found suggestions.
 * 
 * 
 * 
 * @author Paavo Toivanen paavo.v.toivanen@gmail.com
 */
public class TabooBoxSearch {

    /** the size of margin (margin as in layout) added to a taboo box - should lie within range (-0.5,0.5) for 
     * MLPs (within the range of connection weight values of newly created networks)  */
    public float THRESHOLD = 0.1f / 2.0f;
    
    /** the best solution so far */
    public MLP best = null;
    

    // 
    //  If old.space1 is equal to old.space2, then the expected number of computations inside this routine is 
    //  1*(1-(THRESHOLD*2/RANGE)) + 2*(THRESHOLD*2/RANGE)*(1-(THRESHOLD*2/RANGE)) + 3*(THRESHOLD*2/RANGE)^2*(1-(THRESHOLD*2/RANGE)) + ...
    //  which is ~ 1.2, when THRESHOLD/RANGE = 1/20  
    //
    private boolean within(Taboo t, Taboo old)
    {
        float[] A = t.space1,
                X = old.space1,
                Y = old.space2;
        for (int k = 0; k < A.length; k++)
        {
            float x = X[k],
                    y = Y[k],
                    a = A[k];
            if (x > y)
            {
                if (a > x + THRESHOLD || a < y - THRESHOLD)
                    return false;
            }
            else
            {
                if (a < x - THRESHOLD || a > y + THRESHOLD)
                    return false;
            }
        }
        return true;
    }

    
    public static class Taboo 
    {
        public float error = 0f;
        public float[] 
                space1,
                space2;
    }

    
    public float leastError = Float.MAX_VALUE;
    BackPropagation ebp = new BackPropagation();
    
    
    public boolean learnEpoch(MLP n, TrainingSet teach, TrainingSet valid, List<Taboo> taboos)
    {
        
        n = n.newNetwork(); // create a new solution with random weights

        Taboo maytaboo = new Taboo();
        maytaboo.space1 = flatten(n.weights);
        boolean within = false;
        for(Taboo old : taboos)
        {
            if (within(maytaboo, old))
            {
                return false;
                // ok, don't add a new taboo; wait for the intensification phase of learning
            }
        }
        if (!within)
            taboos.add(maytaboo);
        
        MLP better = n.copy();
        int maxGradIters = (int)(Math.random() * 4);
        for (int i = 0; i < maxGradIters; i++)
        {
            teach.trainEpoch(better, ebp, TrainMode.SUPERVISED_ONLINE_MODE, new Object[]{0.1f});    // too big a learning coefficient will lead to locking into local minima
        }
        Trainres r = valid.trainEpoch(better, ebp, TrainMode.SUPERVISED_NO_TRAINING, new Object[]{0.1f});
        if (Float.isNaN(r.variance))
        {   // suggested nw diverged after gradient learning - insert only a point into taboo space
            maytaboo.space2 = maytaboo.space1;
            r = valid.trainEpoch(n, ebp, TrainMode.SUPERVISED_NO_TRAINING, new Object[]{0.1f});
            maytaboo.error = r.variance;
        }
        else
        {   // taboo box found using gradient learning - insert the bos into taboo space
            maytaboo.space2 = flatten(better.weights);
            maytaboo.error = r.variance;
        }
        
        
        if (leastError > maytaboo.error)
        {
            leastError = maytaboo.error;
            best = better;
            return true;
        }
        return false;   // didn't find anything spectacular this round
    }

}
