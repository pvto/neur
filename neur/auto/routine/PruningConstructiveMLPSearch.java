
package neur.auto.routine;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import neur.MLP;
import neur.auto.NNSearchSpace;
import neur.auto.TopologyFinding;
import neur.auto.TopologySearchRoutine;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.learning.Teachers;
import neur.util.Log;

/**
 *
 * @author Paavo Toivanen
 */
public class PruningConstructiveMLPSearch implements TopologySearchRoutine {

    private static Log log = Log.create.chained(Log.cout, Log.file.bind("pru-srch.log"));
    
    public float nearnessRule = 2f;
    
    public int rounds = 1;
    
    RelativeMLPFitnessComparator c = new RelativeMLPFitnessComparator();
    
    private int count = 0;
    private class _RecursiveAction extends RecursiveAction
    {
        _RecursiveAction(int low, int high, LearnParams p, NNSearchSpace s, TopologyFinding res)
        {
            this.low = low;  this.high = high;  this.p = p;  this.s = s;  this.res = res;  }
        int low, high;
        LearnParams p;
        NNSearchSpace s;
        TopologyFinding res;
        
        @Override
        protected void compute()
        {
            log.log("------"+low+"--"+high+"-----count="+count);
            if (low == high)
            {
                p = s.resolveTopologyFromFlattenedIndex(p, low);
                teachOne();
                count++;
                return;
            }
            List<RecursiveAction> L = new LinkedList<RecursiveAction>();
            int mid = low + (int)((high-low+1) * Math.random());
            L.add(new _RecursiveAction(low, mid, p, s, res));
            if (mid+1 <= high)
                L.add(new _RecursiveAction(mid+1, high, p, s, res));
            invokeAll(L);
        }
        
        private void teachOne()
        {
            for(int i = 0; i < res.records.length; i++)
            {
                if (res.records[i] != null)
                {
                    if (near(p, res.records[i].res.p, s, res))
                    {
                        log.log("------near %d", low);
                        return;
                    }
                }
            }

            LearnParams p = this.p.copy();
            LearnRecord<MLP> rec = new LearnRecord<MLP>(p);
            int check = 0;
            while(rec.p.getNumberOfPendingTrainingSets(rec) > 0)
            {
                rec.p.nnw = new MLP(rec.p);
                rec.p.D.initTrain_Test_Sets(rec.p.TESTSET_SIZE, rec.p.DATASET_SLICING);
                new Teachers().tabooBoxAndIntensification(rec.p, rec, log);
                try {   // don't eat up all cpu!
                    Thread.sleep(20);
                } catch (Exception e) {
                }
                if (check++ > 20 && rec.p.getNumberOfPendingTrainingSets(rec) == rec.p.NUMBER_OF_TRAINING_SETS)
                {
                    log.log("no result");
                    return;
                }
            }            
            rec.aggregateResults();
            c.putFitness(rec);
            res.countDown(rec);
        }

    };

    public boolean near(LearnParams p, LearnParams q, NNSearchSpace s, TopologyFinding res)
    {
        if (p.NNW_DIMS[0] != q.NNW_DIMS[0])
            return false;
        if (p.NNW_DIMS[p.NNW_DIMS.length - 1] != q.NNW_DIMS[q.NNW_DIMS.length - 1])
            return false;
        if (p.NNW_DIMS.length != q.NNW_DIMS.length)
            return false;
        int dist = Math.abs(p.NNW_DIMS[1] - q.NNW_DIMS[1]);
        double nearness = nearnessRule;
        if (dist > nearness)
            return false;
        
        Object[] oo = res.atomic_getRecordsAndBest();
        TopologyFinding.Item[] ii = (TopologyFinding.Item[])oo[0];
        int ind = (Integer)oo[1];
        if (s.simpleDimensions[0].getDiscretePoints().size() / 3 <
                Math.abs(ii[ind].res.p.NNW_DIMS[1] - p.NNW_DIMS[1]))
            return false;
        return true;
    }


    
    @Override
    public TopologyFinding<MLP> search(final LearnParams templParams, NNSearchSpace searchSpace)
    {
        int size = searchSpace.linearEstimateForSize();
        
        final TopologyFinding<MLP> res = new TopologyFinding(size);
        res.searchState = res.SEARCH_STARTED;
        
        java.util.concurrent.ForkJoinPool fjPool = new ForkJoinPool();
        
        _RecursiveAction a = new _RecursiveAction(0, size-1, templParams, searchSpace, res);
        fjPool.execute(a);
        // return immediately - client will monitor results 
        return res;
    }

}
