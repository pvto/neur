package neur;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import neur.auto.in.Sampler;
import neur.data.Dataset;
import neur.data.TrainMode;
import neur.learning.LearnParams;
import neur.learning.LearnRec;
import neur.learning.Teachers;
import neur.learning.clf.Fast1OfNClassifier;
import neur.learning.learner.ElasticBackProp;
import neur.struct.ActivationFunction;
import neur.util.Log;
import neur.util.dataio.DiskIO;
import static util.store.DbCli.x.*;
import util.store.Q;

/**
 *
 * @author Paavo Toivanen
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, SQLException
    {
        new File("mlp").mkdir();
        
        //intFeed();
        int max_runs = 40;
        final int dataset = 104;
        final String sample = "new-thyroid";
        final String datagen = sample;
        
//        DbSamples dbSamples = new DbSamples(getDbClient());
//        final float[][][] tdata = createSample();
//        final float[][][] tdata = dbSamples.loadSample(dataset, sample);
        final float[][][] tdata = new Sampler().extractSampleFromCSV(
                new DiskIO().loadCSV(
                "/media/KINGSTON/nlg/data/MATLAB/"+sample+".data",","),
                new int[]{1,2,3,4,5},new int[]{0});
                //"/media/KINGSTON/nlg/data/MATLAB/haberman.data",","),
                //new int[]{0,1,2},new int[]{3});
                //"/media/KINGSTON/nlg/data/MATLAB/ecoli.data","\\s+"),
                //new int[]{1,2,5,6,7},new int[]{8});
                //bupa.data", ","), 
                //new int[]{0,1,2,3,4,5}, // input layer columns
                //new int[]{6});  // supervision columns (out layer expected values)
                
//        Statf.normalise(tdata);
//        dbSamples.saveSample(dataset, datagen, sample, tdata);
        //dbSamples.deleteSample(dataset, sample);
        LearnParams<MLP,ElasticBackProp> p = new LearnParams()
        {{
                NNW_AFUNC = ActivationFunction.Types.AFUNC_SIGMOID;
                MODE = TrainMode.ONLINE_MODE;
                NNW_DIMS = new int[]{tdata[0][0].length, 30, tdata[0][1].length};
                //NNW_DIMS = new int[]{2, 40, 2};

                L = new neur.learning.learner.ElasticBackProp();
                LEARNING_RATE_COEF = 0.1f;
                DYNAMIC_LEARNING_RATE = true;
                TRG_ERR = 1e-6f;
                TEACH_MAX_ITERS = 1000;
                TEACH_TARRY_NOT_CONVERGING = 2;
                DIVERGENCE_PRESUMED = TEACH_MAX_ITERS / 2;
                RANDOM_SEARCH_ITERS = 1;

                D = new Dataset()
                {{
                        DATA_GEN = datagen;
                        DATASET = dataset;
                        SAMPLE = sample;
                        data = tdata;
                        initTestVldSets(data.length * 1 / 10, Slicing.TakeRandom);
                }};
                
                int outlen = D.data[0][1].length;
//                if (outlen == 2)
//                    CF = new Classifiers.OptimisedLinearOut2Classifier();
//                else if (outlen > 2)
                    CF = new Fast1OfNClassifier();
        }};        

        for (int i = 0; i < max_runs; )
        {
            p.nnw = new MLP(p.NNW_DIMS, p.NNW_AFUNC);
            LearnRec<MLP> r = new LearnRec<MLP>(); r.p = p;
//            nnw.layers[0][0].netInput = 1f; // 0f disables bias neuron
//            nnw.layers[1][0].netInput = 1f; // 0f disables bias neuron
            runTest(p, r);
            if (++i % 4 == 0)
            {
                p.NNW_DIMS[1]++;
            }            
        }
        cli().close();
        
    }

    private static Log log = Log.log;
    
    
    private static void runTest(LearnParams p, LearnRec<MLP> r) throws IOException, SQLException
    {
        new Teachers().monteCarloAndIntensification(p, r, log);
        
        String filename = "mlp" + File.separator 
                +p.D.DATASET+"-"+p.D.SAMPLE+"-hid"+(r.best.layers[1].length-1)+"-"
                +ActivationFunction.Types.asString(p.NNW_AFUNC).substring(0,4)
                +"-ok"+String.format("%.2f", r.testsetCorrect*100.0/p.D.V.set.size())
                +"-"+new SimpleDateFormat("yyMMddHHmmss").format(new Date())
                +".mlp";
        new DiskIO().save(filename, r.best);
        log.log("saved: " + filename);

        if(1==1) return;
//        for (int j = 0; j < p.D.data.length; j++)
//        {   // copy resulting values from output neurons to data array for plotting
//            p.D.data[j][1][3] = r.out[j][0];
//            p.D.data[j][1][4] = r.out[j][1];
//        }        
//        BufferedImage img = new Plot(800,800).cross(2).plot(p.D.data, 0, 1, new int[]{0,1,2,3,4});
//        javax.imageio.ImageIO.write(img, "png", new java.io.File(filename+".png"));
        

        String sql = Q.insertInto("run")
                .a("date", new Date())
                .a("runtime", r.totalDur)
                .a("inlr", r.best.IN().length-1, "hidlr", r.best.layers[1].length-1, "outlr", r.best.OUT().length-1)
                .a("teach_mode",r.p.MODE)
                .a("function",ActivationFunction.Types.asString(r.p.NNW_AFUNC))
                .a("dyn_lrate",(r.p.DYNAMIC_LEARNING_RATE?1:0))
                .a("learning_rate",r.p.LEARNING_RATE_COEF,"%.3f")
                .a("sample",r.p.D.data.length, "data_gen", r.p.D.DATA_GEN)
                .a("testset_size",r.p.D.V.set.size(),"vldset_size",r.p.D.T.set.size())
                .a("rnd_search_iters",r.p.RANDOM_SEARCH_ITERS, "rnd_best_iter",r.rndBestis, "rnd_search_time",r.rndSearchDur)
                .a("max_iters",r.p.TEACH_MAX_ITERS, "iters",r.i)
                .a("targ_sd",r.lastTrainres.variance,"%.5f")
                .a("imprv_epochs",r.imprvEpochs,"testset_correct",r.testsetCorrect,"vldset_correct",r.vldsetCorrect)
                .a("testset_percentage",(r.testsetCorrect*100.0/p.D.V.set.size()),"%.2f")
                .a("testset_percentage",(r.vldsetCorrect*100.0/p.D.T.set.size()),"%.2f")
                .a("mlpfile",filename)
                .z();
        System.out.println(sql);
        cli().execute(sql);

    }
    



    private static float[][][] createSample()
    {
        float[][][] data = new float[800][][]
                ;
        for(int i = 0 ; i < data.length; i++)
        {
            float x = (float)Math.random();
            float y = (float)Math.random();
            
            float func = 
                    //(float)(Math.sin(-x * Math.PI*20d)*Math.sin(x * Math.PI*3d))*0.5f;
                    //(float)(Math.sin(-x * Math.PI*2d))*0.5f + 0.5f;
                    //(x-0.5f)*(x-0.5f)*(x-0.5f);
                     //(x-0.5f)*(x-0.5f) + y;
                    //(float)(Math.cos(x*Math.PI*4f)*0.5f) + 0.5f;
                    ((x-0.65f)*(x-0.65f)+(y-0.65f)*(y-0.65f));
                     //x;
            float func2 = ((x-0.35f)*(x-0.35f)+(y-0.65f)*(y-0.65f));
            float func3 =  ((x-0.35f)*(x-0.35f)+(y-0.35f)*(y-0.35f));
            float func4 =  ((x-0.35f)*(x-0.35f)+(y-0.35f)*(y-0.35f));
            data[i] = new float[][]{
                {x,y},
                {func <= 0.17f || func2 <= 0.17f || func3 <= 0.17f || func4 <= 0.17f ? 1f : 0f, 
                 func > 0.17f && func2 > 0.17f && func3 > 0.17f && func4 > 0.17f ? 1f : 0f,
                 func + func2, // for plotting reference
                0f,0f} // for plotting received values
            };
        };
        return data;
    }

    

}
