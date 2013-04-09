package neur;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import neur.auto.in.Sampler;
import neur.data.Dataset;
import neur.data.TrainMode;
import neur.learning.LearnParams;
import neur.learning.LearnRecord;
import neur.learning.Teachers;
import neur.learning.clf.Fast1OfNClassifier;
import neur.learning.fit.TrainFit;
import neur.learning.learner.ElasticBackProp;
import neur.struct.ActivationFunction;
import neur.util.Arrf;
import neur.util.Log;
import neur.util.dataio.DiskIO;
import neur.util.visuals.MLPVisualisation;





public class Main {


    
    
    public static void main(String[] args) throws IOException, SQLException
    {
        final int dataset = 104;
        int nInputColumns = 16*17;
        int[] in = Arrf.ints(Arrf.range(0f, nInputColumns, 1f));    // input columns to use from the csv
        int[] out = {nInputColumns};    // supervision columns from csv
        final String sample = "abcd";
        final String datagen = sample;
        
        List<String[]> data_ = new DiskIO()
                .loadCSV("src_copy/data/pattern/abcd_16x17.data", "(,\\s*|,?\\s+)"); 
                //.loadCSV("src_copy/data/MATLAB/"+sample+".data","(,\\s*|,?\\s+)"); 
        float[][][] tdata_ = 
                new Sampler(){{
                    DISCR_DATA_UPPER_THRESHOLD = 5;
                    EVEN_OUT_CL_DISTRIB=0;
                }}.extractSample(data_, in, out);
        final float[][][] tdata =
                tdata_;
                //neur.util.Arrf.normaliseMinmax(tdata_);
                //neur.util.Arrf.standardise(tdata_);
        log.log("loaded dataset %s (in,out):(%d,%d) size %d", sample, tdata[0][0].length, tdata[0][1].length, tdata.length);
        //printStatistics(tdata);
        
        
        LearnParams<MLP,ElasticBackProp> p = new LearnParams()
        {{
                NNW_AFUNC = ActivationFunction.Types.AFUNC_SIGMOID;
                NNW_AFUNC_PARAMS = new float[]{ 3f };
                MODE = TrainMode.SUPERVISED_ONLINE_MODE;
                NNW_DIMS = new int[]{tdata[0][0].length, 20,20, tdata[0][1].length};

                L = new neur.learning.learner.
                        BackPropagation();
                        //ElasticBackProp();
                        //MomentumEBP();
                LEARNING_RATE_COEF = 0.01f;
                DYNAMIC_LEARNING_RATE = true;
                TRG_ERR = 1e-9f;
                TEACH_MAX_ITERS = 8000;
                DIVERGENCE_PRESUMED = 2000;
                STOCHASTIC_SEARCH_ITERS = 0;
                FIT_FUNC = new TrainFit();
                DATASET_SLICING = Dataset.Slicing.RandomDueClassification;
                D = new Dataset()
                {{
                        DATA_GEN = datagen;
                        DATASET = dataset;
                        SAMPLE = sample;
                        data = tdata;
                        initTrain_Test_Sets(data.length * 4 / 10, DATASET_SLICING);
                }};
                
                CF = new Fast1OfNClassifier();
        }};


        p.nnw = new MLP(p.NNW_DIMS, ActivationFunction.Types.create(p.NNW_AFUNC, p.NNW_AFUNC_PARAMS));
        LearnRecord<MLP> r = new LearnRecord<MLP>(p);
        new MLPVisualisation().createFrame(r, 400, 300, 7).run();
        new Teachers().tabooBoxAndIntensification(p, r, log);
        
        // visualisation of classification wrt different param pairs. This creates cols*cols native windows, beware!
//            int nvisu = tdata[0][0].length -1;
//            for(int x = 0; x < nvisu; x++)
//            {
//                int y = 0;
//                for (int j = 0; j < nvisu; j++) {
//                    if (y == x) y++;
//                    String scrPos = String.format("%d,%d %d,%d", nvisu+1+1, nvisu+1, x+2, j+1);
//                    new ClfVisualisation(){{optimise=4.0; WINDOW_DECO=false; SHOW_FPS=false;}}
//                            .setParameter("Y", y)
//                            .setParameter("X", x)
//                            .setParameter("margin", 1)
//                            .createFrame(r, 340, 260, 0.2).setScreenPos(scrPos)
//                            .run()
//                            ;
//                    y++;
//                }
//            }
    }

    private static Log log = Log.create.chained(Log.cout, Log.file.bind("main-java.log"));
    
    



    private static void learnRecordPlusToCsv(
            String filename, LearnParams<MLP, ElasticBackProp> p, LearnRecord<MLP> r) throws IOException
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

    
    private static void printStatistics(float[][][] tdata)
    {
        float[][] cols = Arrf.cols(tdata, 1);
        for (int i = 0; i < cols.length; i++)
            cols[i] = Arrf.mult(cols[i], (float)(i+1));
        float[] distribData = Arrf.add(cols);
        for (int i = 0; i < tdata[0][0].length; i++)
        {
            float[] icol = Arrf.col(tdata, 0, i);
            log.log("col=%d  mean=%f sd=%f  pearson=%s", i, 
                    Arrf.evdist_mean(icol), Arrf.evdist_sd(icol),
                    Arrf.pearsonCorrelation(icol, distribData));
        }

    }

    
}
