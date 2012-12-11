
package neur.auto.routine;

import java.math.BigDecimal;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import neur.MLP;
import neur.auto.NNSearchSpace;
import neur.auto.TopologyFinding;
import neur.auto.TopologySearchRoutine;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.learning.Teachers;
import neur.struct.ActivationFunction;
import neur.util.Arrf;
import neur.util.Log;

/**
 *
 * @author Paavo Toivanen
 */
public class MonteCarloMLPSearch implements TopologySearchRoutine<MLP> {

    private static Log log = Log.file.bind("mc-srch.log");

    public int rounds = 1;
    public BigDecimal QUANTISER = new BigDecimal(1.0);
    
    
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
                p = s.resolveTopologyFromFlattenedIndex(p, low, QUANTISER);
                teachOne();
            }
            int mid = low + (int)((high-low) * Math.random());
            _RecursiveAction t = new _RecursiveAction(low, mid, p, s, res);
            _RecursiveAction u = new _RecursiveAction(mid+1, high, p, s, res);
            invokeAll(t, u);
        }
        
        private void teachOne()
        {
            LearnRecord<MLP> masterRec = new LearnRecord<MLP>();
            
            for (int i = 0; i < rounds; i++)
            {
                LearnParams p = this.p.copy();
                p.NNW_DIMS = Arrf.copy(this.p.NNW_DIMS);
                p.NNW_DIMS[1] = low; 
                p.nnw = new MLP(p.NNW_DIMS, ActivationFunction.Types.create(p.NNW_AFUNC, p.NNW_AFUNC_PARAMS));
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
    
    float evaluateFitness(LearnRecord r)
    {
        // TODO:
        return 1f;
    }
    
    @Override
    public TopologyFinding<MLP> search(final LearnParams templParams, NNSearchSpace searchSpace)
    {
        int size = searchSpace.linearEstimateForSize(BigDecimal.ONE);
        
        final TopologyFinding<MLP> res = new TopologyFinding(size);
        res.searchState = res.SEARCH_STARTED;
        
        java.util.concurrent.ForkJoinPool fjPool = new ForkJoinPool();
        
        _RecursiveAction a = new _RecursiveAction(0, size-1, templParams, searchSpace, res);
        fjPool.execute(a);
        // return immediately - client will monitor results 
        return res;
    }

}
