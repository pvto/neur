
package neur.auto;

import neur.learning.LearnParams;
import neur.util.sdim.SearchSpace;


public abstract class NNSearchSpace extends SearchSpace {

    public abstract LearnParams resolveTopologyFromFlattenedIndex(LearnParams templ, int offset);
}
