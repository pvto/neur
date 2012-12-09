
package neur.auto;

import java.util.Comparator;
import neur.NeuralNetwork;
import neur.data.Trainres;
import neur.learning.LearnRecord;

/**
 *
 * @author Paavo Toivanen
 */
public class TopologyResult<T extends NeuralNetwork> {

    public static final int 
            SEARCH_NOT_STARTED     = 0,
            SEARCH_STARTED         = 1,
            SEARCH_FINISHED        = 2
            ;

    public int searchState = SEARCH_NOT_STARTED;


    public int      pendingOperations; 
    public Item[]   records;
    public int      best = -1;
    public Trainres trainres;
    
    /** Call this, rather than records[best], if the search is not finished and could overflow. 
     * @return {records[], (int)best} */
    public synchronized Object[] atomic_getRecordsAndBest() { return new Object[]{records,best}; }



    public static class Item {          public Item(LearnRecord r){this.res=r;}
        public LearnRecord res;
        public long timestamp = System.currentTimeMillis();
    }
    static Comparator<Item> CompareFitness = new Comparator<Item>() {
        @Override public int compare(Item o1, Item o2) {
            return (o1 == null && o2 == null) ? 0 : 
                    (o1 == null ? -1 : (o2 == null ? 1 : 
                    (o1.res.fitness > o2.res.fitness ? 1 : (o1.res.fitness == o2.res.fitness ? 0 : -1))));
        }
    };


    public void countDown(LearnRecord rec)
    {
        int slot = pendingOperations - 1;
        boolean newBest = (best < 0 || records[best].res.fitness < rec.fitness);
        if (slot < 0)
        {   // "overflowing" - pendingOperations was given smaller that the actual number of calls to countDown()
            // - no room for additional search records - throwing the worst record away
            sort();
            slot = 0;            
            if (records[slot].res.fitness >= rec.fitness)
                slot = -1;
        }
        if (slot >= 0)
        {
            records[slot] = new Item(rec);
            if (newBest)
                best = slot;
            if (slot == 0) 
                sort();
            if (searchState == SEARCH_NOT_STARTED)
                searchState = SEARCH_STARTED;
        }
        if (--pendingOperations < 0)
            searchState = SEARCH_FINISHED;
    }

    private void sort()
    {
        Item[] records = java.util.Arrays.copyOf(this.records, this.records.length);
        java.util.Arrays.sort(records, CompareFitness);
        synchronized(this)
        {
            this.records = records;
            best = records.length - 1;
        }
    }

    
    public TopologyResult(int pendingOperations)
    {
        this.pendingOperations = pendingOperations;
        records = new Item[pendingOperations];
    }
}
