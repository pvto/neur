
package neur.auto.routine;

import annot.Stateless;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import neur.MLP;
import neur.auto.NNSearchSpace;
import neur.auto.TopologyFinding;
import neur.auto.TopologySearchRoutine;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import static neur.util.Arrf.concatite;
import neur.util.sdim.SearchDimension.Parameterised;

/**
 *
 * @author Paavo Toivanen
 */
@Stateless
public class GeneticMLPSearch implements TopologySearchRoutine<MLP> {

    
    private static class Specimen {
        LearnParams p;
        LearnRecord r;
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
        // pick hidden layer dimension
        int dim = Math.min(a.p.NNW_DIMS[1], b.p.NNW_DIMS[1]);
        int dim2 = Math.max(a.p.NNW_DIMS[1], b.p.NNW_DIMS[1]);
        eve.p.NNW_DIMS[1] = dim + (int) ((dim2 - dim) * Math.random());
        // pick activation function and parameters for stochastic search by random with a small probability
        if (Math.random() < 0.1)
        {   
            Parameterised sdimAfunc = searchSpace.parameterisedForName(NNSearchSpace.Dim.ACTIVATION_FUNC);
            if (sdimAfunc != null)
            {
                int i = (int) (Math.random() * searchSpace.linearEstimateForSize(sdimAfunc));
                BigDecimal[] func_steepness = searchSpace.indexedClassKey_value(sdimAfunc, i);
                eve.p.NNW_AFUNC = func_steepness[0].intValue();
                eve.p.NNW_AFUNC_PARAMS = new float[]{ func_steepness[1].floatValue() };
            }
            Parameterised stoc = searchSpace.parameterisedForName(NNSearchSpace.Dim.STOCHASTIC_SEARCH_SIZE);
            if (stoc != null)
            {
                int i = (int) (Math.random() * searchSpace.linearEstimateForSize(stoc));
                BigDecimal[] func_steepness = searchSpace.indexedClassKey_value(stoc, i);
                eve.p.RANDOM_SEARCH_ITERS = func_steepness[0].intValue();
            }

        }
        // else from either parent
        else if (Math.random() > 0.5)
        {
            eve.p.NNW_AFUNC = b.p.NNW_AFUNC;
            eve.p.RANDOM_SEARCH_ITERS = a.p.RANDOM_SEARCH_ITERS;
            eve.p.NNW_AFUNC_PARAMS = b.p.NNW_AFUNC_PARAMS;
            eve.p.L = b.p.L.copy();
            eve.p.LEARNING_RATE_COEF = b.p.LEARNING_RATE_COEF;
            eve.p.MODE = b.p.MODE;
        }
        return eve;
    }
    
    public int GENEPOOL_SIZE = 20;
    
    
    
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
                    if (population.size() < 2 || Math.random() > 0.5)
                    {
                        // create a new random individual
                        int ind = (int)(Math.random() * linsize);
                        eve = new Specimen();
                        eve.p = searchSpace.resolveTopologyFromFlattenedIndex(templParams, ind);
                        // do not add exact replicas...
                        for(Specimen lil : concatite(deceased, population))
                            if (eve == lil || searchSpace.equal(eve.p, lil.p))
                                continue;
                    }
                    else
                    {
                        // cross two individuals
                        Specimen
                                a = population.get((int)(Math.random() * population.size())),
                                b = population.get((int)(Math.random() * population.size()))
                                ;
                        // sanity check, do not cross close relatives
                        if (a == b || searchSpace.equal(a.p, b.p))
                            continue;
                        // ok
                        eve = cross(a, b, searchSpace);
                    }
                    evaluateFitness(eve, c);
                    population.add(eve);
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
            if (a.r.averageFitness > b.r.averageFitness)
                return 1;
            if (a.r.averageFitness < b.r.averageFitness)
                return -1;
            return 0;
        }

    };
        
    private static void evaluateFitness(Specimen x, RelativeMLPFitnessComparator c)
    {
        // TODO: learning!
        x.r.aggregateResults();
        c.putFitness(x.r);
    }

}
