
package neur.auto.routine;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import neur.MLP;
import neur.auto.NNSearchSpace;
import neur.auto.TopologyFinding;
import neur.auto.TopologySearchRoutine;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.learning.Teachers;
import neur.util.Arrf;
import neur.util.Log;

/**
 *
 * @author Paavo Toivanen
 */
public class PruningConstructiveMLPSearch implements TopologySearchRoutine {

    private static Log log = Log.file.bind("pru-srch.log");
    
    public float nearnessRule = 2f;
    
    public int rounds = 1;
    
    RelativeMLPFitnessComparator c = new RelativeMLPFitnessComparator();
    
    
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
            if (low > high) return;
            if (low == high)
            {
                p = s.resolveTopologyFromFlattenedIndex(p, low);
                teachOne();
            }
            int mid = low + (int)((high-low) * Math.random());
            _RecursiveAction t = new _RecursiveAction(low, mid, p, s, res);
            _RecursiveAction u = new _RecursiveAction(mid+1, high, p, s, res);
            invokeAll(t, u);
        }
        
        private void teachOne()
        {
            for(int i = 0; i < res.records.length; i++)
            {
                if (res.records[i] != null)
                {
                    if (near(p, res.records[i].res.p, s, res))
                        return;
                }
            }

            LearnParams p = this.p.copy();
            p.NNW_DIMS = Arrf.copy(this.p.NNW_DIMS);
            p.NNW_DIMS[1] = low; 
            LearnRecord<MLP> rec = new LearnRecord<MLP>(p);
            for (int i = 0; i < rounds; i++)
            {
                rec.p.nnw = new MLP(p);  // former networks are simply discarded; only their results interest us
                LearnRecord.Item it = rec.addItem();
                new Teachers().monteCarloAndIntensification(p, rec, log);
                it.finish(rec.p.nnw);
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
        if (p.NNW_DIMS[2] != q.NNW_DIMS[2])
            return false;
        if (Math.abs(p.NNW_DIMS[1] - q.NNW_DIMS[1]) > nearnessRule)
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
