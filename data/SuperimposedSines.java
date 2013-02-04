
package data;

import java.io.IOException;
import neur.util.dataio.DiskIO;

/**
 *
 * @author Paavo Toivanen
 */
public class SuperimposedSines {

    
    
    public static void main(String[] args) throws IOException {
        
        double[][][] data = new double[1000][][];
        double normvar = 4.0;
        for(int i = 0; i < data.length; i++)
        {
            double x = //(double)i / (double)data.length - 0.5;
                    Math.random() - 0.5;
            double y = Math.sin(x*27) + Math.sin((x+1)*11);
            double point = norm(normvar, (Math.random() - 0.5) * 40);
            if (i % 2 == 0)
                point = -point;
            double[] in = new double[]{x, y, y + point};
            double[] out = new double[]{point >= 0 ? 1.0 : 0.0};
            data[i] = new double[][]{in, out};
        }
        new DiskIO().saveSampleCSV("sup-2-sines.data", data, 7);
    }
    
    
    static double norm(double var, double x)
    {
        return (Math.exp(-0.5 * (x / var) * (x / var))) 
                / (Math.sqrt(Math.PI * 2.0) * var)
                ;
    }


}
