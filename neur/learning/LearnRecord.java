
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

    public LearnParams p;
    /** this tracks the best achieved network */
    public T best;
    public Item bestItem = null;
    public List<Item> items = new ArrayList<Item>();
    public long timestamp = 0,
            duration = 0;
    public int 
            lastUpdateIteration = 0,
            totalIterations = 0;

    public float
            averageSummedError = 0f,
            averageTestsetCorrect = 0f,
            averageTrainsetCorrect = 0f,
            averageFitness = 0f
            ;
    public float[] 
            averageOutputError;

    
    public void aggregateResults()
    {
        duration = System.currentTimeMillis() - timestamp;
        int o = p.NNW_DIMS[p.NNW_DIMS.length - 1];
        int count = Math.max(1, items.size());
        averageOutputError = new float[o];
        for(Item item : items)
        {
            averageFitness += item.fitness;
            for (int i = 0; i < o; i++)
            {
                averageOutputError[i] += item.error[i];
            }
        }
        for (int i = 0; i < o; i++)
        {
            averageSummedError += averageOutputError[i];
            averageOutputError[i] /= count;
        }
        averageSummedError /= o * count;
        averageFitness = averageFitness / count;

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
                searchDuration
                ;
        public Trainres trainres;
        /** fitness is calculated by an evaluator of the fitness of this result */
        public float fitness;
        public float[] error;
        public int 
                testsetCorrect = 0,
                trainsetCorrect = 0,
                bestIteration = 0
                ;

        private static Fast1OfNClassifier clf = new Fast1OfNClassifier();
        
        public void clear()
        {
            testsetCorrect = 0;
            trainsetCorrect = 0;
            bestIteration = 0;
            fitness = 0f;
            error = null;
        }
        public void finish(T nnw)
        {
            
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
            if (best == null
                    ||  testsetCorrect > best.testsetCorrect
                    ||  (testsetCorrect == best.testsetCorrect && sum(error) < sum(best.error))
                    )
            {
                L.bestItem = this;
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
