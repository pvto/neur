
package neur.auto;

import neur.learning.LearnParams;
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
}
