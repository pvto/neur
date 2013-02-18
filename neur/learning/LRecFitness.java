package neur.learning;

import annot.Stateless;
import java.io.Serializable;

@Stateless
public interface LRecFitness extends Serializable {

    float fitness(LearnRecord.Item item);
    
}
