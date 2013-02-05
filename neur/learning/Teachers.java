
package neur.learning;

import java.util.ArrayList;
import java.util.List;
import neur.MLP;
import neur.NeuralNetwork;
import neur.data.Trainres;
import neur.learning.LearnRecord.Item;
import neur.learning.learner.MonteCarloSearch;
import neur.learning.learner.TabooBoxSearch;
import neur.learning.learner.TabooBoxSearch.Taboo;
import neur.util.Log;

public class Teachers {

    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void monteCarloAndIntensification(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        monteCarlo(p, r, log);
        intensification(p, r, log, (T)r.best);  // erroneous
        r.aggregateResults();
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void tabooBoxAndIntensification(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        LearnRecord.Item stochItem = null;
        T nnw;
        if (p.STOCHASTIC_SEARCH_ITERS > 0)
        {
            stochItem = tabooBox(p, r, log);
            r.items.remove(r.items.size() - 1);
            nnw = (T)r.best;
        }
        else
        {
            nnw = p.nnw.copy();
        }
        if (!intensification(p, r, log, nnw) && stochItem != null)
        {   // put stochastic result back...
            r.items.add(stochItem);
        }
        else {
            if (r.bestItem == stochItem)
            {
                if (r.items.size() > 0)
                    r.items.remove(r.items.size() - 1);
                r.items.add(stochItem);
            }
        }
        if (stochItem != null)
        {
            LearnRecord.Item item = (LearnRecord.Item) r.items.get(r.items.size() - 1);
            item.totalStochasticIterations = stochItem.totalStochasticIterations;
            item.bestStochasticIteration = stochItem.bestStochasticIteration;
            item.stochSearchDuration = stochItem.stochSearchDuration;
        }
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void monteCarlo(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        new MonteCarloSearch().learn(p, r);
        r.aggregateResults();
        log.log("RND_SRC in "+r.bestItem.searchDuration+"ms");
    }
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            Item tabooBox(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        T nnw = p.nnw;
        T best;
        TabooBoxSearch TS = new TabooBoxSearch();
        List<Taboo> taboos = new ArrayList<Taboo>();
        LearnRecord.Item item = r.addItem();
        long time = System.currentTimeMillis();
        for(int i = 0; i < p.STOCHASTIC_SEARCH_ITERS; i++)
        {
            item.totalStochasticIterations = i + 1;
            if (TS.learnEpoch((MLP)p.nnw, p.D.TEST, p.D.TRAIN, taboos))
            {
                item.bestStochasticIteration = i;
            }
        }
        r.best = TS.best;
        item.finish(TS.best);
        time = System.currentTimeMillis() - time;
        item.stochSearchDuration = time;
        log.log("TABOO_SRC ok="+item.testsetCorrect +"  "+ time+"ms " + item.bestStochasticIteration+"("+p.STOCHASTIC_SEARCH_ITERS+") var="+TS.leastError);
        return item;
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm>
            
            boolean intensification(LearnParams<T,U> p, LearnRecord r, Log log, T nnw)
    {
        p.L.clear();        
        Trainres resBest = new Trainres();
        resBest.mse = Float.MAX_VALUE;
        float sdPrev = Float.MAX_VALUE;
        int sdIncreasing = 0;
        float learRok= 0.05f,
                k = p.LEARNING_RATE_COEF;
        int totalIterations = 0;
        long start = System.currentTimeMillis();
        LearnRecord.Item bestItem = null;
        for (; totalIterations < p.TEACH_MAX_ITERS; totalIterations++)
        {
            if (bestItem != null && 
                    (bestItem.bestIteration + p.DIVERGENCE_PRESUMED < totalIterations
                    || (bestItem == null || bestItem.bestIteration == 0) && totalIterations > p.DIVERGENCE_PRESUMED / 4)
                )
            {
                break;
            }
            LearnRecord.Item item = r.createItem();
            Trainres tres = p.D.TRAIN.trainEpoch(nnw, p.L, p.MODE, new Object[]{k});
            
            if (Float.isNaN(tres.mse))
            {
                if (r.best != null)
                    nnw = r.best.copy();
                k = learRok * 0.8f;
                continue;
            }
            item.finish(nnw);
            if (bestItem == null || item.fitness > bestItem.fitness)
            {
                bestItem = item;
                item.bestIteration = totalIterations;
                log.log("ok=%d/%d lok=%d/%d  error=%.5f lrate=%.5f it=%d", bestItem.testsetCorrect, p.D.TEST.set.size(),
                        bestItem.trainsetCorrect, p.D.TRAIN.set.size(),
                        tres.mse, k, totalIterations);
            }
            if (p.DYNAMIC_LEARNING_RATE)
            {
                if (sdIncreasing > 40)
                {
                    learRok = k;                    
                    k *= (0.5f + Math.random()*1.0f);
                    sdIncreasing -= 20;
//                    log.log("lrate="+k);
                }
                k *= 0.9998f;
                //System.out.print(" ["+L.learningRateCoef+"] ");
            }
            if (tres.mse < p.TRG_ERR)
            {
                log.log("sd target %.4f reached, it=%d", p.TRG_ERR, totalIterations);
                break;
            }
            else
            if (tres.mse >= sdPrev)
            {
                sdIncreasing++;
            }
            else
            {
                sdIncreasing--;
            }
            if (tres.mse < resBest.mse)
            {
                resBest = tres;
                sdIncreasing = 0;
            }
            sdPrev = tres.mse;
        }
        long time = System.currentTimeMillis() - start;
        log.log("EBP %dms, %d lrn-epochs", time, totalIterations);
        if (bestItem != null)
        {
            r.items.add(bestItem);
            bestItem.totalIterations = totalIterations + 1;
            bestItem.searchDuration += time;
        }
        return bestItem != null;
    }
    
    

//    private static void logSuccess(Log log, int vldCorrect, int testCorrect, TrainingSet T, TrainingSet U)
//    {
//        log.log("trng set; corr  %d/%d (%.2f)%%", vldCorrect, T.set.size(), (vldCorrect*100.0/T.set.size()));
//        log.log("test set; corr  %d/%d (%.2f)%%", testCorrect, U.set.size(), (testCorrect*100.0/U.set.size()));
//    }
}
