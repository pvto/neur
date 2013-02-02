
package neur.auto.routine;

import annot.Stateless;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import neur.MLP;
import neur.auto.NNSearchSpace;
import neur.auto.TopologyFinding;
import neur.auto.TopologySearchRoutine;
import neur.data.TrainMode;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.learning.LearningAlgorithm;
import neur.learning.Teachers;
import static neur.util.Arrf.concatite;
import neur.util.Log;
import neur.util.sdim.SearchDimension;
import neur.util.sdim.SearchDimension.Parameterised;

/**
 *
 * @author Paavo Toivanen
 */
@Stateless
public class GeneticMLPSearch implements TopologySearchRoutine<MLP> {

    public int GENEPOOL_SIZE = 20;
    public double PROB_GENERATE_NEW_INDIVIDUAL = 0.5;
    public double PROB_RANDOM_MUTATION = 0.1;
    
    public boolean REMOTING = false; // TODO: implement remote server+client
    private Executor exepool = Executors.newCachedThreadPool();
    
    private static class Specimen {
        LearnParams p;
        LearnRecord lrec;
    }
    /** 
     * @return a cross between the two given individuals, where continuous properties like 
     * hidden-layer dimension are picked from an even distribution in [a,b], 
     * and non-continuous properties like the activation function 
     * are picked randomly from one of the two, with a small chance for random mutation.
     */
    private Specimen cross(Specimen a, Specimen b, NNSearchSpace searchSpace)
    {
        Specimen eve = new Specimen();
        eve.p = a.p.copy();
        eve.lrec = new LearnRecord(eve.p);
        // pick hidden layer dimension
        int dim = Math.min(a.p.NNW_DIMS[1], b.p.NNW_DIMS[1]);
        int dim2 = Math.max(a.p.NNW_DIMS[1], b.p.NNW_DIMS[1]);
        eve.p.NNW_DIMS[1] = dim + (int) ((dim2 - dim) * Math.random());
        // pick activation function and parameters for stochastic search by random mutation with a small probability
        // else from either parent
        if (Math.random() > 0.5)
        {
            eve.p.NNW_AFUNC = b.p.NNW_AFUNC;
            eve.p.STOCHASTIC_SEARCH_ITERS = a.p.STOCHASTIC_SEARCH_ITERS;
            eve.p.NNW_AFUNC_PARAMS = b.p.NNW_AFUNC_PARAMS;
            eve.p.L = b.p.L.copy();
            eve.p.LEARNING_RATE_COEF = b.p.LEARNING_RATE_COEF;
            eve.p.MODE = b.p.MODE;
        }
        if (Math.random() < PROB_RANDOM_MUTATION)
        {   
            Parameterised sdimAfunc = searchSpace.parameterisedForName(NNSearchSpace.Dim.ACTIVATION_FUNC);
            if (sdimAfunc != null)
            {
                int i = (int) (Math.random() * searchSpace.linearEstimateForSize(sdimAfunc));
                BigDecimal[] func_steepness = searchSpace.indexedClassKey_value(sdimAfunc, i);
                eve.p.NNW_AFUNC = func_steepness[0].intValue();
                eve.p.NNW_AFUNC_PARAMS = new float[]{ func_steepness[1].floatValue() };
            }
            SearchDimension stoc = searchSpace.dimensionForName(NNSearchSpace.Dim.STOCHASTIC_SEARCH_SIZE);
            if (stoc != null)
            {
                int i = (int) (Math.random() * searchSpace.linearEstimateForSize(stoc));
                BigDecimal stocCount = searchSpace.getIndexedPoint(stoc, i);
                eve.p.STOCHASTIC_SEARCH_ITERS = stocCount.intValue();
            }
            SearchDimension lalg = searchSpace.dimensionForName(NNSearchSpace.Dim.LEARNING_ALGORITHM);
            if (lalg != null)
            {
                int ind = (int) (Math.random() * searchSpace.linearEstimateForSize(lalg));
                Object[] oo = lalg.getTargetGenerator().generate(ind);
                eve.p.L = (LearningAlgorithm) oo[0];//o.L;
                eve.p.LEARNING_RATE_COEF = (float) oo[1]; //o.LEARNING_RATE_COEF;
                eve.p.DYNAMIC_LEARNING_RATE = (boolean) oo[2]; //o.DYNAMIC_LEARNING_RATE;
                eve.p.MODE = (TrainMode) oo[3]; //o.MODE;

//                NNSearchSpace.LearningAlgorithmParameters o = lalg.getTargetGenerator().generate(i);
//                eve.p.L = o.L;
//                eve.p.LEARNING_RATE_COEF = o.LEARNING_RATE_COEF;
//                eve.p.DYNAMIC_LEARNING_RATE = o.DYNAMIC_LEARNING_RATE;
//                eve.p.MODE = o.MODE;
            }
        }
        return eve;
    }
    

    
    
    
    @Override
    public TopologyFinding<MLP> search(final LearnParams templParams, final NNSearchSpace searchSpace)
    {
        final int linsize = searchSpace.linearEstimateForSize();
        final int maxOperations = 
                Math.min(linsize, linsize / 3 + 4);
        final TopologyFinding<MLP> ret = new TopologyFinding<>(maxOperations);
        
        Runnable r = new Runnable()
        {

            @Override
            public void run()
            {
                 RelativeMLPFitnessComparator c = new RelativeMLPFitnessComparator();

                List<Specimen> population = new ArrayList<>();
                List<Specimen> deceased = new ArrayList<>();
                
                int i = 0;
                while(i < maxOperations)
                {
                    Specimen eve = null;
                    if (population.size() < 2 || Math.random() > 0.6)
                    {
                        // create a new random individual
                        int ind = (int)(Math.random() * linsize);
                        eve = new Specimen();
                        eve.p = searchSpace.resolveTopologyFromFlattenedIndex(templParams, ind);
                        // treat hidden layer specifically. get it from a square distribution, favoring small hidden layer sizes
                        eve.p.NNW_DIMS[1] = Math.max(1, (int)(eve.p.NNW_DIMS[1] * Math.random()));
                        eve.lrec = new LearnRecord(eve.p);
                        // do not add exact replicas...
                        for(Specimen lil : concatite(deceased, population))
                            if (eve == lil || searchSpace.equal(eve.p, lil.p))
                                continue;
                    }
                    else
                    {
                        // cross two individuals, slightly favoring fittest
                        double bias = Math.pow(Math.random(), 3.0);
                        Specimen
                                a = population.get((int)(Math.random() * population.size())),
                                b = population.get(population.size() - 1 - (int)(bias * population.size()))
                                ;
                        // sanity check, do not cross close relatives
                        if (a == b || searchSpace.equal(a.p, b.p))
                            continue;
                        // ok
                        eve = cross(a, b, searchSpace);
                    }
                    evaluateFitness(eve, c);
                    population.add(eve);
                    ret.countDown(eve.lrec);
                    // keep the population relatively small with some fit and some random individuals
                    if (++i % (GENEPOOL_SIZE * 2) == GENEPOOL_SIZE * 2 - 1)
                    {
                        Collections.sort(population, FITNESS);
                        while(population.size() > GENEPOOL_SIZE)
                        {
                            Specimen ex = population.remove((int)(Math.random() * 0.5 * GENEPOOL_SIZE));
                            deceased.add(ex);
                        }
                    }
                }
            }


        };
        new Thread(r).start();
        return ret;
    }
    
    private static Comparator<Specimen> FITNESS = 
            new Comparator<Specimen>()
    {

        @Override
        public int compare(Specimen a, Specimen b)
        {
            if (a.lrec.averageFitness > b.lrec.averageFitness)
                return 1;
            if (a.lrec.averageFitness < b.lrec.averageFitness)
                return -1;
            return 0;
        }

    };

    static Log log = Log.create.chained(Log.cout, Log.file.bind("gen-mlp-s.log"));
    private static void evaluateFitness(Specimen x, RelativeMLPFitnessComparator c)
    {
        log.log("evaluate fitness h%d L%s ", x.p.NNW_DIMS[1], x.p.L.getClass());
        
        while(x.p.getNumberOfPendingTrainingSets(x.lrec) > 0)
        {
            // TODO: this could be remote with forkjoin!            
            x.p.nnw = new MLP(x.p);
            x.p.D.initTrain_Test_Sets(x.p.TESTSET_SIZE, x.p.DATASET_SLICING);
            new Teachers().tabooBoxAndIntensification(x.p, x.lrec, log);
        }
        x.lrec.aggregateResults();
        c.putFitness(x.lrec);
    }

}
