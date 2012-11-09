
package neur.auto.in;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import static neur.util.Arrf.*;

/** Class to extract neural net supervision data from a raw data collection.
 *
 * @author Paavo Toivanen
 */
public class Sampler {
    
    public int DISCR_DATA_UPPER_THRESHOLD = 10;

    public float[][][] extractSampleFromCSV(List<String[]> raw, int[] in, int[] out)
    {
        float[][][] r = new float[raw.size()][][];
        String[] templ = raw.get(0);
        // for each column, determine if its data is discrete numeric, or numeric, or alphabetic
        boolean[] numeric = new boolean[templ.length];
        boolean[] discrete = new boolean[templ.length];
        boolean[] inContains = new boolean[templ.length];
        boolean[] outContains = new boolean[templ.length];
        for (int i = 0; i < numeric.length; i++)
        {
            numeric[i] = true;
            discrete[i] = true;
            inContains[i] = contains(in, i);
            outContains[i] = contains(out, i);
        }
        for (int k = 0; k < raw.size(); k++)
        {
            String[] cols = raw.get(k);
            for (int j = 0; j < templ.length; j++)
            {
                cols[j] = cols[j]
                        .replace(',', '.');
                if (numeric[j])
                {
                    if (!cols[j].matches("(\\d*\\.\\d+|\\d+(\\.\\d*)?)"))    // not a decimal number
                    {
                        discrete[j] = false;
                        numeric[j] = false;
                    }
                    else if (!cols[j].matches("\\d+") || Long.parseLong(cols[j]) > DISCR_DATA_UPPER_THRESHOLD)
                    {
                        discrete[j] = false;
                    }
                }
                
            }
        }
        // extract each input column according to its datatype into a temporary array
        Object[] tmpArrays = new Object[numeric.length];
        for (int i = 0; i < tmpArrays.length; i++)
        {
            if (discrete[i])    { tmpArrays[i] = ints(col(raw, i)); }
            else if (numeric[i]){ tmpArrays[i] = floats(col(raw, i)); } // 1
            else                { tmpArrays[i] = col(raw, i); }
        }        
        
        // separate each input column into classes, if it is discrete-numeric or discrete-other
        Object[] classes = new Object[numeric.length];
        int totalIns = 0,
                totalOuts = 0;
        for (int i = 0; i < tmpArrays.length; i++)
        {
            int n = 1; 
            if (discrete[i])
            {
                int[][] cc = classes((int[])tmpArrays[i]);
                classes[i] = cc;
                n = cc.length;
            }
            else if (numeric[i]) { } // n = 1
            else
            {
                TreeMap cc = classes((String[])tmpArrays[i]);
                classes[i] = cc;
                n = cc.size();
            }
            if (inContains[i]) totalIns += n;
            if (outContains[i]) totalOuts += n;
        }
        // create n columns of numeric input values from each column according to data contents
        for (int k = 0; k < r.length; k++)
        {
            r[k] = new float[][] { new float[totalIns], new float[totalOuts] };
            int iind = 0,
                    oind = 0;
            for (int i = 0; i < numeric.length; i++)
            {
                if (discrete[i])
                {   // discrete numeric classes, convert each to n numeric input columns of {0,1}
                    int[] arr = (int[])tmpArrays[i];
                    int[][] cc = (int[][])classes[i];
                    for (int h = 0; h < cc.length; h++)
                    {
                        if (inContains[i])
                            r[k][0][iind++] =
                                    (cc[h][0] == arr[k] ? 1f: 0f);
                        if (outContains[i])
                            r[k][1][oind++] =
                                    (cc[h][0] == arr[k] ? 1f: 0f);
                    }
                }
                else if (numeric[i])
                {   // continuous numeric values, retain this column
                    float[] arr = (float[])tmpArrays[i];
                    if (inContains[i])
                        r[k][0][iind++] = arr[k];
                    if (outContains[i])
                        r[k][1][oind++] = arr[k];
                }
                else
                {   // other discrete classes, convert each to n numeric input columns of {0,1}
                    String[] arr = (String[])tmpArrays[i];
                    TreeMap<String,Integer> cc = (TreeMap<String,Integer>)classes[i];
                    Entry<String,Integer> e = cc.firstEntry();
                    for(int h = 0; h < cc.size(); h++)
                    {
                        if (inContains[i])
                            r[k][0][iind++] =
                                    (e.getKey().equals(arr[k]) ? 1f : 0f);
                        if (outContains[i])
                            r[k][1][oind++] =
                                    (e.getKey().equals(arr[k]) ? 1f : 0f);
                        e = cc.higherEntry(e.getKey());
                    }
                }
            }
        }
        return r;
    }

}
