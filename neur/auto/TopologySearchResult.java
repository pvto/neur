
package neur.auto;

import neur.NeuralNetwork;
import neur.data.Trainres;
import neur.learning.LearnRec;

/**
 *
 * @author Paavo Toivanen
 */
public class TopologySearchResult<T extends NeuralNetwork> {

    public T best;
    public Trainres trainres;
    public LearnRec bestRec;

    public LearnRec[] res;
    
    int first, 
            last;
    public int[] fitness;
    public TopologySearchResult(int first, int last)
    {
        this.first = first;
        this.last = last;
        fitness = new int[last + 1];
    }
}
