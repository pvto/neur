
package neur.util.dataio;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiskIO {

    public void save(String filename, Serializable mlp) throws IOException
    {
        File f = new File(filename);
        FileOutputStream out = new FileOutputStream(f);
        ObjectOutputStream oo = new ObjectOutputStream(out);
        oo.writeObject(mlp);
        oo.flush();
        oo.close();
    }


    public List<String[]> loadCSV(String filename, String separatorRegexp) throws IOException
    {
        FileInputStream in = null;
        try
        {
            in = new FileInputStream(new File(filename));
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            List<String[]> ret = new ArrayList<String[]>();
            String line;
            while((line = r.readLine()) != null)
            {
                ret.add(line.split(separatorRegexp));
            }
            return ret;
        }
        finally
        {
            if (in != null)
            {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    public void saveSampleCSV(String filename, double[][][] data, int precision) throws IOException
    {
        String fmt = "%."+precision+"f";
        OutputStream out = new FileOutputStream(filename);
        for (int i = 0; i <data.length; i++)
        {
            double[] dd = neur.util.Arrf.concat(data[i][0],data[i][1]);
            int x = 0;
            for(double d : dd)
            {
                String extra = (x++ == dd.length - 1) 
                        ? "\r\n"
                        : ", ";
                out.write(String.format(Locale.ENGLISH, fmt+extra, d).getBytes());
            }
        }
        out.close();
    }
}
