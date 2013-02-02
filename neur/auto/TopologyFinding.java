
package neur.auto;

import java.util.Comparator;
import java.util.Observable;
import neur.NeuralNetwork;
import neur.learning.LearnRecord;

/**
 *
 * @author Paavo Toivanen
 */
public class TopologyFinding<T extends NeuralNetwork> extends Observable {

    public static final int 
            SEARCH_NOT_STARTED     = 0,
            SEARCH_STARTED         = 1,
            SEARCH_FINISHED        = 2
            ;

    public volatile int searchState = SEARCH_NOT_STARTED;


    public volatile int      pendingOperations; 
    public volatile Item[]   records;
    public volatile int      best = -1;
    
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
                    (o1.res.averageFitness > o2.res.averageFitness ? 1 : (o1.res.averageFitness == o2.res.averageFitness ? 0 : -1))));
        }
    };


    public void countDown(LearnRecord rec)
    {
        int slot;
        boolean newBest;
        synchronized(this)
        {
            slot = --pendingOperations;
            if (pendingOperations <= 0)
                searchState = SEARCH_FINISHED;
            newBest = (best < 0 || records[best].res.averageFitness < rec.averageFitness);
            if (slot < 0)
            {   // "overflowing" - pendingOperations was given smaller that the actual number of calls to countDown()
                // - no room for additional search records - throwing the worst record away
                sort();
                slot = 0;            
                if (records[slot].res.averageFitness >= rec.averageFitness)
                    return;
            }
            super.setChanged(); // implement contract for observable
            
            records[slot] = new Item(rec);
            if (newBest)
                best = slot;
        }
        if (pendingOperations == 0) // sort for client's convenience
            sort();
        if (searchState == SEARCH_NOT_STARTED)
            searchState = SEARCH_STARTED;
        
        super.notifyObservers(rec); // implement contract for observable
    }

    private synchronized void sort()
    {
        Item[] records = java.util.Arrays.copyOf(this.records, this.records.length);
        java.util.Arrays.sort(records, CompareFitness);
        this.records = records;
        best = records.length - 1;
    }

    
    public TopologyFinding(int pendingOperations)
    {
        this.pendingOperations = pendingOperations;
        records = new Item[pendingOperations];
    }
}
