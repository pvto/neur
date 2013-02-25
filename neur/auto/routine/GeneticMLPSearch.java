
package neur.auto.routine;

import annot.Stateless;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
import neur.util.visuals.MLPVisualisation;

/**
 *
 * @author Paavo Toivanen
 */
@Stateless
public class GeneticMLPSearch implements TopologySearchRoutine<MLP> {

    public int GENEPOOL_SIZE = 20;
    public double PROB_GENERATE_NEW_INDIVIDUAL = 0.5;
    public double PROB_RANDOM_MUTATION = 0.1;
    public double MAX_DIMENSION_SEARCH_RATIO = 1.0 / 6.0;
    
    public boolean REMOTING = false; // TODO: implement remote server+client
    private Executor exepool = Executors.newCachedThreadPool();

    static Log log = Log.create.chained(Log.cout, Log.file.bind("gen-mlp-s.log"));
    
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
        // copy attributes from either parent
        Specimen A = (Math.random() > 0.5 ? b : a),
                B = A == a ? b : a;
        Specimen eve = new Specimen();
        eve.p = A.p.copy();
        eve.lrec = new LearnRecord(eve.p);
        // pick hidden layer dimension from between parents'
        if (A.p.NNW_DIMS.length > 2 && B.p.NNW_DIMS.length > 2)
        {
            int dim = Math.min(A.p.NNW_DIMS[1], B.p.NNW_DIMS[1]);
            int dim2 = Math.max(A.p.NNW_DIMS[1], B.p.NNW_DIMS[1]);
            int hidsize = dim + (int) ((dim2 - dim) * Math.random());
            int hidlayers = eve.p.NNW_DIMS.length - 1;
            for (int i = 1; i < hidlayers; i++)
                eve.p.NNW_DIMS[i] = hidsize;
            // pick hidden layer count from between parents'
            int hc = Math.min(A.p.NNW_DIMS.length - 2, B.p.NNW_DIMS.length - 2);
            int hc2 = Math.max(A.p.NNW_DIMS.length - 2, B.p.NNW_DIMS.length - 2);
            if (hc2 > hc)
            {
                hidlayers = 1 + (int) (Math.random() * (hc2 - hc));
                while (hidlayers * eve.p.NNW_DIMS[1] > eve.p.D.data.length * MAX_DIMENSION_SEARCH_RATIO)
                    hidlayers--;
                int[] t = eve.p.NNW_DIMS;
                eve.p.NNW_DIMS = new int[2 + hidlayers];
                eve.p.NNW_DIMS[0] = t[0];
                eve.p.NNW_DIMS[eve.p.NNW_DIMS.length - 1] = t[t.length - 1];
                for(int i = hidlayers; i > 0; i--)
                    eve.p.NNW_DIMS[i] = t[1];
            }
        }
        // Get randomly genes from the either parent.
        // There are no dominant genes.
        if (Math.random() > 0.5)
        {
            eve.p.DYNAMIC_LEARNING_RATE = B.p.DYNAMIC_LEARNING_RATE;
            eve.p.LEARNING_RATE_COEF = B.p.LEARNING_RATE_COEF;
            eve.p.MODE = B.p.MODE;
            eve.p.NNW_AFUNC = B.p.NNW_AFUNC;
            eve.p.NNW_AFUNC_PARAMS = B.p.NNW_AFUNC_PARAMS;
        }
        if (Math.random() > 0.5)
            eve.p.STOCHASTIC_SEARCH_ITERS = B.p.STOCHASTIC_SEARCH_ITERS;
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
                        int hidlrsize = Math.max(1, (int)(eve.p.NNW_DIMS[1] * Math.random()));
                        for (int j = 1; j < eve.p.NNW_DIMS.length - 1; j++)
                            eve.p.NNW_DIMS[j] = hidlrsize;
                        
