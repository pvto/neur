
package neur.learning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import neur.NeuralNetwork;
import neur.data.Trainres;
import neur.learning.clf.Fast1OfNClassifier;
import static neur.util.Arrf.*;


/** A record about network performance on a learning task for a fixed topology, 
 * registered after one or multiple learning sessions.
 * 
 * After each learning session, an item should be added to this record.
 *
 * @author Paavo Toivanen
 */
public class LearnRecord<T extends NeuralNetwork> implements Serializable {

    public LearnRecord(LearnParams params) { this.p = params; }
    
    public LearnParams p;
    /** this tracks the best achieved network */
    public T best;
    public Item bestItem = null;
    public List<Item> items = new ArrayList<Item>();
    public long timestamp = 0,
            duration = 0;

    public float
            averageSummedError = 0f,
            averageTestsetCorrect = 0f,
            varianceTestsetCorrect = 0f,
            averageTrainsetCorrect = 0f,
            varianceTrainsetCorrect = 0f,
            averageBestStochasticIteration = 0f,
            varianceBestStochasticIteration = 0f,
            averageBestIteration = 0f,
            varianceBestIteration = 0f,
            averageTotalIterations = 0f,
            varianceTotalIterations = 0f,
            averageStochSearchDuration = 0f,
            varianceStochSearchDuration = 0f,
            averageSearchDuration = 0f,
            varianceSearchDuration = 0f,
            averageFitness = 0f,
            varianceFitness = 0f
            ;
    public float[] 
            averageOutputError;

    
    private float sqr(float a) { return a * a; }
    

    public void aggregateResults()
    {
        duration = System.currentTimeMillis() - timestamp;
        int o = p.NNW_DIMS[p.NNW_DIMS.length - 1];
        int count = Math.max(1, items.size());
        averageOutputError = new float[o];
        float[] avg = new float[]{0f,0f,0f,0f,0f,0f,0f,0f,0f,};
        float[] vars = new float[]{0f,0f,0f,0f,0f,0f,0f,0f,0f,};
        for(Item item : items)
        {
            avg = add(avg, new float[]{
                item.testsetCorrect,
                item.trainsetCorrect,
                item.bestStochasticIteration,
                item.bestIteration,
                item.totalIterations,
                item.totalStochasticIterations,
                item.stochSearchDuration,
                item.searchDuration,
                item.fitness});
            for (int i = 0; i < o; i++)
            {
                averageOutputError[i] += item.error[i];
            }
            vars = add(vars, new float[]{
                sqr(item.testsetCorrect - avg[0]),
                sqr(item.trainsetCorrect - avg[1]),
                sqr(item.bestStochasticIteration - avg[2]),
                sqr(item.bestIteration - avg[3]),
                sqr(item.totalIterations - avg[4]),
                sqr(item.totalStochasticIterations - avg[5]),
                sqr(item.stochSearchDuration - avg[6]),
                sqr(item.searchDuration - avg[7]),
                sqr(item.fitness - avg[8])
            });
        }
        avg = div(avg, items.size());
        vars = div(vars, items.size());
        
        averageTestsetCorrect = avg[0];
        varianceTestsetCorrect = vars[0];
        averageTrainsetCorrect = avg[1];
        varianceTrainsetCorrect = vars[1];
        averageBestStochasticIteration = avg[2];
        varianceBestStochasticIteration = vars[2];
        averageBestIteration = avg[3];
        varianceBestIteration = vars[3];
        averageTotalIterations = avg[4];
        varianceTotalIterations = vars[4];
        averageStochSearchDuration = avg[5];
        varianceStochSearchDuration = vars[5];
        averageSearchDuration = avg[6];
        varianceSearchDuration = vars[6];
        averageFitness = avg[7];
        varianceFitness = vars[7];
        
        for (int i = 0; i < o; i++)
        {
            averageSummedError += averageOutputError[i];
            averageOutputError[i] /= count;
        }
        averageSummedError /= o * count;

    }

    
    public static class Item<T extends NeuralNetwork>
    {
        private LearnRecord<T> L;
        private Item(LearnRecord<T> L)
        { 
            this.L = L;
            if (L.timestamp == 0)
                L.timestamp = timestamp;
        }
        public long 
                timestamp = System.currentTimeMillis(),
                stochSearchDuration = 0,
                searchDuration = 0
                ;
        public Trainres trainres;
        /** fitness is calculated by an evaluator of the fitness of this result */
        public float fitness;
        public float[] error;
        public int 
                testsetCorrect = 0,
                trainsetCorrect = 0,
                bestStochasticIteration = 0,
                totalStochasticIterations = 0,
                bestIteration = 0,
                totalIterations = 0;
                ;

        private static Fast1OfNClassifier clf = new Fast1OfNClassifier();
        
//        public void clear()
//        {
//            testsetCorrect = 0;
//            trainsetCorrect = 0;
//            bestIteration = 0;
//            fitness = 0f;
//            error = null;
//        }
        public void finish(T nnw)
        {
            searchDuration = System.currentTimeMillis() - timestamp;
            int o = L.p.NNW_DIMS[L.p.NNW_DIMS.length - 1];  // output layer size
            float[][] out = new float[L.p.D.data.length][o];
            for (int k = 0; k < out.length; k++)
            {
                float[] res = nnw.feedf(L.p.D.data[k][0]);
                for (int i = 0; i < res.length; i++)
                {
                    out[k][i] = res[i];
                    
                }
                
                int correct = clf.correctness(L.p.D.data[k], res);
                if (L.p.D.istest.get(k))
                {
                    testsetCorrect += correct;
                }
                else
                {
                    trainsetCorrect += correct;
                }
            }
            error = new float[o];
            for (int i = 0; i < o; i++)
            {
                error[i] = avg(subtract(col(L.p.D.data, 1,i), col(out, i)));
            }
            
            Item best = L.bestItem;
            fitness = (float)testsetCorrect + (1f / (1f + (float)trainsetCorrect));
            if (best == null || fitness > best.fitness)
            {
                L.bestItem = this;
                L.best = nnw.copy();
            }
        }
    }
    


    
    /** Client should call Item.finish() after learning is done!      */
    public Item addItem()
    { 
        Item item = new Item(this);
        items.add(item);
        return item; 
    }
    /** Client should call LearnRecord.items.add(Item) if necessary and Item.finish() after learning is done!      */
    public Item createItem()
    {
        return new Item(this);
    }
    
    
}
