
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
    
    public transient LearnParams p;
    /** this tracks the best achieved network */
    public volatile T best;
    public volatile T current;
    public Item bestItem = null;
    public List<Item<T>> items = new ArrayList<>();
    public long timestamp = 0,
            duration = 0;

    public double
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

    
    private double sqr(double a) { return a * a; }
    

    public boolean isAggregated()   {   return averageOutputError != null;  }
    public void aggregateResults()
    {
        duration = System.currentTimeMillis() - timestamp;
        int o = p.NNW_DIMS[p.NNW_DIMS.length - 1];
        int count = Math.max(1, items.size());
        averageOutputError = new float[o];
        double[] avg = new double[]{0,0,0,0,0,0,0,0,0,};
        double[] vars = new double[]{0,0,0,0,0,0,0,0,0,};
        for(Item item : items)
        {
            avg = add(avg, new double[]{
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
        }
        avg = div(avg, items.size());
        for(Item item : items)
        {
            vars = add(vars, new double[]{
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
        averageStochSearchDuration = avg[6];
        varianceStochSearchDuration = vars[6];
        averageSearchDuration = avg[7];
        varianceSearchDuration = vars[7];
        averageFitness = avg[8];
        varianceFitness = vars[8];
        
        for (int i = 0; i < o; i++)
        {
            averageSummedError += averageOutputError[i];
            averageOutputError[i] /= count;
        }
        averageSummedError /= o * count;

    }

    
    public static class Item<T extends NeuralNetwork> implements Serializable
    {
        public LearnRecord<T> L;
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
        public double fitness;
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
            
            fitness = L.p.FIT_FUNC.fitness(this);
                    //(testsetCorrect + trainsetCorrect) * 
                    //(1 - Math.max(0.1,
                    //Math.abs((float)testsetCorrect / L.p.D.TEST.set.size() - (float)trainsetCorrect / L.p.D.TRAIN.set.size()))
                    //);
                    //(float)testsetCorrect + (1f / (1f + (L.p.D.TRAIN.set.size() - (float)trainsetCorrect)));
            Item best = L.bestItem;
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