                        if (eve.p.NNW_DIMS.length > 2)
                        {
                            // even out to not get too big dimensions
                            int nh = eve.p.NNW_DIMS.length - 2;
                            while (nh > 1 && eve.p.NNW_DIMS[1] * nh > eve.p.D.data.length * MAX_DIMENSION_SEARCH_RATIO)
                                nh--;
                            if (nh > eve.p.NNW_DIMS.length - 2)
                            {
                                int[] t = eve.p.NNW_DIMS;
                                eve.p.NNW_DIMS = new int[nh + 2];
                                eve.p.NNW_DIMS[0] = t[0];
                                eve.p.NNW_DIMS[eve.p.NNW_DIMS.length - 1] = t[t.length - 1];
                                for(int x = nh; x > 0; x--)
                                    eve.p.NNW_DIMS[x] = t[1];
                            }
                        }
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
                    
                    ++i;
                    
                    if (cat.challenge(eve) == 0)
                        continue;

                    MLPVisualisation vis = new MLPVisualisation().createFrame(eve.lrec, 400, 300, 10).run();
                    evaluateFitness(eve, c, cat);
                    vis.doRun = false;
                    population.add(eve);
                    ret.countDown(eve.lrec);
                    // keep the population relatively small with some fit and some random individuals
                    if (i % (GENEPOOL_SIZE * 2) == GENEPOOL_SIZE * 2 - 1)
                    {
                        Collections.sort(population, FITNESS);
                        while(population.size() > GENEPOOL_SIZE)
                        {
                            Specimen ex = population.remove((int)(Math.random() * 0.5 * GENEPOOL_SIZE));
                            deceased.add(ex);
                        }
                    }
                }
                ret.finish();
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


    private static void evaluateFitness(Specimen x, RelativeMLPFitnessComparator c, Predator cat)
    {
        log.log("evaluate fitness h%d L%s ", x.p.NNW_DIMS[1], x.p.L.getClass());
        
        int check = 0;
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
            if (check++ > 20 && x.p.getNumberOfPendingTrainingSets(x.lrec) == x.p.NUMBER_OF_TRAINING_SETS)
            {
                log.log("no result");
                return;
            }
        }
        x.lrec.aggregateResults();
        c.putFitness(x.lrec);
        cat.memorise(x);
    }

    
    private static class Predator {
        double meek = 2;
        LearnParams L = new LearnParams(){{
            NNW_DIMS = new int[]{9,12,2};
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
                    x.p.NNW_DIMS[1], x.p.NNW_DIMS.length - 2,
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
                try{
                if (preyLooksHealthy(in))
                    return ret;
                }catch(Exception ex)
                {
                    log.err("in: " + Arrays.toString(in), ex);
                    log.log("dims: " + Arrays.toString(L.NNW_DIMS));
                }
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
            LearnParams p = prey.p.copy();
            p.nnw = new MLP(prey.p);
            LearnRecord lrec = new LearnRecord(p);
            p.D.initTrain_Test_Sets(p.TESTSET_SIZE, p.DATASET_SLICING);
            new Teachers().tabooBoxAndIntensification(p, lrec, challog);
            LearnRecord.Item item = (LearnRecord.Item) lrec.bestItem;
            if (item == null)
                return new Object[]{null,null};
            boolean ok = item.testsetCorrect >= p.D.TEST.set.size() / meek
                   && item.trainsetCorrect >= p.D.TRAIN.set.size() / meek;
            challog.log("chase: fled= %s (te= %d"
                    + "|%f, tr= %d|%f)", ok, 
                    item.testsetCorrect, p.D.TEST.set.size() / meek,
                    item.trainsetCorrect, p.D.TRAIN.set.size() / meek);
            return new Object[]{ok?true:null, item};
        }
        void logChallenge(Specimen prey)
        {
            challog.log("chall: assess= %s; eve= (h=%d*%d, stoc=%d, af=%d, k=%f, m=%s, lr=%f)", 
                    readyToAssess(),
                    prey.p.NNW_DIMS[1], prey.p.NNW_DIMS.length-2, 
                    prey.p.STOCHASTIC_SEARCH_ITERS, prey.p.NNW_AFUNC,
                    prey.p.NNW_AFUNC_PARAMS[0], prey.p.MODE, prey.p.LEARNING_RATE_COEF);
        }
    }

}
