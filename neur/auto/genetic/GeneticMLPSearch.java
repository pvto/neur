
package neur.auto.genetic;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import neur.MLP;
import neur.auto.TopologySearch;
import neur.auto.TopologySearchResult;
import neur.learning.LearnParams;
import neur.learning.LearnRec;
import neur.learning.Teachers;
import neur.util.Arrf;
import neur.util.Log;

/**
 *
 * @author Paavo Toivanen
 */
public class GeneticMLPSearch implements TopologySearch<MLP> {

    private static Log log = new Log()
    {
        private SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        private OutputStream out;
        {
            try {
                out = new BufferedOutputStream(new FileOutputStream(new File("gen-srch.log")));
            } 
            catch(IOException e) { e.printStackTrace(); }
        }
        public void log(String f, Object ... fs)
        {
            String s = String.format(f, fs);
            System.out.println(s);
            try {
                out.write(s.getBytes());
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    };
    
    private static class RecursiveAction_ extends RecursiveAction
    {
        public RecursiveAction_(int low, int high, LearnParams p, TopologySearchResult res)
        {
            this.low = low;  this.high = high;
            this.p = p;
            this.res = res;
        }
        int low, high;
        LearnParams p;
        TopologySearchResult res;
        
        @Override
        protected void compute()
        {
            if (low > high) return;
            if (low == high)
            {
                teachOne();
            }
            int mid = low + (int)((high-low) * Math.random());
            RecursiveAction_ t = new RecursiveAction_(low, mid, p, res);
            RecursiveAction_ u = new RecursiveAction_(mid+1, high, p, res);
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
        }
    };
    
    @Override
    public TopologySearchResult<MLP> search(final LearnParams templParams)
    {
        int first = 1;
        int last = templParams.D.data.length;
        final TopologySearchResult<MLP> res = new TopologySearchResult(first, last);
        
        java.util.concurrent.ForkJoinPool fjPool = new ForkJoinPool();
        
        RecursiveAction_ a = new RecursiveAction_(first, last, templParams, res);
        fjPool.submit(a);
        try
        {
            fjPool.awaitTermination(2, TimeUnit.DAYS);
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }
        
        return res;
    }

}
