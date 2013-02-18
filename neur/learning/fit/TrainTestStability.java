package neur.learning.fit;

import annot.Stateless;
import neur.learning.LRecFitness;
import neur.learning.LearnRecord.Item;

@Stateless
public class TrainTestStability  implements LRecFitness {

    @Override
    public float fitness(Item item)
    {
        float tr = item.testsetCorrect / (float)item.L.p.D.TEST.set.size();
        float te = item.trainsetCorrect / (float)item.L.p.D.TRAIN.set.size();
        return (tr + te) / 2.0f - Math.abs(tr - te);
    }
    
    
}
