package neur.learning.fit;

import neur.learning.LRecFitness;
import neur.learning.LearnRecord;

public class TrainTestStability2 implements LRecFitness {
    
    @Override
    public float fitness(LearnRecord.Item item)
    {
        float tr = item.testsetCorrect / (float)item.L.p.D.TEST.set.size();
        float te = item.trainsetCorrect / (float)item.L.p.D.TRAIN.set.size();
        return (item.testsetCorrect + item.trainsetCorrect) 
                * (1f - Math.max(0.1f, Math.abs(te - tr)));
    }
}
