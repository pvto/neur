
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
        wrapup(p, r);            
        logSuccess(log, r.vldsetCorrect, r.testsetCorrect, p.D.TRAIN, p.D.TEST);
        
        if (r.testsetCorrect == p.D.TEST.set.size() && r.vldsetCorrect == p.D.TRAIN.set.size())
        {
            return;
        }
        intensification(p, r, log);
        wrapup(p, r);
        logSuccess(log, r.vldsetCorrect, r.testsetCorrect, p.D.TRAIN, p.D.TEST);        
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void tabooBoxAndIntensification(LearnParams<MLP,U> p, LearnRecord r, Log log)
    {
        if (p.RANDOM_SEARCH_ITERS > 0)
        {
            tabooBox(p, r, log);
            wrapup(p, r);            
            logSuccess(log, r.vldsetCorrect, r.testsetCorrect, p.D.TRAIN, p.D.TEST);

            if (r.testsetCorrect == p.D.TEST.set.size() && r.vldsetCorrect == p.D.TRAIN.set.size())
            {
                return;
            }
        }
        intensification(p, r, log);
        wrapup(p, r);
        logSuccess(log, r.vldsetCorrect, r.testsetCorrect, p.D.TRAIN, p.D.TEST);        
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm> 
            
            void monteCarlo(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        init(r);
        new MonteCarloSearch().learn(p, r);
        r.rndSearchDur = System.currentTimeMillis() - r.start;
        log.log("RND_SRC in "+r.rndSearchDur+"ms");
    }
    
    public <U extends LearningAlgorithm> 
            
            void tabooBox(LearnParams<MLP,U> p, LearnRecord r, Log log)
    {
        init(r);
        MLP nnw = p.nnw;
        MLP best;
        TabooBoxSearch TS = new TabooBoxSearch();
        List<Taboo> taboos = new ArrayList<Taboo>();
        
        long time = System.currentTimeMillis();
        
        for(int i = 0; i < p.RANDOM_SEARCH_ITERS; i++)
        {
            if (TS.learnEpoch(p.nnw, p.D.TEST, p.D.TRAIN, taboos))
            {
                r.rndBestis = i;
            }
//            System.out.println(i+" ok="+p.CF.correctCount(p.D.V, TS.best) + " var="+TS.leastError);
        }
        r.best = TS.best;
        r.okBest = +p.CF.correctCount(p.D.TEST, TS.best);
        log.log(r.rndBestis+"("+p.RANDOM_SEARCH_ITERS+") ok="+r.okBest + " var="+TS.leastError);
        r.rndSearchDur = System.currentTimeMillis() - r.start;
        log.log("TABOO_SRC in "+r.rndSearchDur+"ms");
    }
    
    
    public <T extends NeuralNetwork, U extends LearningAlgorithm>
            
            void intensification(LearnParams<T,U> p, LearnRecord r, Log log)
    {
        p.L.clear();        
        init(r);        
        T nnw = (r.best != null) ? (T)r.best.copy() : (T)p.nnw.copy();
        Trainres resBest = new Trainres();
        resBest.variance = Float.MAX_VALUE;
        float sdPrev = Float.MAX_VALUE;
        int sdIncreasing = 0;
        float learRok= 0.05f,
                k = p.LEARNING_RATE_COEF;

        for (; r.i < p.TEACH_MAX_ITERS; r.i++)
        {
            if (r.testsetCorrect == p.D.TEST.set.size() && sdIncreasing >= p.TEACH_TARRY_NOT_CONVERGING)
            {
                break;
            }
            r.lastTrainres = p.D.TRAIN.trainEpoch(nnw, p.L, p.MODE, new Object[]{k});
            
            if (Float.isNaN(r.lastTrainres.variance))
            {
                if (r.best != null)
                    nnw = r.best.copy();
                k = learRok * 0.8f;
                continue;
            }
           
            r.testsetCorrect = p.CF.correctCount(p.D.TEST, nnw);
            if (r.testsetCorrect > r.okBest || r.testsetCorrect == r.okBest && r.lastTrainres.variance < resBest.variance)
            {
                r.imprvEpochs ++;
                r.okBest = r.testsetCorrect;
                r.best = nnw.copy();
                log.log("ok=%d sd=%.5f lrate=%.5f it=%d", r.testsetCorrect, r.lastTrainres.variance, k, r.i);
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
            if (r.lastTrainres.variance < p.TRG_ERR)
            {
                log.log("sd target %.4f reached, it=%d", p.TRG_ERR, r.i);
                break;
            }
            else
            if (r.lastTrainres.variance >= sdPrev)
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
            if (r.lastTrainres.variance < resBest.variance)
            {
                resBest = r.lastTrainres;
                sdIncreasing = 0;
            }
            sdPrev = r.lastTrainres.variance;
        }
        r.totalDur = (System.currentTimeMillis()-r.start);
        log.log("done, %d ms, %d lrn-epochs", r.totalDur, r.i);
        
        if (r.best == null)
        {
            log.log("no result - try again");
            return;
        }

        r.lastTrainres = resBest;
    }
    
    
    
    private void init(LearnRecord r)
    {
        if (r.start == 0L)
        {
            r.start = System.currentTimeMillis();
            r.okBest = 0;
            r.lastTrainres = new Trainres();
            r.rndBestis = 0;
            r.i = 0;
            r.imprvEpochs = 0;
        }
    }
    
    private <T extends NeuralNetwork, U extends LearningAlgorithm> 
            void wrapup(LearnParams<T,U> p, LearnRecord r)
    {
        r.vldsetCorrect = p.CF.correctCount(p.D.TRAIN, r.best);
        r.testsetCorrect = p.CF.correctCount(p.D.TEST, r.best);
        r.computeFinalResults();
        r.sd = new float[p.NNW_DIMS[p.NNW_DIMS.length - 1]];
        r.sd[0] = Arrf.evdist_sd(Arrf.subtract(Arrf.col(p.D.data, 1,0), Arrf.col(r.out, 0)));
        r.sd[1] = Arrf.evdist_sd(Arrf.subtract(Arrf.col(p.D.data, 1,1), Arrf.col(r.out, 1)));
    }
    

    private static void logSuccess(Log log, int vldCorrect, int testCorrect, TrainingSet T, TrainingSet U)
    {
        log.log("trng set; corr  %d/%d (%.2f)%%", vldCorrect, T.set.size(), (vldCorrect*100.0/T.set.size()));
        log.log("test set; corr  %d/%d (%.2f)%%", testCorrect, U.set.size(), (testCorrect*100.0/U.set.size()));
    }
}
