
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
    
    public int DISCR_DATA_UPPER_THRESHOLD = 32;
    public int EVEN_OUT_CL_DISTRIB = 1;
    
    public float[][][] extractSample(List<String[]> raw, int[] in, int[] out)
    {
        float[][][] r = new float[raw.size()][][];
        String[] templ = raw.get(0);
        // for each column, determine if its data is integral, or real, or alphabetic
        boolean[] real = new boolean[templ.length];
        boolean[] integral = new boolean[templ.length];
        boolean[] inContains = new boolean[templ.length];
        boolean[] outContains = new boolean[templ.length];
        for (int i = 0; i < real.length; i++)
        {
            real[i] = true;
            integral[i] = true;
            inContains[i] = contains(in, i);
            outContains[i] = contains(out, i);
        }
        for (int j = 0; j < templ.length; j++)
        {
            for (int k = 0; k < raw.size(); k++)            
            {
                String[] cols = raw.get(k);
                cols[j] = cols[j]
                        .replace(',', '.');
                if (real[j])
                {
                    if (!cols[j].matches("-?(\\d*\\.\\d+|\\d+(\\.\\d*)?)"))    // not a decimal number
                    {
                        integral[j] = false;
                        real[j] = false;
                        break;
                    }
                    else if (!cols[j].matches("-?\\d+(\\.0*)?") 
                            || Long.parseLong(cols[j].replaceFirst("\\..*", "")) > DISCR_DATA_UPPER_THRESHOLD)
                    {
                        integral[j] = false;
                    }
                }
            }
        }
        // extract each input column according to its datatype into a temporary array
        Object[] tmpArrays = new Object[real.length];
        for (int i = 0; i < tmpArrays.length; i++)
        {
            if (integral[i])    { tmpArrays[i] = ints(col(raw, i)); }
            else if (real[i]){ tmpArrays[i] = floats(col(raw, i)); } // 1
            else                { tmpArrays[i] = col(raw, i); }
        }        
        
        // separate each input column into classes if it is discrete-numeric or discrete-other
        Object[] classes = new Object[real.length];
        int totalIns = 0,
                totalOuts = 0;
        for (int i = 0; i < tmpArrays.length; i++)
        {
            int n = 1; 
            if (integral[i])
            {
                int[][] cc = classes((int[])tmpArrays[i]);
                classes[i] = cc;
                n = cc.length;
            }
            else if (real[i]) { } // n = 1
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
            for (int i = 0; i < real.length; i++)
            {
                if (integral[i])
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
                else if (real[i])
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
        // if classifying, make the distribution of output classes even by randomly copying input rows into the sample
        if (EVEN_OUT_CL_DISTRIB > 0 && out.length == 1 && totalOuts > 1)
        {
            int[] volumes = new int[totalOuts];
            for (int k = 0; k < r.length; k++)
                for (int j = 0; j < volumes.length; j++)
                    if (r[k][1][j] != 0f)
                        volumes[j] ++;
            int max = max(volumes);
            int additions = sum(subtract(max, volumes));
            int origlen = r.length;
            int k = origlen;
            r = java.util.Arrays.copyOf(r, r.length + additions);

            while(k < r.length)
            {
                for (int i = 0; i < volumes.length; i++)
                {
                    if (volumes[i] == max) 
                        continue;
                    for(;;)
                    {
                        int itemInd = (int)(Math.random() * origlen);   // TODO: rnd functions should be deterministic - would enable the regeneration of the same sample 
                        if (r[itemInd][1][i] == 0f)
                            continue;
                        r[k++] = r[itemInd];
                        volumes[i]++;
                        break;
                    }
                }
            }
            System.out.println("padded sample from within: " + origlen + " to " + r.length);
        }
        
        
            System.out.println("shuffling");
            r = shuffle(r);
        
        return r;
    }

}
