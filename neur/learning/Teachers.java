
package neur.learning;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import neur.MLP;
import neur.NeuralNetwork;
import neur.data.TrainMode;
import neur.data.Trainres;
import neur.learning.LearnRecord.Item;
import neur.learning.learner.BackPropagation;
import neur.learning.learner.MonteCarloSearch;
import neur.learning.learner.TabooBoxSearch;
import neur.learning.learner.TabooBoxSearch.Taboo;
import neur.util.Log;
import neur.util.visuals.MLPVisualisation;

public class Teachers {

    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void monteCarloAndIntensification(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        monteCarlo(p, r, log);
        intensification(p, r, log, (T)r.best);  // erroneous
        r.aggregateResults();
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void tabooBoxAndIntensification(LearnParams<T,U> p, final LearnRecord r, Log log)
    {
        LearnRecord.Item stochItem = null;
        T nnw;
        if (p.STOCHASTIC_SEARCH_ITERS > 0)
        {
            stochItem = tabooBox(p, r, log);
            nnw = (T)r.best;
        }
        else
        {
            nnw = p.nnw.copy();
        }
        LearnRecord.Item item = intensification(p, r, log, nnw);
        if (item == null && stochItem != null)
        {   // put stochastic result back...
            r.items.add(stochItem);
        }
        else if (stochItem != null)
        {
            if (r.bestItem == stochItem)
            {
                if (item != null)
                    r.items.remove(item);
                r.items.add(stochItem);
            }
            else
            {
                item.totalStochasticIterations = stochItem.totalStochasticIterations;
                item.bestStochasticIteration = stochItem.bestStochasticIteration;
                item.stochSearchDuration = stochItem.stochSearchDuration;
            }
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
        LearnRecord.Item item = r.createItem();
        long time = System.currentTimeMillis();
        for(int i = 0; i < p.STOCHASTIC_SEARCH_ITERS; i++)
        {
            r.current = TS.best;
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
        log.log("TABOO_SRC tok="+item.testsetCorrect +" lok=" + item.trainsetCorrect + "  "+ time+"ms " + item.bestStochasticIteration+"("+p.STOCHASTIC_SEARCH_ITERS+") var="+TS.leastError);
        return item;
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm>
            
            LearnRecord.Item intensification(LearnParams<T,U> p, LearnRecord r, Log log, T nnw)
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
            r.current = nnw;
            if (bestItem != null && 
                    (bestItem.bestIteration + p.DIVERGENCE_PRESUMED < totalIterations
                    || (bestItem == null || bestItem.bestIteration == 0) && totalIterations > p.DIVERGENCE_PRESUMED / 4)
                )
            {
                break;
            }
            LearnRecord.Item item = r.createItem();
            Trainres tresp = p.D.TRAIN.trainEpoch(nnw, p.L, p.MODE, new Object[]{k, p.MINIBATCH_SIZE});
            Trainres tres = p.D.TEST.trainEpoch(nnw, new BackPropagation(), TrainMode.SUPERVISED_NO_TRAINING, new Object[]{k, p.MINIBATCH_SIZE});
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
                    k *= (0.5f + Math.random()*0.5f);
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
                //System.out.println(String.format("before %s a %s  %d", Arrays.toString(tresp.errorTerms), Arrays.toString(tres.errorTerms), totalIterations));
            }
            sdPrev = tres.mse;
        }
        long time = System.currentTimeMillis() - start;
        if (bestItem != null)
        {
            r.items.add(bestItem);
            bestItem.totalIterations = totalIterations + 1;
            bestItem.searchDuration = time;
            log.log("EBP tok= %d lok= %d  %dms, %d lrn-epochs", bestItem.testsetCorrect, bestItem.trainsetCorrect, time, totalIterations);
        }
        return bestItem;
    }
    
    

//    private static void logSuccess(Log log, int vldCorrect, int testCorrect, TrainingSet T, TrainingSet U)
//    {
//        log.log("trng set; corr  %d/%d (%.2f)%%", vldCorrect, T.set.size(), (vldCorrect*100.0/T.set.size()));
//        log.log("test set; corr  %d/%d (%.2f)%%", testCorrect, U.set.size(), (testCorrect*100.0/U.set.size()));
//    }
}
