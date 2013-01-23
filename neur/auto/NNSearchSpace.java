
package neur.auto;

import neur.data.TrainMode;
import neur.learning.LearnParams;
import neur.learning.LearningAlgorithm;
import neur.util.sdim.SearchSpace;


public abstract class NNSearchSpace extends SearchSpace {

    public abstract LearnParams resolveTopologyFromFlattenedIndex(LearnParams templ, int offset);

    public abstract boolean equal(LearnParams a, LearnParams b);
    
    
    public static enum Dim
    {
        HIDDEN_LR_SIZE,
        ACTIVATION_FUNC,
        STOCHASTIC_SEARCH_SIZE,
        LEARNING_ALGORITHM
        ;
    }
    
    public static class LearningAlgorithmParameters {
        public LearningAlgorithm L;
        public float LEARNING_RATE_COEF = 0.1f;
        public boolean DYNAMIC_LEARNING_RATE = true;
        public TrainMode MODE;
    }
}
