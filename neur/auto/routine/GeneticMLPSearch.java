
package neur.auto.routine;

import neur.MLP;
import neur.auto.NNSearchSpace;
import neur.auto.TopologyFinding;
import neur.auto.TopologySearchRoutine;
import neur.learning.LearnParams;

/**
 *
 * @author Paavo Toivanen
 */
public class GeneticMLPSearch implements TopologySearchRoutine<MLP> {

    @Override
    public TopologyFinding<MLP> search(LearnParams templParams, NNSearchSpace searchSpace)
    {
        int linsize = searchSpace.linearEstimateForSize();
        TopologyFinding<MLP> f = new TopologyFinding<MLP>();
        
        
    }

}
