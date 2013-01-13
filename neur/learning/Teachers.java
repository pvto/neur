
package neur.learning;

import java.util.ArrayList;
import java.util.List;
import neur.MLP;
import neur.NeuralNetwork;
import neur.data.TrainingSet;
import neur.data.Trainres;
import neur.learning.learner.MonteCarloSearch;
import neur.learning.learner.TabooBoxSearch;
import neur.learning.learner.TabooBoxSearch.Taboo;
import neur.util.Arrf;
import neur.util.Log;

public class Teachers {

    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void monteCarloAndIntensification(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        monteCarlo(p, r, log);
        logSuccess(log, r.bestItem.trainsetCorrect, r.bestItem.testsetCorrect, p.D.TRAIN, p.D.TEST);
        
        if (r.bestItem.testsetCorrect == p.D.TEST.set.size() && r.bestItem.trainsetCorrect == p.D.TRAIN.set.size())
        {
            return;
        }
        intensification(p, r, log);
        r.aggregateResults();
        logSuccess(log, r.bestItem.trainsetCorrect, r.bestItem.testsetCorrect, p.D.TRAIN, p.D.TEST);        
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void tabooBoxAndIntensification(LearnParams<MLP,U> p, LearnRecord r, Log log)
    {
        if (p.RANDOM_SEARCH_ITERS > 0)
        {
            tabooBox(p, r, log);
            logSuccess(log, r.bestItem.trainsetCorrect, r.bestItem.testsetCorrect, p.D.TRAIN, p.D.TEST);

            if (r.bestItem.testsetCorrect == p.D.TEST.set.size() && r.bestItem.trainsetCorrect == p.D.TRAIN.set.size())
            {
                return;
            }
        }
        if (r.items.size() > 0)
            r.items.remove(r.items.size() - 1);
        intensification(p, r, log);
        r.aggregateResults();
        logSuccess(log, r.bestItem.trainsetCorrect, r.bestItem.testsetCorrect, p.D.TRAIN, p.D.TEST);        
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void monteCarlo(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        new MonteCarloSearch().learn(p, r);
        r.aggregateResults();
        log.log("RND_SRC in "+r.bestItem.searchDuration+"ms");
    }
    
    public <U extends LearningAlgorithm> 
            
            void tabooBox(LearnParams<MLP,U> p, LearnRecord r, Log log)
    {
        MLP nnw = p.nnw;
        MLP best;
        TabooBoxSearch TS = new TabooBoxSearch();
        List<Taboo> taboos = new ArrayList<Taboo>();
        LearnRecord.Item item = r.addItem();
        long time = System.currentTimeMillis();
        r.totalIterations += p.RANDOM_SEARCH_ITERS;
        for(int i = 0; i < p.RANDOM_SEARCH_ITERS; i++)
        {
            if (TS.learnEpoch(p.nnw, p.D.TEST, p.D.TRAIN, taboos))
            {
                r.lastUpdateIteration = i;
            }
//            System.out.println(i+" ok="+p.CF.correctCount(p.D.V, TS.best) + " var="+TS.leastError);
        }
        r.best = TS.best;
        item.finish(r.best);
        log.log(r.lastUpdateIteration+"("+p.RANDOM_SEARCH_ITERS+") ok="+r.bestItem.testsetCorrect + " var="+TS.leastError);
        time = System.currentTimeMillis() - time;
        r.duration += time;
        log.log("TABOO_SRC in "+time+"ms");
        r.aggregateResults();
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm>
            
            void intensification(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        p.L.clear();        
        T nnw = (r.best != null) ? (T)r.best.copy() : (T)p.nnw.copy();
        Trainres resBest = new Trainres();
        resBest.variance = Float.MAX_VALUE;
        float sdPrev = Float.MAX_VALUE;
        int sdIncreasing = 0;
        float learRok= 0.05f,
                k = p.LEARNING_RATE_COEF;

        for (; r.totalIterations < p.TEACH_MAX_ITERS; r.totalIterations++)
        {
            if (r.bestItem != null && r.bestItem.testsetCorrect == p.D.TEST.set.size() && sdIncreasing >= p.TEACH_TARRY_NOT_CONVERGING)
            {
                break;
            }
            LearnRecord.Item item = r.createItem();
            Trainres tres = p.D.TRAIN.trainEpoch(nnw, p.L, p.MODE, new Object[]{k});
            
            if (Float.isNaN(tres.variance))
            {
                if (r.best != null)
                    nnw = r.best.copy();
                k = learRok * 0.8f;
                continue;
            }
            item.finish(nnw);
            if (r.bestItem == item)
            {
                log.log("ok=%d error=%.5f lrate=%.5f it=%d", r.bestItem.testsetCorrect, Arrf.avg(item.error), k, r.totalIterations);
            }
            if (p.DYNAMIC_LEARNING_RATE)
            {
                if (sdIncreasing > 40)
                {
                    learRok = k;                    
                    k *= (0.5f + Math.random()*1.0f);
                    sdIncreasing -= 20;
                    log.log("lrate="+k);
                }
                k *= 0.9998f;
                //System.out.print(" ["+L.learningRateCoef+"] ");
            }
            if (tres.variance < p.TRG_ERR)
            {
                log.log("sd target %.4f reached, it=%d", p.TRG_ERR, r.totalIterations);
                break;
            }
            else
            if (tres.variance >= sdPrev)
            {
                if (++sdIncreasing > p.DIVERGENCE_PRESUMED)
                {
                    log.log("diverging...");
                    break;
                }
            }
            else
            {
                sdIncreasing--;
            }
            if (tres.variance < resBest.variance)
            {
                resBest = tres;
                sdIncreasing = 0;
            }
            sdPrev = tres.variance;
        }
        log.log("done, %d ms, %d lrn-epochs", r.duration, r.totalIterations);
        
        if (r.best == null)
        {
            log.log("no result - try again");
            return;
        }
        r.items.add(r.bestItem);
    }
    
    

    private static void logSuccess(Log log, int vldCorrect, int testCorrect, TrainingSet T, TrainingSet U)
    {
        log.log("trng set; corr  %d/%d (%.2f)%%", vldCorrect, T.set.size(), (vldCorrect*100.0/T.set.size()));
        log.log("test set; corr  %d/%d (%.2f)%%", testCorrect, U.set.size(), (testCorrect*100.0/U.set.size()));
    }
}
