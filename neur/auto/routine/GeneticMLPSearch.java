
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
import neur.data.Dataset;
import neur.data.Dataset.Slicing;
import neur.data.TrainMode;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.learning.LearningAlgorithm;
import neur.learning.Teachers;
import neur.learning.clf.Fast1Of2Classifier;
import neur.learning.learner.ElasticBackProp;
import neur.struct.ActivationFunction;
import neur.util.Arrf;
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
        // copy attributes from either parent
        eve.p = (Math.random() > 0.5 ? b.p : a.p).copy();
        eve.lrec = new LearnRecord(eve.p);
        // pick hidden layer dimension from between parents'
        int dim = Math.min(a.p.NNW_DIMS[1], b.p.NNW_DIMS[1]);
        int dim2 = Math.max(a.p.NNW_DIMS[1], b.p.NNW_DIMS[1]);
        eve.p.NNW_DIMS[1] = dim + (int) ((dim2 - dim) * Math.random());
        // modify genes by random mutation with a small probability
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
                Math.max(40, linsize / 6);
        final TopologyFinding<MLP> ret = new TopologyFinding<>(maxOperations);
        
        final Predator cat = new Predator();
        
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
                    // do not add exact replicas...
                    for(Specimen lil : population)
                        if (eve == lil || searchSpace.equal(eve.p, lil.p))
                            continue;
                    for(Specimen lil : deceased)
                        if (eve == lil || searchSpace.equal(eve.p, lil.p))
                            continue;
                    if (cat.challenge(eve) == 0)
                        continue;

                    evaluateFitness(eve, c, cat);
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
    private static void evaluateFitness(Specimen x, RelativeMLPFitnessComparator c, Predator cat)
    {
        log.log("evaluate fitness h%d L%s ", x.p.NNW_DIMS[1], x.p.L.getClass());
        
        while(x.p.getNumberOfPendingTrainingSets(x.lrec) > 0)
        {
            // TODO: this could be remote with forkjoin!            
            x.p.nnw = new MLP(x.p);
            x.p.D.initTrain_Test_Sets(x.p.TESTSET_SIZE, x.p.DATASET_SLICING);
            new Teachers().tabooBoxAndIntensification(x.p, x.lrec, log);
            try {   // don't eat up all cpu!
                Thread.sleep(20);
            } catch (Exception e) {
            }
        }
        x.lrec.aggregateResults();
        c.putFitness(x.lrec);
        cat.memorise(x);
    }

    
    private static class Predator {
        double meek = 1.5;
        LearnParams L = new LearnParams(){{
            NNW_DIMS = new int[]{3,12,2};
            CF = new Fast1Of2Classifier();
            L = new ElasticBackProp();
            STOCHASTIC_SEARCH_ITERS = 600;
            TEACH_MAX_ITERS = 260;
            DATASET_SLICING = Slicing.Random;
            D = new Dataset();
            DIVERGENCE_PRESUMED = 60;
            TRG_ERR = 0.1f;
            NNW_AFUNC = ActivationFunction.Types.AFUNC_SOFTSIGN;
            DYNAMIC_LEARNING_RATE = false;
            NUMBER_OF_TRAINING_SETS = 1;
        }};
        MLP reasoning = new MLP(L);
        List<float[][]> knowledge = new ArrayList<float[][]>();
        int memorised = 0;
        
        boolean preyLooksHealthy(float[] in)        { return reasoning.feedf(in)[1] > 0f; }
        boolean readyToAssess()                     { return memorised > 30; }
        Log challog = Log.cout;
                //Log.file.bind("challence.log");

        float[] observe(Specimen x)
        {   // gives an observation of a prey from the predator's point of view
            return new float[]{
                    x.p.NNW_DIMS[1],
                    x.p.STOCHASTIC_SEARCH_ITERS,
                    x.p.MODE.isSupervised()?0f:1f, x.p.LEARNING_RATE_COEF,
                    x.p.NNW_AFUNC==1?1f:0f, x.p.NNW_AFUNC==3?1f:0f, x.p.NNW_AFUNC==4?1f:0f,
                    x.p.NNW_AFUNC_PARAMS[0], 
                };
        }
        void memorise(Specimen x)
        {
            int healthy = (x.lrec.averageTestsetCorrect >= x.p.D.TEST.set.size() / meek
                    && x.lrec.averageTrainsetCorrect >= x.p.D.TRAIN.set.size() / meek)
                    ? 1 : 0;
            knowledge.add(new float[][]{ observe(x), new float[]{healthy,1-healthy} });
            if (knowledge.size() > 29 && (knowledge.size()-10) % 20 == 0)
            {
                memorised = knowledge.size();
                L.D.data = Arrf.arrayff_shallow(knowledge);
                L.D.initTrain_Test_Sets(0, L.DATASET_SLICING);
                
            }
        }
        public int challenge(Specimen prey)
        {
            int ret = 1;
            logChallenge(prey);
            float[] in = observe(prey);
            if (readyToAssess())
            {
                if (preyLooksHealthy(in))
                    return ret;
            }
            Object[] preyHealth = chase(prey);
            if (preyHealth[0] == null)
            {
                ret = 0;
                float[] out = new float[]{ret, 1-ret};
                knowledge.add(new float[][]{in,out});
            }
            return ret;
        }
        Object[] chase(Specimen prey)
        {
            MLP m = new MLP(prey.p);
            LearnParams p = prey.p.copy();
            LearnRecord lrec = new LearnRecord(p);
            p.D.initTrain_Test_Sets(p.TESTSET_SIZE, p.DATASET_SLICING);
            new Teachers().tabooBoxAndIntensification(p, lrec, challog);
            LearnRecord.Item item = (LearnRecord.Item) lrec.bestItem;
            boolean ok = item.testsetCorrect >= p.D.TEST.set.size() / meek
                   && item.trainsetCorrect >= p.D.TRAIN.set.size() / meek;
            challog.log("chase: fleed= %s (te= %d|%f, tr= %d|%f)", ok, 
                    item.testsetCorrect, p.D.TEST.set.size() / meek,
                    item.trainsetCorrect, p.D.TRAIN.set.size() / meek);
            return new Object[]{ok?true:null, item};
        }
        void logChallenge(Specimen prey)
        {
            challog.log("chall: assess= %s; eve= (h=%d, stoc=%d, af=%d, k=%f, m=%s, lr=%f)", 
                    readyToAssess(),
                    prey.p.NNW_DIMS[1], prey.p.STOCHASTIC_SEARCH_ITERS, prey.p.NNW_AFUNC,
                    prey.p.NNW_AFUNC_PARAMS[0], prey.p.MODE, prey.p.LEARNING_RATE_COEF);
        }
    }

}
