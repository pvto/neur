
package neur.learning.fit;

import neur.learning.LRecFitness;
import neur.learning.LearnRecord;

public class TrainFit implements LRecFitness {

    @Override
    public float fitness(LearnRecord.Item item)
    {
        return item.trainsetCorrect / (float)item.L.p.D.TRAIN.set.size();
    }
    
}
