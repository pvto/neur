package neur.util.dataio;

import java.sql.SQLException;
import java.util.Date;
import neur.learning.LearnRecord;
import neur.struct.ActivationFunction;
import neur.util.Arrf;
import static util.store.DbCli.x.cli;
import util.store.Q;

/** A helper class for persisting a learning record. See script/neur_schema.sql. */
public class LRecPersistence {

    
    public String sample;

    
    
    public void persist(LearnRecord<?> r)
    {
        Date date = new Date();
        for(LearnRecord.Item item : r.items)
        {
            String sql = Q.insertInto("rrec")
            .a( "date",         date,
                "data_gen",     r.p.D.DATA_GEN, 
                "sample",       sample)
            .a( "hid_count",    r.p.NNW_DIMS[1],
                "hid_lr_c",     r.p.NNW_DIMS.length - 2,
                "lalg",         r.p.L.getClass().getSimpleName(),
                "afunc",        ActivationFunction.Types.asString(r.p.NNW_AFUNC),
                "afunc_lrc",    (r.p.NNW_AFUNC_PARAMS.length==0 ? 1f : r.p.NNW_AFUNC_PARAMS[0]),
                "teach_mode",   r.p.MODE)
            .a( "lrate",        r.p.LEARNING_RATE_COEF)
            .a( "dyn_lrate",    (r.p.DYNAMIC_LEARNING_RATE?1:0))
            .a( "test_size",    r.p.TESTSET_SIZE,
                "train_size",   r.p.TRAINSET_SIZE(),
                "test_ok",      item.testsetCorrect,
                "train_ok",     item.trainsetCorrect)
            .a( "sqr_error",    Arrf.avg(Arrf.sqr(item.error)))
            .a( "stoch_best_iter", item.bestStochasticIteration, 
                "stoch_tot_iter", item.totalStochasticIterations,
                "ebp_best_iter", item.bestIteration, 
                "ebp_tot_iter", item.totalIterations)
            .a( "stoch_time",   item.stochSearchDuration, 
                "tot_time",     item.searchDuration,
                "fitness",      item.fitness)
            .z();
            sql = sql.replace("'NaN'","'0.00000'")
                    .replace("'Infinity'", "0.0000")
                    .replaceAll("'\\d{14,}.\\d+'", "'99999999999.9999'");
            try
            {
                cli().execute(sql);
            }
            catch (SQLException ex)
            {
                System.out.println(sql);
                ex.printStackTrace();
            }
        }
        String sql = Q.insertInto("rrec_avg")
        .a( "run_count",    r.items.size(),
            "data_gen",     r.p.D.DATA_GEN, 
            "sample",       sample)
        .a( "hid_count",    r.p.NNW_DIMS[1],
            "hid_lr_c",     r.p.NNW_DIMS.length - 2,
            "lalg",         r.p.L.getClass().getSimpleName(),
            "afunc",        ActivationFunction.Types.asString(r.p.NNW_AFUNC),
            "afunc_lrc",    (r.p.NNW_AFUNC_PARAMS.length==0 ? 1f : r.p.NNW_AFUNC_PARAMS[0]),
            "teach_mode",   r.p.MODE)
        .a( "lrate",        r.p.LEARNING_RATE_COEF)
        .a( "dyn_lrate",    (r.p.DYNAMIC_LEARNING_RATE?1:0))
        .a( "test_size",    r.p.TESTSET_SIZE,
            "train_size",   r.p.TRAINSET_SIZE(),
            "test_ok",      r.averageTestsetCorrect,
            "test_ok_var",  r.varianceTestsetCorrect,
            "train_ok",     r.averageTrainsetCorrect,
            "train_ok_var",  r.varianceTrainsetCorrect)
        .a( "test_err",     100d * (1d - r.averageTestsetCorrect / (double)r.p.TESTSET_SIZE), "%3.2f")
        .a( "train_err",    100d * (1d - r.averageTrainsetCorrect / (double)r.p.TRAINSET_SIZE()), "%3.2f")
        .a( "stoch_best_iter", r.averageBestStochasticIteration,
            "stoch_best_iter_var", r.varianceBestStochasticIteration,
            "stoch_tot_iter", r.p.STOCHASTIC_SEARCH_ITERS,
            "stoch_tot_iter_var", 0.0,
            "ebp_best_iter", r.averageBestIteration,
            "ebp_best_iter_var", r.varianceBestIteration,
            "ebp_tot_iter", r.averageTotalIterations,
            "ebp_tot_iter_var", r.varianceTotalIterations)
        .a( "stoch_time",   r.averageStochSearchDuration, 
            "stoch_time_var",   r.varianceStochSearchDuration, 
            "tot_time",     r.averageSearchDuration,
            "tot_time_var", r.varianceSearchDuration,
            "fitness",      r.averageFitness,
            "fitness_var",  r.varianceFitness)
        .z();
        sql = sql.replace("'NaN'","'0.00000'")
                .replace("'Infinity'", "0.0000")
                .replaceAll("'\\d{14,}.\\d+'", "'99999999999.9999'");
        try
        {
            cli().execute(sql);
        }
        catch (SQLException ex)
        {
            System.out.println(sql);
            ex.printStackTrace();
        }
    }
}
