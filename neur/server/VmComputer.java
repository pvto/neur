package neur.server;

import neur.MLP;
import neur.learning.LearnRecord;
import neur.learning.Teachers;
import neur.util.Log;




public class VmComputer implements Computer {

    Log log = Log.create.chained(Log.file.bind("vm-exec.log"), Log.cout);
    
    private TcpjServer serv;
    public VmComputer(TcpjServer serv)
    {
        this.serv = serv;
    }
    
    
    @Override
    public boolean execute(int id, LearnRecord r)
    {
        log.log("vmexecuting start %d, n=%d", id, r.p.getNumberOfPendingTrainingSets(r));
        try
        {
            int check = 0;
            while(r.p.getNumberOfPendingTrainingSets(r) > 0)
            {
                r.p.nnw = new MLP(r.p);
                r.p.D.initTrain_Test_Sets(r.p.TESTSET_SIZE, r.p.DATASET_SLICING);
                new Teachers().tabooBoxAndIntensification(r.p, r, log);
                if (check++ > 20 && r.p.getNumberOfPendingTrainingSets(r) == r.p.NUMBER_OF_TRAINING_SETS)
                {
                    log.log("no result");
                    return false;
                }
            }
            r.aggregateResults();
            log.log("vmexecuting done %d, submitting", id);
            serv.submitResults(id, r);
            return true;
        }
        catch(Exception e)
        {
            log.err("failed to execute learning ", e);
        }
        return false;
    }
    
    public boolean isReserved() { return false; }   // stateless
}
