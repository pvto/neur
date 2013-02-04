
package data;

import java.io.IOException;
import neur.util.dataio.DiskIO;

/**
 *
 * @author Paavo Toivanen
 */
public class Parabola {

    
    public static void main(String[] args) throws IOException {
        
        double[][][] data = new double[100][][];
        double normvar = 4.0;
        for(int i = 0; i < data.length; i++)
        {
            double x = //(double)i / (double)data.length - 0.5;
                    Math.random() - 0.5;
            double y = x*x;
            double point = norm(normvar, (Math.random() - 0.5) * 10);
            if (i % 2 == 0)
                point = -point;
            double[] in = new double[]{x, y, y + point};
            double[] out = new double[]{point >= 0 ? 1.0 : 0.0};
            data[i] = new double[][]{in, out};
        }
        new DiskIO().saveSampleCSV("parabola.data", data, 7);
    }
    
    
    static double norm(double var, double x)
    {
        return (Math.exp(-0.5 * (x / var) * (x / var))) 
                / (Math.sqrt(Math.PI * 2.0) * var)
                ;
    }
}
