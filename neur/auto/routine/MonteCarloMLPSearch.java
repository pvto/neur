
package neur.auto.routine;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import neur.MLP;
import neur.auto.TopologyResult;
import neur.auto.TopologySearchRoutine;
import neur.learning.LearnParams;
import neur.learning.LearnRec;
import neur.learning.Teachers;
import neur.util.Arrf;
import neur.util.Log;

/**
 *
 * @author Paavo Toivanen
 */
public class MonteCarloMLPSearch implements TopologySearchRoutine<MLP> {

    private static Log log = Log.file.bind("mc-srch.log");
    
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
            LearnParams p = this.p.copy();
            p.NNW_DIMS = Arrf.copy(this.p.NNW_DIMS);
            p.NNW_DIMS[1] = low; 
            p.nnw = new MLP(p.NNW_DIMS, p.NNW_AFUNC);
            LearnRec<MLP> r = new LearnRec<MLP>(); r.p = p;
            new Teachers().monteCarloAndIntensification(p, r, log);
            float fitness = 0f; //TODO
            res.countDown(r, fitness);
        }
    };
    
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
