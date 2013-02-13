package neur;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import neur.auto.in.Sampler;
import neur.data.Dataset;
import neur.data.TrainMode;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
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
        int max_runs = 1;
        final int dataset = 104;
//        final String sample = "ecoli";
//        int[] in = {1,2,3,4,5,6,7}, out = new int[]{8};
        final String sample = "ecoli";
        int[] in = {1,2,3,4,5,6,7}, out = new int[]{8};
        final String datagen = sample;
        
//        DbSamples dbSamples = new DbSamples(getDbClient());
//        final float[][][] tdata = createSample();
//        final float[][][] tdata = dbSamples.loadSample(dataset, sample);
        final float[][][] tdata = 
                neur.util.Arrf.normaliseMinmax(
                
                new Sampler(){{EVEN_OUT_CL_DISTRIB=1;}}
                .extractSample(
                new DiskIO().loadCSV("/home/paavoto/src/neur2/src_copy/data/MATLAB/"+sample+".data","(,\\s*|,?\\s+)"), 
                in, out))
                ;
                
//        dbSamples.saveSample(dataset, datagen, sample, tdata);
        //dbSamples.deleteSample(dataset, sample);
        log.log("loaded dataset %s (in,out):(%d,%d)", sample, tdata[0][0].length, tdata[0][1].length);
        LearnParams<MLP,ElasticBackProp> p = new LearnParams()
        {{
                NNW_AFUNC = ActivationFunction.Types.AFUNC_TANH;
                NNW_AFUNC_PARAMS = new float[]{ 3f };
                MODE = TrainMode.SUPERVISED_MINIBATCH;
                NNW_DIMS = new int[]{tdata[0][0].length, 12,12, tdata[0][1].length};

                L = new neur.learning.learner.
                        //BackPropagation();
                        ElasticBackProp();
                LEARNING_RATE_COEF = 0.01f;
                DYNAMIC_LEARNING_RATE = true;
                TRG_ERR = 1e-9f;
                TEACH_MAX_ITERS = 6000;
                DIVERGENCE_PRESUMED = Math.min(Math.max(400, TEACH_MAX_ITERS / 2), 1000);
                STOCHASTIC_SEARCH_ITERS = 0;

                DATASET_SLICING = Dataset.Slicing.RandomDueClassification;
                D = new Dataset()
                {{
                        DATA_GEN = datagen;
                        DATASET = dataset;
                        SAMPLE = sample;
                        data = tdata;
                        initTrain_Test_Sets(data.length * 1 / 10, DATASET_SLICING);
                }};
                
                CF = new Fast1OfNClassifier();
        }};        

        for (int i = 0; i < max_runs; )
        {
            p.nnw = new MLP(p.NNW_DIMS, ActivationFunction.Types.create(p.NNW_AFUNC, p.NNW_AFUNC_PARAMS));
            LearnRecord<MLP> r = new LearnRecord<MLP>(p);
//            nnw.layers[0][0].netInput = 1f; // 0f disables bias neuron
//            nnw.layers[1][0].netInput = 1f; // 0f disables bias neuron
            runTest(p, r);
            if (i==0)
                saveResults(sample+".clf", p, r);            
            if (++i % 4 == 0)
            {
                p.NNW_DIMS[1]++;
            }            
        }
        cli().close();
        
    }

    private static Log log = Log.cout;
    
    
    private static void runTest(LearnParams p, LearnRecord<MLP> r) throws IOException, SQLException
    {
        new Teachers().tabooBoxAndIntensification(p, r, log);
        
//        String filename = "mlp" + File.separator 
//                +p.D.DATASET+"-"+p.D.SAMPLE+"-hid"+(r.best.layers[1].length-1)+"-"
//                +ActivationFunction.Types.asString(p.NNW_AFUNC).substring(0,4)
//                +"-ok"+String.format("%.2f", r.bestItem.testsetCorrect*100.0/p.D.TEST.set.size())
//                +"-"+new SimpleDateFormat("yyMMddHHmmss").format(new Date())
//                +".mlp";
//        new DiskIO().save(filename, r.best);
//        log.log("saved: " + filename);

//        for (int j = 0; j < p.D.data.length; j++)
//        {   // copy resulting values from output neurons to data array for plotting
//            p.D.data[j][1][3] = r.out[j][0];
//            p.D.data[j][1][4] = r.out[j][1];
//        }        
//        BufferedImage img = new Plot(800,800).cross(2).plot(p.D.data, 0, 1, new int[]{0,1,2,3,4});
//        javax.imageio.ImageIO.write(img, "png", new java.io.File(filename+".png"));
        

//        String sql = Q.insertInto("run")
//                .a("date", new Date())
//                .a("runtime", r.duration)
//                .a("inlr", r.best.layers[0].length-1, "hidlr", r.best.layers[1].length-1, "outlr", r.best.outv().length-1)
//                .a("teach_mode",r.p.MODE)
//                .a("function",ActivationFunction.Types.asString(r.p.NNW_AFUNC))
//                .a("dyn_lrate",(r.p.DYNAMIC_LEARNING_RATE?1:0))
//                .a("learning_rate",r.p.LEARNING_RATE_COEF,"%.3f")
//                .a("sample",r.p.D.data.length, "data_gen", r.p.D.DATA_GEN)
//                .a("testset_size",r.p.D.TEST.set.size(),"vldset_size",r.p.D.TRAIN.set.size())
//                .a("rnd_search_iters",r.p.STOCHASTIC_SEARCH_ITERS, "rnd_best_iter",0, "rnd_search_time",r.duration)
//                .a("max_iters",r.p.TEACH_MAX_ITERS, "iters",0)
//                .a("targ_sd",r.averageSummedError,"%.5f")
//                .a("imprv_epochs",0,"testset_correct",r.bestItem.testsetCorrect,"vldset_correct",r.bestItem.trainsetCorrect)
//                .a("testset_percentage",(r.bestItem.testsetCorrect*100.0/p.D.TEST.set.size()),"%.2f")
//                .a("testset_percentage",(r.bestItem.trainsetCorrect*100.0/p.D.TRAIN.set.size()),"%.2f")
//                .a("mlpfile",filename)
//                .z();
//        System.out.println(sql);
//        cli().execute(sql);

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

    private static void saveResults(String filename, LearnParams<MLP, ElasticBackProp> p, LearnRecord<MLP> r) throws IOException
    {
        OutputStream out = new FileOutputStream(filename);
        for (int i = 0; i < p.D.data.length; i++)
        {
            float[][] d = p.D.data[i];
            float[] res = r.best.feedf(d[0]);
            float[] interpreted = p.CF.normalisedClassification(d,res);
            out.write(
                    (java.util.Arrays.toString(neur.util.Arrf.concat(d[0],res,interpreted,d[1]))
                    .replaceAll("[\\[\\]]","")+"\r\n").getBytes()
                    );
        }
        out.close();
    }

    
}
