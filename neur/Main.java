package neur;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
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
import neur.util.visuals.ClfVisualisation;
import neur.util.visuals.MLPVisualisation;
import static util.store.DbCli.x.*;

/**
 *
 * @author Paavo Toivanen
 */
public class Main {


    
    
    public static void main(String[] args) throws IOException, SQLException
    {
        new File("mlp").mkdir();
        
        int max_runs = 1;
        final int dataset = 104;
//        final String sample = "ecoli";
//        int[] in = {1,2,3,4,5,6,7}, out = new int[]{8};
        final String sample = "sup-2-sines";
        int[] in = {0,2}, out = {3};
        final String datagen = sample;
        
//        DbSamples dbSamples = new DbSamples(getDbClient());
//        final float[][][] tdata = createSample();
//        final float[][][] tdata = dbSamples.loadSample(dataset, sample);
//        final float[][][] tdata = 
//                neur.util.Arrf.normaliseMinmax(
//                
//                new Sampler(){{EVEN_OUT_CL_DISTRIB=0;}}
//                .extractSample(
//                new DiskIO().loadCSV("/media/KINGSTON/nlg/data/MATLAB/"+sample+".data","(,\\s*|,?\\s+)"), 
//                in, out))
//                ;
        final float[][][] tdata = new float[200][][];
        for (int i = 0; i < tdata.length; i++)
        {
            double x = (double)i / (double)tdata.length;
            double y =  Math.sin(x * 11);
            double tan = Math.cos(x * 11);
            double add = 0.4 *  (Math.random()>0.5?1.0:-1.0);
                    //(0.1+Math.random())*0.2 * (Math.random()>0.5?1.0:-1.0);
            tdata[i] = 
                    new float[][] {{(float)x,(float)(y + add),(float)tan},  {add>0?1f:0f,add>0?0f:1f}};
        }
        log.log("loaded dataset %s (in,out):(%d,%d) size %d", sample, tdata[0][0].length, tdata[0][1].length, tdata.length);
        LearnParams<MLP,ElasticBackProp> p = new LearnParams()
        {{
                NNW_AFUNC = ActivationFunction.Types.AFUNC_TANH;
                NNW_AFUNC_PARAMS = new float[]{ 3f };
                MODE = TrainMode.SUPERVISED_ONLINE_MODE;
                NNW_DIMS = new int[]{tdata[0][0].length, 24, tdata[0][1].length};

                L = new neur.learning.learner.
                        //BackPropagation();
                        //ElasticBackProp();
                        MomentumEBP();
                LEARNING_RATE_COEF = 0.1f;
                DYNAMIC_LEARNING_RATE = false;
                TRG_ERR = 1e-9f;
                TEACH_MAX_ITERS = 48000;
                DIVERGENCE_PRESUMED = Math.min(Math.max(400, TEACH_MAX_ITERS / 2), 24000);
                STOCHASTIC_SEARCH_ITERS = 100;

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

        for (int i = 0; i < max_runs; i++)
        {
            p.nnw = new MLP(p.NNW_DIMS, ActivationFunction.Types.create(p.NNW_AFUNC, p.NNW_AFUNC_PARAMS));
            LearnRecord<MLP> r = new LearnRecord<MLP>(p);
            new ClfVisualisation(){{optimise=4.0;}}.createFrame(r, 500, 400, 0.5).setScreenPos("21 21").run();
            new MLPVisualisation().createFrame(r, 700, 600, 15).run();
            runTest(p, r);
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
