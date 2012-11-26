
package neur.auto.routine;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import neur.MLP;
import neur.auto.TopologyResult;
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
    
    
    private class _RecursiveAction extends RecursiveAction
    {
        public _RecursiveAction(int low, int high, LearnParams p, TopologyResult res)
        {
            this.low = low;  this.high = high;
            this.p = p;
            this.res = res;
        }
        int low, high;
        LearnParams p;
        TopologyResult res;
        
        @Override
        protected void compute()
        {
            if (low > high) return;
            if (low == high)
            {
                teachOne();
            }
            int mid = low + (int)((high-low) * Math.random());
            _RecursiveAction t = new _RecursiveAction(low, mid, p, res);
            _RecursiveAction u = new _RecursiveAction(mid+1, high, p, res);
            invokeAll(t, u);
        }
        
        private void teachOne()
        {
            for(int i = 0; i < res.records.length; i++)
            {
                if (res.records[i] != null)
                {
                    if (near(p, res.records[i].res.p))
                        return;
                }
            }
            LearnRecord<MLP> masterRec = new LearnRecord<MLP>();
            
            for (int i = 0; i < rounds; i++)
            {
                LearnParams p = this.p.copy();
                p.NNW_DIMS = Arrf.copy(this.p.NNW_DIMS);
                p.NNW_DIMS[1] = low; 
                p.nnw = new MLP(p.NNW_DIMS, p.NNW_AFUNC);
                LearnRecord<MLP> r = new LearnRecord<MLP>(); r.p = p;
                new Teachers().monteCarloAndIntensification(p, r, log);
                r.fitness = evaluateFitness(r);
                masterRec.fitness += r.fitness;
                masterRec.rounds++;
            }
            masterRec.fitness = masterRec.fitness / masterRec.rounds;
            res.countDown(masterRec);
        }

    };

    public boolean near(LearnParams p, LearnParams q)
    {
        if (p.NNW_DIMS[0] != q.NNW_DIMS[0])
            return false;
        if (p.NNW_DIMS[2] != q.NNW_DIMS[2])
            return false;
        if (Math.abs(p.NNW_DIMS[1] - q.NNW_DIMS[1]) > nearnessRule)
            return false;
        return true;
    }

    float evaluateFitness(LearnRecord r)
    {
        // TODO:
        return 1f;
    }
    
    @Override
    public TopologyResult<MLP> search(final LearnParams templParams)
    {
        int first = 1;
        int last = templParams.D.data.length;
        
        final TopologyResult<MLP> res = new TopologyResult(last - first + 1);
        res.searchState = res.SEARCH_STARTED;
        
        java.util.concurrent.ForkJoinPool fjPool = new ForkJoinPool();
        
        _RecursiveAction a = new _RecursiveAction(first, last, templParams, res);
        fjPool.execute(a);
        // return immediately - client will monitor results 
        return res;
    }

}
