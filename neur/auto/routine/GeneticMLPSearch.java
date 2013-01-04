
package neur.auto.routine;

import annot.Stateless;
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
                        eve = new Specimen();
                        eve.p = a.p.copy();
                        eve.p.NNW_DIMS[1] = (a.p.NNW_DIMS[1] + b.p.NNW_DIMS[1]) / 2;
                        if (Math.random() > 0.5)
                        {
                            eve.p.NNW_AFUNC = b.p.NNW_AFUNC;
                            eve.p.NNW_AFUNC_PARAMS = b.p.NNW_AFUNC_PARAMS;
                        }
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
            if (a.r.fitness > b.r.fitness)
                return 1;
            if (a.r.fitness < b.r.fitness)
                return -1;
            return 0;
        }

    };
        
    private static void evaluateFitness(Specimen x, RelativeMLPFitnessComparator c)
    {
        
        c.putFitness(x.r);
    }

}
